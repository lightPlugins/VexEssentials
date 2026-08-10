package dev.vexsoft.essentials.paper.teleport.messaging.handler.position;

import dev.vexsoft.core.api.messaging.MessageContext;
import dev.vexsoft.core.api.messaging.MessageHandler;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.service.teleport.position.NetworkPositionService;
import dev.vexsoft.essentials.paper.teleport.messaging.position.PlayerPositionResponse;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import java.util.Objects;

/** Completes a pending position lookup on its requesting backend. */
@Dependencies(NetworkPositionService.class)
public final class PlayerPositionResponseHandler implements MessageHandler<PlayerPositionResponse> {

  private final NetworkPositionService positions;

  /** Creates the response handler. */
  public PlayerPositionResponseHandler(final VexServiceRegistry services) {
    positions = Objects.requireNonNull(services, "services").require(NetworkPositionService.class);
  }

  @Override
  public MessageType<PlayerPositionResponse> getMessageType() {
    return TeleportMessages.POSITION_RESPONSE;
  }

  @Override
  public void handle(final PlayerPositionResponse response, final MessageContext context) {
    positions.accept(response);
  }
}
