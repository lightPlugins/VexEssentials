package dev.vexsoft.essentials.paper.teleport.messaging.handler.request;

import dev.vexsoft.core.api.messaging.MessageContext;
import dev.vexsoft.core.api.messaging.MessageHandler;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestCompletion;
import java.util.Objects;

/** Completes the mirrored request state after a remote teleport finishes. */
@Dependencies(TeleportRequestService.class)
public final class TeleportRequestCompletionHandler
    implements MessageHandler<TeleportRequestCompletion> {

  private final TeleportRequestService requests;

  public TeleportRequestCompletionHandler(final VexServiceRegistry services) {
    requests = Objects.requireNonNull(services, "services").require(TeleportRequestService.class);
  }

  @Override
  public MessageType<TeleportRequestCompletion> getMessageType() {
    return TeleportMessages.REQUEST_COMPLETION;
  }

  @Override
  public void handle(final TeleportRequestCompletion completion, final MessageContext context) {
    requests.receive(completion);
  }
}
