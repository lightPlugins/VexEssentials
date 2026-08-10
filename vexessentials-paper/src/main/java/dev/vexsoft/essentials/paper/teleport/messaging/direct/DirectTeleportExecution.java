package dev.vexsoft.essentials.paper.teleport.messaging.direct;

import dev.vexsoft.core.api.world.ServerPosition;
import java.util.UUID;

/** Requests a direct administrative teleport on the backend hosting the moving player. */
public record DirectTeleportExecution(
    UUID operationId,
    UUID actorId,
    UUID playerId,
    String playerName,
    String destinationName,
    ServerPosition destination
) {
}
