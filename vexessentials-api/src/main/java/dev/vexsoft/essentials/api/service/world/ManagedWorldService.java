package dev.vexsoft.essentials.api.service.world;

import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.core.api.world.WorldKey;
import dev.vexsoft.essentials.api.world.ManagedWorld;
import dev.vexsoft.essentials.api.world.WorldGeneratorType;
import dev.vexsoft.essentials.api.world.WorldOperationResult;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/** Creates and controls server-local dimensions identified by persistent world keys. */
public interface ManagedWorldService extends VexService {

  void initialize();

  boolean reload();

  Collection<ManagedWorld> getWorlds();

  Optional<ManagedWorld> find(WorldKey key);

  CompletableFuture<WorldOperationResult> create(
      WorldKey key,
      WorldGeneratorType generator,
      OptionalLong seed
  );

  CompletableFuture<WorldOperationResult> importWorld(WorldKey key);

  CompletableFuture<WorldOperationResult> load(WorldKey key);

  CompletableFuture<WorldOperationResult> unload(WorldKey key);

  CompletableFuture<WorldOperationResult> delete(WorldKey key);

  Optional<ServerPosition> getServerSpawn();

  /** Returns whether players are moved to the configured server spawn when joining. */
  boolean teleportToServerSpawnOnJoin();

  CompletableFuture<WorldOperationResult> setSpawn(ServerPosition position);

  void setServerSpawn(ServerPosition position);
}
