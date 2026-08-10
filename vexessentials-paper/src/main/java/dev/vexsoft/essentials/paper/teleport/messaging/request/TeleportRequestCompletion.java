package dev.vexsoft.essentials.paper.teleport.messaging.request;

import java.util.UUID;

/** Reports the final execution result to the other request participant. */
public record TeleportRequestCompletion(UUID requestId, boolean successful, String detail) {
}
