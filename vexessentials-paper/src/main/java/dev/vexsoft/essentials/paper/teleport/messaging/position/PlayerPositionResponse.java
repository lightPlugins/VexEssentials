package dev.vexsoft.essentials.paper.teleport.messaging.position;

import dev.vexsoft.core.api.world.ServerPosition;
import java.util.UUID;

/** Returns an optional current player position to the requesting backend. */
public record PlayerPositionResponse(
    UUID requestId,
    UUID playerId,
    ServerPosition position
) {
}
