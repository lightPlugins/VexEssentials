package dev.vexsoft.essentials.paper.service.world;

import dev.vexsoft.core.api.configuration.ConfigurationSection;
import dev.vexsoft.core.api.configuration.VexConfiguration;
import dev.vexsoft.core.api.service.configuration.ConfigurationService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.core.api.world.WorldKey;
import dev.vexsoft.core.paper.service.network.ServerIdentityService;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.core.paper.service.world.WorldService;
import dev.vexsoft.essentials.api.service.world.ManagedWorldService;
import dev.vexsoft.essentials.api.world.ManagedWorld;
import dev.vexsoft.essentials.api.world.ManagedWorldState;
import dev.vexsoft.essentials.api.world.WorldGeneratorType;
import dev.vexsoft.essentials.api.world.WorldOperationResult;
import dev.vexsoft.essentials.paper.world.generator.VoidChunkGenerator;
import io.papermc.paper.math.Position;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

/** Paper-26.2 lifecycle manager for server-local namespaced dimensions. */
@Dependencies({
    ConfigurationService.class,
    ScheduleService.class,
    WorldService.class,
    ServerIdentityService.class
})
public final class VexManagedWorldService implements ManagedWorldService {

  private static final Set<WorldKey> PROTECTED_WORLDS = Set.of(
      new WorldKey("minecraft", "overworld"),
      new WorldKey("minecraft", "the_nether"),
      new WorldKey("minecraft", "the_end")
  );
  private static final WorldKey OVERWORLD = new WorldKey("minecraft", "overworld");

  private final ConfigurationService configurations;
  private final ScheduleService schedules;
  private final WorldService worlds;
  private final ServerIdentityService serverIdentity;
  private final Path levelDirectory;
  private final Logger logger;
  private final Map<WorldKey, Definition> definitions = new ConcurrentHashMap<>();
  private final Set<WorldKey> operations = ConcurrentHashMap.newKeySet();
  private volatile VexConfiguration configuration;
  private volatile ServerPosition serverSpawn;
  private volatile boolean teleportToServerSpawnOnJoin;
  private volatile boolean unloadOverworldOnStartup;

  public VexManagedWorldService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    configurations = checked.require(ConfigurationService.class);
    schedules = checked.require(ScheduleService.class);
    worlds = checked.require(WorldService.class);
    serverIdentity = checked.require(ServerIdentityService.class);
    levelDirectory = Bukkit.getServer().getLevelDirectory().toAbsolutePath().normalize();
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
    reload();
  }

  @Override
  public void initialize() {
    List<CompletableFuture<WorldOperationResult>> loads = definitions.values().stream()
        .filter(Definition::autoLoad)
        .map(Definition::key)
        .map(this::load)
        .toList();
    applyServerSpawn();
    CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new)).whenComplete((ignored, error) -> {
      applyServerSpawn();
      if (unloadOverworldOnStartup) {
        schedules.runGlobalLater(20, () -> unload(OVERWORLD));
      }
    });
  }

  @Override
  public synchronized boolean reload() {
    try {
      VexConfiguration loaded = configurations.load("worlds.yml");
      Map<WorldKey, Definition> loadedDefinitions = readDefinitions(loaded);
      ServerPosition loadedSpawn = readSpawn(loaded).orElse(null);
      boolean loadedTeleportOnJoin = loaded.getBoolean("teleport-on-join", true);
      boolean loadedUnloadOverworld = loaded.getBoolean("unload-overworld-on-startup", false);
      configuration = loaded;
      definitions.clear();
      definitions.putAll(loadedDefinitions);
      serverSpawn = loadedSpawn;
      teleportToServerSpawnOnJoin = loadedTeleportOnJoin;
      unloadOverworldOnStartup = loadedUnloadOverworld;
      applyServerSpawn();
      return true;
    } catch (RuntimeException exception) {
      logger.log(Level.WARNING, "Unable to load worlds.yml; keeping the previous world state", exception);
      return false;
    }
  }

  @Override
  public Collection<ManagedWorld> getWorlds() {
    return definitions.values().stream()
        .map(this::view)
        .sorted(Comparator.comparing(world -> world.key().asString()))
        .toList();
  }

  @Override
  public Optional<ManagedWorld> find(final WorldKey key) {
    Definition definition = definitions.get(Objects.requireNonNull(key, "key"));
    return definition == null ? Optional.empty() : Optional.of(view(definition));
  }

  @Override
  public CompletableFuture<WorldOperationResult> create(
      final WorldKey key,
      final WorldGeneratorType generator,
      final OptionalLong seed
  ) {
    WorldKey checkedKey = Objects.requireNonNull(key, "key");
    if (PROTECTED_WORLDS.contains(checkedKey)) {
      return completedFailure("protected");
    }
    dimensionPath(checkedKey);
    WorldGeneratorType checkedGenerator = Objects.requireNonNull(generator, "generator");
    if (definitions.containsKey(checkedKey) || worlds.find(checkedKey).isPresent()) {
      return completedFailure("already-exists");
    }
    if (Files.exists(dimensionPath(checkedKey))) {
      return completedFailure("folder-exists");
    }
    Definition definition = new Definition(checkedKey, checkedGenerator, true, seed);
    return operate(checkedKey, () -> createNow(definition, true));
  }

  @Override
  public CompletableFuture<WorldOperationResult> importWorld(final WorldKey key) {
    WorldKey checkedKey = Objects.requireNonNull(key, "key");
    if (PROTECTED_WORLDS.contains(checkedKey)) {
      return completedFailure("protected");
    }
    dimensionPath(checkedKey);
    if (definitions.containsKey(checkedKey)) {
      return completedFailure("already-managed");
    }
    if (!Files.isDirectory(dimensionPath(checkedKey))) {
      return completedFailure("folder-missing");
    }
    Definition definition = new Definition(
        checkedKey,
        WorldGeneratorType.NORMAL,
        false,
        OptionalLong.empty()
    );
    definitions.put(checkedKey, definition);
    save();
    return CompletableFuture.completedFuture(WorldOperationResult.success());
  }

  @Override
  public CompletableFuture<WorldOperationResult> load(final WorldKey key) {
    WorldKey checkedKey = Objects.requireNonNull(key, "key");
    if (checkedKey.equals(OVERWORLD)) {
      if (worlds.find(checkedKey).isPresent()) {
        return completedFailure("already-loaded");
      }
      Definition overworld = new Definition(
          checkedKey,
          WorldGeneratorType.NORMAL,
          false,
          OptionalLong.empty()
      );
      return operate(checkedKey, () -> createNow(overworld, false)).thenApply(result -> {
        if (result.successful()) {
          setUnloadOverworldOnStartup(false);
        }
        return result;
      });
    }
    Definition definition = definitions.get(checkedKey);
    if (definition == null) {
      return completedFailure("not-managed");
    }
    if (worlds.find(checkedKey).isPresent()) {
      return completedFailure("already-loaded");
    }
    if (!Files.isDirectory(dimensionPath(checkedKey))) {
      return completedFailure("folder-missing");
    }
    return operate(checkedKey, () -> createNow(definition, false));
  }

  @Override
  public CompletableFuture<WorldOperationResult> unload(final WorldKey key) {
    WorldKey checkedKey = Objects.requireNonNull(key, "key");
    if (PROTECTED_WORLDS.contains(checkedKey) && !checkedKey.equals(OVERWORLD)) {
      return completedFailure("protected");
    }
    if (!definitions.containsKey(checkedKey) && !checkedKey.equals(OVERWORLD)) {
      return completedFailure("not-managed");
    }
    Optional<World> loadedWorld = worlds.find(checkedKey);
    if (loadedWorld.isEmpty()) {
      return completedFailure("not-loaded");
    }
    if (!loadedWorld.get().getPlayers().isEmpty()) {
      return completedFailure("players-present");
    }
    if (serverSpawn != null && serverSpawn.world().equals(checkedKey)
        || Bukkit.getServer().getRespawnWorld() == loadedWorld.get()) {
      return completedFailure("server-spawn");
    }
    return operate(checkedKey, () -> Bukkit.unloadWorld(loadedWorld.get(), true)
        ? WorldOperationResult.success()
        : WorldOperationResult.failed("unload-rejected")).thenApply(result -> {
          if (result.successful() && checkedKey.equals(OVERWORLD)) {
            setUnloadOverworldOnStartup(true);
          }
          return result;
        });
  }

  @Override
  public CompletableFuture<WorldOperationResult> delete(final WorldKey key) {
    WorldKey checkedKey = Objects.requireNonNull(key, "key");
    if (PROTECTED_WORLDS.contains(checkedKey)) {
      return completedFailure("protected");
    }
    if (!definitions.containsKey(checkedKey)) {
      return completedFailure("not-managed");
    }
    Optional<World> loadedWorld = worlds.find(checkedKey);
    if (loadedWorld.isPresent() && !loadedWorld.get().getPlayers().isEmpty()) {
      return completedFailure("players-present");
    }
    if (serverSpawn != null && serverSpawn.world().equals(checkedKey)
        || loadedWorld.isPresent() && Bukkit.getServer().getRespawnWorld() == loadedWorld.get()) {
      return completedFailure("server-spawn");
    }
    if (!operations.add(checkedKey)) {
      return completedFailure("operation-running");
    }
    CompletableFuture<WorldOperationResult> result = new CompletableFuture<>();
    schedules.runGlobal(() -> {
      try {
        if (loadedWorld.isPresent() && !Bukkit.unloadWorld(loadedWorld.get(), true)) {
          operations.remove(checkedKey);
          result.complete(WorldOperationResult.failed("unload-rejected"));
          return;
        }
        schedules.runAsync(() -> deleteFiles(checkedKey, result));
      } catch (RuntimeException exception) {
        completeDeleteFailure(checkedKey, result, exception);
      }
    });
    return result;
  }

  @Override
  public Optional<ServerPosition> getServerSpawn() {
    return Optional.ofNullable(serverSpawn);
  }

  @Override
  public boolean teleportToServerSpawnOnJoin() {
    return teleportToServerSpawnOnJoin;
  }

  @Override
  public CompletableFuture<WorldOperationResult> setSpawn(final ServerPosition position) {
    ServerPosition checked = Objects.requireNonNull(position, "position");
    if (!checked.server().equals(serverIdentity.getServerId())) {
      return completedFailure("wrong-server");
    }
    Optional<Location> location = worlds.createLocation(checked);
    if (location.isEmpty()) {
      return completedFailure("not-loaded");
    }
    CompletableFuture<WorldOperationResult> result = new CompletableFuture<>();
    schedules.runGlobal(() -> result.complete(location.get().getWorld().setSpawnLocation(location.get())
        ? WorldOperationResult.success()
        : WorldOperationResult.failed("spawn-rejected")));
    return result;
  }

  @Override
  public void setServerSpawn(final ServerPosition position) {
    ServerPosition checked = Objects.requireNonNull(position, "position");
    if (!checked.server().equals(serverIdentity.getServerId())) {
      throw new IllegalArgumentException("The server spawn must belong to this backend server");
    }
    serverSpawn = checked;
    definitions.computeIfPresent(checked.world(), (key, definition) -> new Definition(
        definition.key(),
        definition.generator(),
        true,
        definition.seed()
    ));
    save();
    writeSpawn(checked);
    applyServerSpawn();
  }

  private CompletableFuture<WorldOperationResult> operate(
      final WorldKey key,
      final Operation operation
  ) {
    if (!operations.add(key)) {
      return completedFailure("operation-running");
    }
    CompletableFuture<WorldOperationResult> result = new CompletableFuture<>();
    schedules.runGlobal(() -> {
      try {
        result.complete(operation.execute());
      } catch (RuntimeException exception) {
        logger.log(Level.WARNING, "World operation failed for " + key.asString(), exception);
        result.complete(WorldOperationResult.failed("internal-error"));
      } finally {
        operations.remove(key);
      }
    });
    return result;
  }

  private WorldOperationResult createNow(final Definition definition, final boolean register) {
    WorldCreator creator = WorldCreator.ofKey(toNamespacedKey(definition.key()));
    definition.seed().ifPresent(creator::seed);
    switch (definition.generator()) {
      case NORMAL -> creator.type(WorldType.NORMAL);
      case FLAT -> creator.type(WorldType.FLAT);
      case VOID -> creator
          .generator(new VoidChunkGenerator())
          .generateStructures(false)
          .forcedSpawnPosition(Position.block(0, 64, 0), 0, 0);
    }
    World world = Bukkit.createWorld(creator);
    if (world == null) {
      return WorldOperationResult.failed("creation-rejected");
    }
    if (register) {
      definitions.put(definition.key(), definition);
      save();
    }
    if (definition.generator() == WorldGeneratorType.VOID) {
      Location spawn = new Location(world, 0.5, 64, 0.5);
      world.setSpawnLocation(spawn);
    }
    applyServerSpawn();
    return WorldOperationResult.success();
  }

  private ManagedWorld view(final Definition definition) {
    ManagedWorldState state;
    if (operations.contains(definition.key())) {
      state = worlds.find(definition.key()).isPresent()
          ? ManagedWorldState.UNLOADING
          : ManagedWorldState.LOADING;
    } else {
      state = worlds.find(definition.key()).isPresent()
          ? ManagedWorldState.LOADED
          : ManagedWorldState.UNLOADED;
    }
    return new ManagedWorld(
        definition.key(),
        definition.generator(),
        state,
        definition.autoLoad()
    );
  }

  private Path dimensionPath(final WorldKey key) {
    return WorldDimensionPathResolver.resolve(levelDirectory, key);
  }

  private void deleteFiles(
      final WorldKey key,
      final CompletableFuture<WorldOperationResult> result
  ) {
    try {
      Path dimensions = levelDirectory.resolve("dimensions");
      WorldDirectoryDeletion.delete(dimensions, dimensionPath(key));
      definitions.remove(key);
      save();
      operations.remove(key);
      result.complete(WorldOperationResult.success());
    } catch (IOException | RuntimeException exception) {
      completeDeleteFailure(key, result, exception);
    }
  }

  private void completeDeleteFailure(
      final WorldKey key,
      final CompletableFuture<WorldOperationResult> result,
      final Throwable exception
  ) {
    operations.remove(key);
    logger.log(Level.WARNING, "Unable to delete world " + key.asString(), exception);
    result.complete(WorldOperationResult.failed("delete-failed"));
  }

  private NamespacedKey toNamespacedKey(final WorldKey key) {
    return new NamespacedKey(key.namespace(), key.value());
  }

  private synchronized void save() {
    VexConfiguration current = Objects.requireNonNull(configuration, "configuration");
    List<Map<String, Object>> values = definitions.values().stream()
        .sorted(Comparator.comparing(value -> value.key().asString()))
        .map(this::serialize)
        .toList();
    current.set("worlds", values);
    current.save();
  }

  private synchronized void setUnloadOverworldOnStartup(final boolean enabled) {
    unloadOverworldOnStartup = enabled;
    VexConfiguration current = Objects.requireNonNull(configuration, "configuration");
    current.set("unload-overworld-on-startup", enabled);
    current.save();
  }

  private Map<String, Object> serialize(final Definition definition) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("key", definition.key().asString());
    value.put("generator", definition.generator().name().toLowerCase(Locale.ROOT));
    value.put("auto-load", definition.autoLoad());
    if (definition.seed().isPresent()) {
      value.put("seed", definition.seed().getAsLong());
    }
    return value;
  }

  private Map<WorldKey, Definition> readDefinitions(final ConfigurationSection source) {
    Object raw = source.get("worlds");
    if (!(raw instanceof List<?> list)) {
      return Map.of();
    }
    Map<WorldKey, Definition> loaded = new LinkedHashMap<>();
    for (Object element : list) {
      if (!(element instanceof Map<?, ?> map)) {
        continue;
      }
      WorldKey key = WorldKey.parse(String.valueOf(map.get("key")));
      Object rawGenerator = map.containsKey("generator") ? map.get("generator") : "normal";
      WorldGeneratorType generator = WorldGeneratorType.valueOf(
          String.valueOf(rawGenerator).toUpperCase(Locale.ROOT)
      );
      Object rawAutoLoad = map.containsKey("auto-load") ? map.get("auto-load") : true;
      boolean autoLoad = Boolean.parseBoolean(String.valueOf(rawAutoLoad));
      Object rawSeed = map.get("seed");
      OptionalLong seed = rawSeed instanceof Number number
          ? OptionalLong.of(number.longValue())
          : OptionalLong.empty();
      loaded.put(key, new Definition(key, generator, autoLoad, seed));
    }
    return loaded;
  }

  private Optional<ServerPosition> readSpawn(final ConfigurationSection source) {
    if (!source.contains("server-spawn.world")) {
      return Optional.empty();
    }
    return Optional.of(new ServerPosition(
        serverIdentity.getServerId(),
        WorldKey.parse(source.getString("server-spawn.world")),
        source.getDouble("server-spawn.x", 0.5),
        source.getDouble("server-spawn.y", 64),
        source.getDouble("server-spawn.z", 0.5),
        (float) source.getDouble("server-spawn.yaw", 0),
        (float) source.getDouble("server-spawn.pitch", 0)
    ));
  }

  private void writeSpawn(final ServerPosition spawn) {
    VexConfiguration current = Objects.requireNonNull(configuration, "configuration");
    current.set("server-spawn.world", spawn.world().asString());
    current.set("server-spawn.x", spawn.x());
    current.set("server-spawn.y", spawn.y());
    current.set("server-spawn.z", spawn.z());
    current.set("server-spawn.yaw", decimalRotation(spawn.yaw()));
    current.set("server-spawn.pitch", decimalRotation(spawn.pitch()));
    current.save();
  }

  private double decimalRotation(final float rotation) {
    return Double.parseDouble(Float.toString(rotation));
  }

  private void applyServerSpawn() {
    ServerPosition spawn = serverSpawn;
    if (spawn == null) {
      return;
    }
    worlds.createLocation(spawn).ifPresent(location -> schedules.runGlobal(() -> {
      location.getWorld().setSpawnLocation(location);
      Bukkit.getServer().setRespawnWorld(location.getWorld());
    }));
  }

  private CompletableFuture<WorldOperationResult> completedFailure(final String reason) {
    return CompletableFuture.completedFuture(WorldOperationResult.failed(reason));
  }

  private record Definition(
      WorldKey key,
      WorldGeneratorType generator,
      boolean autoLoad,
      OptionalLong seed
  ) {
  }

  @FunctionalInterface
  private interface Operation {
    WorldOperationResult execute();
  }
}
