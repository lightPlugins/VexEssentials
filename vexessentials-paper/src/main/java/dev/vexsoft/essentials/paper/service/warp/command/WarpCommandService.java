package dev.vexsoft.essentials.paper.service.warp.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.VexService;
import java.util.concurrent.CompletableFuture;

/** Coordinates player-facing warp command workflows outside command binding classes. */
public interface WarpCommandService extends VexService {

  CompletableFuture<Boolean> teleport(VexPlayer player, String warpId);

  CompletableFuture<Boolean> teleportOther(
      VexPlayer actor,
      String playerName,
      String warpId
  );

  CompletableFuture<Boolean> list(VexPlayer player);

  CompletableFuture<Boolean> create(VexPlayer player, String warpId, String displayName);

  CompletableFuture<Boolean> update(VexPlayer player, String warpId);

  CompletableFuture<Boolean> delete(VexPlayer player, String warpId);
}
