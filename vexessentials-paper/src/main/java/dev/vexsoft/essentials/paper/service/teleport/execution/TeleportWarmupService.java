package dev.vexsoft.essentials.paper.service.teleport.execution;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.essentials.paper.teleport.execution.WarmupCancelReason;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Coordinates cancellable player teleport warmups. */
public interface TeleportWarmupService extends VexService {

  CompletableFuture<Boolean> begin(VexPlayer player);

  void cancel(UUID playerId, WarmupCancelReason reason);

  boolean hasWarmup(UUID playerId);
}
