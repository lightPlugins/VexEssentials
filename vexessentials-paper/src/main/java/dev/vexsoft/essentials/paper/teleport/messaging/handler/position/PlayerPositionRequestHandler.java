package dev.vexsoft.essentials.paper.teleport.messaging.handler.position;

import dev.vexsoft.core.api.messaging.MessageContext;
import dev.vexsoft.core.api.messaging.MessageHandler;
import dev.vexsoft.core.api.messaging.MessageTarget;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.service.messaging.MessagingService;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.essentials.paper.service.teleport.position.TeleportPositionService;
import dev.vexsoft.essentials.paper.teleport.messaging.position.PlayerPositionRequest;
import dev.vexsoft.essentials.paper.teleport.messaging.position.PlayerPositionResponse;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import java.util.Objects;
import java.util.logging.Logger;

/** Answers network position queries on the backend currently hosting the player. */
@Dependencies({PlayerService.class, TeleportPositionService.class, MessagingService.class})
public final class PlayerPositionRequestHandler implements MessageHandler<PlayerPositionRequest> {

  private final PlayerService players;
  private final TeleportPositionService positions;
  private final MessagingService messages;
  private final Logger logger;

  /** Creates the request handler. */
  public PlayerPositionRequestHandler(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    positions = checked.require(TeleportPositionService.class);
    messages = checked.require(MessagingService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public MessageType<PlayerPositionRequest> getMessageType() {
    return TeleportMessages.POSITION_REQUEST;
  }

  @Override
  public void handle(final PlayerPositionRequest request, final MessageContext context) {
    players.find(request.playerId()).ifPresentOrElse(
        player -> positions.capture(player).thenAccept(position -> respond(request,
            position.orElse(null))),
        () -> respond(request, null)
    );
  }

  private void respond(
      final PlayerPositionRequest request,
      final ServerPosition position
  ) {
    try {
      messages.send(
          MessageTarget.server(request.replyServer()),
          TeleportMessages.POSITION_RESPONSE,
          new PlayerPositionResponse(request.requestId(), request.playerId(), position)
      );
    } catch (RuntimeException exception) {
      logger.warning(
          "A cross-server player position response could not be sent. The requesting server "
              + "will safely stop the teleport after its timeout. Reason: "
              + exception.getMessage()
      );
    }
  }
}
