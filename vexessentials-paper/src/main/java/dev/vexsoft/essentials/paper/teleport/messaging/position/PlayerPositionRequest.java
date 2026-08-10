package dev.vexsoft.essentials.paper.teleport.messaging.position;

import java.util.UUID;

/** Requests the current position of one online player from their backend server. */
public record PlayerPositionRequest(UUID requestId, UUID playerId, String replyServer) {
}
