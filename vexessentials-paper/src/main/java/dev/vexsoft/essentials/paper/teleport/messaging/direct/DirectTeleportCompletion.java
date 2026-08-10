package dev.vexsoft.essentials.paper.teleport.messaging.direct;

import java.util.UUID;

/** Reports a remote direct teleport result to the command actor. */
public record DirectTeleportCompletion(
    UUID operationId,
    UUID actorId,
    String playerName,
    String destinationName,
    boolean successful
) {
}
