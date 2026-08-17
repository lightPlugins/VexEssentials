package dev.vexsoft.essentials.paper.service.world.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.core.api.world.WorldKey;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.core.paper.service.messages.SendMessageService;
import dev.vexsoft.core.paper.service.network.ServerIdentityService;
import dev.vexsoft.core.paper.service.world.WorldService;
import dev.vexsoft.essentials.api.service.teleport.EssentialsTeleportService;
import dev.vexsoft.essentials.api.service.world.ManagedWorldService;
import dev.vexsoft.essentials.api.teleport.TeleportOptions;
import dev.vexsoft.essentials.api.world.ManagedWorld;
import dev.vexsoft.essentials.api.world.WorldGeneratorType;
import dev.vexsoft.essentials.api.world.WorldOperationResult;
import dev.vexsoft.essentials.paper.service.teleport.position.TeleportPositionService;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Default localized command facade for managed worlds. */
@Dependencies({
    ManagedWorldService.class,
    SendMessageService.class,
    WorldService.class,
    ServerIdentityService.class,
    EssentialsTeleportService.class,
    PlayerService.class,
    TeleportPositionService.class
})
public final class VexWorldCommandService implements WorldCommandService {

  private static final String DEFAULT_NAMESPACE = "vexessentials";
  private static final Map<String, WorldKey> VANILLA_WORLDS = Map.of(
      "overworld", new WorldKey("minecraft", "overworld"),
      "nether", new WorldKey("minecraft", "the_nether"),
      "end", new WorldKey("minecraft", "the_end")
  );

  private final ManagedWorldService worlds;
  private final SendMessageService messages;
  private final WorldService loadedWorlds;
  private final ServerIdentityService serverIdentity;
  private final EssentialsTeleportService teleports;
  private final PlayerService players;
  private final TeleportPositionService positions;

  public VexWorldCommandService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    worlds = checked.require(ManagedWorldService.class);
    messages = checked.require(SendMessageService.class);
    loadedWorlds = checked.require(WorldService.class);
    serverIdentity = checked.require(ServerIdentityService.class);
    teleports = checked.require(EssentialsTeleportService.class);
    players = checked.require(PlayerService.class);
    positions = checked.require(TeleportPositionService.class);
  }

  @Override
  public CompletableFuture<Boolean> create(
      final VexCommandSource source,
      final String world,
      final WorldGeneratorType generator,
      final String seed
  ) {
    WorldKey key = parse(source, world);
    if (key == null) {
      return CompletableFuture.completedFuture(false);
    }
    OptionalLong parsedSeed;
    try {
      parsedSeed = seed == null || seed.isBlank()
          ? OptionalLong.empty()
          : OptionalLong.of(Long.parseLong(seed));
    } catch (NumberFormatException exception) {
      send(source, "create.invalid-seed", Map.of("seed", seed));
      return CompletableFuture.completedFuture(false);
    }
    return worlds.create(key, generator, parsedSeed).thenCompose(result -> {
      sendResult(source, "create", key, result);
      if (!result.successful() || !(source.getSender() instanceof Player player)) {
        return CompletableFuture.completedFuture(result.successful());
      }
      Location spawn = loadedWorlds.find(key).orElseThrow().getSpawnLocation();
      ServerPosition destination = position(key, spawn);
      return teleports.teleport(player.getUniqueId(), destination, TeleportOptions.defaults())
          .thenApply(outcome -> result.successful());
    });
  }

  @Override
  public CompletableFuture<Boolean> importWorld(
      final VexCommandSource source,
      final String world
  ) {
    return operation(source, "import", world, worlds::importWorld);
  }

  @Override
  public CompletableFuture<Boolean> load(final VexCommandSource source, final String world) {
    return operation(source, "load", world, worlds::load);
  }

  @Override
  public CompletableFuture<Boolean> unload(final VexCommandSource source, final String world) {
    return operation(source, "unload", world, worlds::unload);
  }

  @Override
  public CompletableFuture<Boolean> delete(
      final VexCommandSource source,
      final String world,
      final String confirmation
  ) {
    WorldKey key = parse(source, world);
    if (key == null) {
      return CompletableFuture.completedFuture(false);
    }
    if (!"confirm".equalsIgnoreCase(confirmation)) {
      send(source, "delete.confirm-required", Map.of("world", commandName(key)));
      return CompletableFuture.completedFuture(false);
    }
    return worlds.delete(key).thenApply(result -> {
      sendResult(source, "delete", key, result);
      return result.successful();
    });
  }

  @Override
  public CompletableFuture<Boolean> teleport(
      final VexCommandSource source,
      final String world
  ) {
    WorldKey key = parse(source, world);
    if (key == null) {
      return CompletableFuture.completedFuture(false);
    }
    if (!(source.getSender() instanceof Player player)) {
      return CompletableFuture.completedFuture(false);
    }
    return loadedWorlds.find(key).map(target -> {
      ServerPosition destination = position(key, target.getSpawnLocation());
      return teleports.teleport(player.getUniqueId(), destination, TeleportOptions.defaults())
          .thenApply(outcome -> {
            send(
                source,
                outcome.successful() ? "teleport.success" : "teleport.failed",
                Map.of("world", commandName(key))
            );
            return outcome.successful();
          });
    }).orElseGet(() -> {
      send(source, "teleport.not-loaded", Map.of("world", commandName(key)));
      return CompletableFuture.completedFuture(false);
    });
  }

  @Override
  public CompletableFuture<Boolean> setServerSpawn(final VexCommandSource source) {
    Player player = (Player) source.getSender();
    VexPlayer vexPlayer = players.require(player.getUniqueId());
    return positions.capture(vexPlayer).thenApply(position -> position.map(value -> {
      worlds.setServerSpawn(value);
      send(source, "set-server-spawn.success", Map.of("world", commandName(value.world())));
      return true;
    }).orElseGet(() -> {
      send(source, "set-server-spawn.failed", Map.of());
      return false;
    }));
  }

  @Override
  public CompletableFuture<Boolean> setSpawn(final VexCommandSource source) {
    Player player = (Player) source.getSender();
    VexPlayer vexPlayer = players.require(player.getUniqueId());
    return positions.capture(vexPlayer).thenCompose(position -> position.map(value -> worlds
        .setSpawn(value)
        .thenApply(result -> {
          sendResult(source, "set-spawn", value.world(), result);
          return result.successful();
        })).orElseGet(() -> {
          send(source, "set-spawn.failed", Map.of());
          return CompletableFuture.completedFuture(false);
        }));
  }

  @Override
  public boolean list(final VexCommandSource source) {
    messages.send(source.getSender(), "world.commands.list.header", false, Map.of());
    for (ManagedWorld world : worlds.getWorlds()) {
      messages.send(source.getSender(), "world.commands.list.entry."
          + world.state().name().toLowerCase(Locale.ROOT), false, Map.of(
          "world", commandName(world.key()),
          "state", world.state().name().toLowerCase(Locale.ROOT),
          "generator", world.generator().name().toLowerCase(Locale.ROOT)
      ));
    }
    messages.send(source.getSender(), "world.commands.list.footer", false, Map.of(
        "count", Integer.toString(worlds.getWorlds().size())
    ));
    return true;
  }

  @Override
  public boolean info(final VexCommandSource source, final String world) {
    WorldKey key = parse(source, world);
    if (key == null) {
      return false;
    }
    return worlds.find(key).map(value -> {
      messages.send(source.getSender(), "world.commands.info.message."
          + value.state().name().toLowerCase(Locale.ROOT), false, Map.of(
          "world", commandName(value.key()),
          "state", value.state().name().toLowerCase(Locale.ROOT),
          "generator", value.generator().name().toLowerCase(Locale.ROOT),
          "auto-load", Boolean.toString(value.autoLoad())
      ));
      return true;
    }).orElseGet(() -> {
      send(source, "info.not-managed", Map.of("world", commandName(key)));
      return false;
    });
  }

  private CompletableFuture<Boolean> operation(
      final VexCommandSource source,
      final String action,
      final String world,
      final Function<WorldKey, CompletableFuture<WorldOperationResult>> operation
  ) {
    WorldKey key = parse(source, world);
    if (key == null) {
      return CompletableFuture.completedFuture(false);
    }
    return operation.apply(key).thenApply(result -> {
      sendResult(source, action, key, result);
      return result.successful();
    });
  }

  private WorldKey parse(final VexCommandSource source, final String value) {
    try {
      if (value.contains(":")) {
        throw new IllegalArgumentException("World commands do not accept namespaces");
      }
      WorldKey vanilla = VANILLA_WORLDS.get(value.toLowerCase(Locale.ROOT));
      if (vanilla != null) {
        return vanilla;
      }
      return new WorldKey(DEFAULT_NAMESPACE, value);
    } catch (IllegalArgumentException exception) {
      send(source, "error.invalid-world", Map.of("world", value));
      return null;
    }
  }

  private ServerPosition position(final WorldKey key, final Location location) {
    return new ServerPosition(
        serverIdentity.getServerId(),
        key,
        location.x(),
        location.y(),
        location.z(),
        location.getYaw(),
        location.getPitch()
    );
  }

  private void sendResult(
      final VexCommandSource source,
      final String action,
      final WorldKey key,
      final WorldOperationResult result
  ) {
    send(
        source,
        result.successful() ? action + ".success" : "error." + result.reason(),
        Map.of("world", commandName(key))
    );
  }

  private void send(
      final VexCommandSource source,
      final String key,
      final Map<String, String> replacements
  ) {
    messages.send(source.getSender(), "world.commands." + key, true, replacements);
  }

  private String commandName(final WorldKey key) {
    if (key.equals(VANILLA_WORLDS.get("overworld"))) {
      return "overworld";
    }
    if (key.equals(VANILLA_WORLDS.get("nether"))) {
      return "nether";
    }
    if (key.equals(VANILLA_WORLDS.get("end"))) {
      return "end";
    }
    return key.value();
  }
}
