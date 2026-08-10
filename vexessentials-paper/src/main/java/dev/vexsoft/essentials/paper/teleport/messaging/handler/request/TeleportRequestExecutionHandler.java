package dev.vexsoft.essentials.paper.teleport.messaging.handler.request;

import dev.vexsoft.core.api.messaging.MessageContext;
import dev.vexsoft.core.api.messaging.MessageHandler;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestExecution;
import java.util.Objects;

/** Starts an accepted teleport-here operation on the target player's backend. */
@Dependencies(TeleportRequestService.class)
public final class TeleportRequestExecutionHandler
    implements MessageHandler<TeleportRequestExecution> {

  private final TeleportRequestService requests;

  public TeleportRequestExecutionHandler(final VexServiceRegistry services) {
    requests = Objects.requireNonNull(services, "services").require(TeleportRequestService.class);
  }

  @Override
  public MessageType<TeleportRequestExecution> getMessageType() {
    return TeleportMessages.REQUEST_EXECUTION;
  }

  @Override
  public void handle(final TeleportRequestExecution execution, final MessageContext context) {
    requests.receive(execution);
  }
}
