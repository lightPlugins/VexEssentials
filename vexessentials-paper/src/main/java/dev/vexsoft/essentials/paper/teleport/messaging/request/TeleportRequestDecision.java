package dev.vexsoft.essentials.paper.teleport.messaging.request;

import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestState;
import java.util.UUID;

/** Returns an accepted, denied, or cancelled request state to the other backend. */
public record TeleportRequestDecision(
    UUID requestId,
    TeleportRequestState state,
    ServerPosition targetPosition
) {
}
