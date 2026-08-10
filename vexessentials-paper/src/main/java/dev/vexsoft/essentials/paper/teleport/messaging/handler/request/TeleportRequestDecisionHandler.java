package dev.vexsoft.essentials.paper.teleport.messaging.handler.request;

import dev.vexsoft.core.api.messaging.MessageContext;
import dev.vexsoft.core.api.messaging.MessageHandler;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestDecision;
import java.util.Objects;

/** Delivers a remote request decision to the local request coordinator. */
@Dependencies(TeleportRequestService.class)
public final class TeleportRequestDecisionHandler
    implements MessageHandler<TeleportRequestDecision> {

  private final TeleportRequestService requests;

  public TeleportRequestDecisionHandler(final VexServiceRegistry services) {
    requests = Objects.requireNonNull(services, "services").require(TeleportRequestService.class);
  }

  @Override
  public MessageType<TeleportRequestDecision> getMessageType() {
    return TeleportMessages.REQUEST_DECISION;
  }

  @Override
  public void handle(final TeleportRequestDecision decision, final MessageContext context) {
    requests.receive(decision);
  }
}
