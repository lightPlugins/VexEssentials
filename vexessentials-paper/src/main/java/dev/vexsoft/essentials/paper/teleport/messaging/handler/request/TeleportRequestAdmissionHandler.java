package dev.vexsoft.essentials.paper.teleport.messaging.handler.request;

import dev.vexsoft.core.api.messaging.MessageContext;
import dev.vexsoft.core.api.messaging.MessageHandler;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestAdmission;
import java.util.Objects;

/** Delivers target-side admission results to the requester's server. */
@Dependencies(TeleportRequestService.class)
public final class TeleportRequestAdmissionHandler
    implements MessageHandler<TeleportRequestAdmission> {

  private final TeleportRequestService requests;

  public TeleportRequestAdmissionHandler(final VexServiceRegistry services) {
    requests = Objects.requireNonNull(services, "services").require(TeleportRequestService.class);
  }

  @Override
  public MessageType<TeleportRequestAdmission> getMessageType() {
    return TeleportMessages.REQUEST_ADMISSION;
  }

  @Override
  public void handle(final TeleportRequestAdmission admission, final MessageContext context) {
    requests.receive(admission);
  }
}
