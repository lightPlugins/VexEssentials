package dev.vexsoft.essentials.paper.teleport.messaging.handler.request;

import dev.vexsoft.core.api.messaging.MessageContext;
import dev.vexsoft.core.api.messaging.MessageHandler;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestOffer;
import java.util.Objects;

/** Delivers a received request offer to the local request coordinator. */
@Dependencies(TeleportRequestService.class)
public final class TeleportRequestOfferHandler implements MessageHandler<TeleportRequestOffer> {

  private final TeleportRequestService requests;

  public TeleportRequestOfferHandler(final VexServiceRegistry services) {
    requests = Objects.requireNonNull(services, "services").require(TeleportRequestService.class);
  }

  @Override
  public MessageType<TeleportRequestOffer> getMessageType() {
    return TeleportMessages.REQUEST_OFFER;
  }

  @Override
  public void handle(final TeleportRequestOffer offer, final MessageContext context) {
    requests.receive(offer);
  }
}
