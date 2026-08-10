package dev.vexsoft.essentials.paper.service.teleport.position;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.core.api.world.ServerPosition;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Captures exact player positions on the entity-owning scheduler. */
public interface TeleportPositionService extends VexService {

  CompletableFuture<Optional<ServerPosition>> capture(VexPlayer player);
}
