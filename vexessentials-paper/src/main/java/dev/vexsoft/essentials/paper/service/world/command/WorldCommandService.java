package dev.vexsoft.essentials.paper.service.world.command;

import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.essentials.api.world.WorldGeneratorType;
import java.util.concurrent.CompletableFuture;

/** Coordinates localized command interaction with the managed world service. */
public interface WorldCommandService extends VexService {

  CompletableFuture<Boolean> create(
      VexCommandSource source,
      String world,
      WorldGeneratorType generator,
      String seed
  );

  CompletableFuture<Boolean> importWorld(VexCommandSource source, String world);

  CompletableFuture<Boolean> load(VexCommandSource source, String world);

  CompletableFuture<Boolean> unload(VexCommandSource source, String world);

  CompletableFuture<Boolean> delete(VexCommandSource source, String world, String confirmation);

  CompletableFuture<Boolean> teleport(VexCommandSource source, String world);

  CompletableFuture<Boolean> setServerSpawn(VexCommandSource source);

  CompletableFuture<Boolean> setSpawn(VexCommandSource source);

  boolean list(VexCommandSource source);

  boolean info(VexCommandSource source, String world);
}
