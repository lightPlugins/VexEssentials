package dev.vexsoft.essentials.paper.teleport.messaging.request;

import dev.vexsoft.essentials.api.teleport.request.TeleportRequestType;
import java.util.UUID;

/** Delivers a new teleport request to its target player's backend. */
public record TeleportRequestOffer(
    UUID requestId,
    UUID requesterId,
    String requesterName,
    UUID targetId,
    String targetName,
    TeleportRequestType type,
    long createdAt,
    long expiresAt
) {
}
