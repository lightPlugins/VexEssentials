package dev.vexsoft.essentials.paper.teleport.messaging.request;

import dev.vexsoft.core.api.world.ServerPosition;
import java.util.UUID;

/** Instructs the request target backend to execute an accepted teleport-here request. */
public record TeleportRequestExecution(UUID requestId, ServerPosition destination) {
}
