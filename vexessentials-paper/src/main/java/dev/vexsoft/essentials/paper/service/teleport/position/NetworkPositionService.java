package dev.vexsoft.essentials.paper.service.teleport.position;

import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.essentials.paper.teleport.messaging.position.PlayerPositionResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Resolves exact online-player positions across the complete proxy network. */
public interface NetworkPositionService extends VexService {

  CompletableFuture<Optional<ServerPosition>> resolve(UUID playerId);

  void accept(PlayerPositionResponse response);
}
