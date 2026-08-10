package dev.vexsoft.essentials.paper.teleport.messaging;

import dev.vexsoft.core.api.messaging.MessageKey;
import dev.vexsoft.core.api.messaging.MessageType;
import lombok.experimental.UtilityClass;
import dev.vexsoft.essentials.paper.teleport.messaging.direct.DirectTeleportCompletion;
import dev.vexsoft.essentials.paper.teleport.messaging.direct.DirectTeleportExecution;
import dev.vexsoft.essentials.paper.teleport.messaging.position.PlayerPositionRequest;
import dev.vexsoft.essentials.paper.teleport.messaging.position.PlayerPositionResponse;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestCompletion;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestDecision;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestExecution;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestOffer;

/** Stable typed message identifiers used by the VexEssentials teleport feature. */
@UtilityClass
public class TeleportMessages {

  public static final MessageType<PlayerPositionRequest> POSITION_REQUEST = MessageType.json(
      MessageKey.of("vexessentials", "teleport.position-request"),
      PlayerPositionRequest.class
  );
  public static final MessageType<PlayerPositionResponse> POSITION_RESPONSE = MessageType.json(
      MessageKey.of("vexessentials", "teleport.position-response"),
      PlayerPositionResponse.class
  );
  public static final MessageType<TeleportRequestOffer> REQUEST_OFFER = MessageType.json(
      MessageKey.of("vexessentials", "teleport.request-offer"),
      TeleportRequestOffer.class
  );
  public static final MessageType<TeleportRequestDecision> REQUEST_DECISION = MessageType.json(
      MessageKey.of("vexessentials", "teleport.request-decision"),
      TeleportRequestDecision.class
  );
  public static final MessageType<TeleportRequestExecution> REQUEST_EXECUTION = MessageType.json(
      MessageKey.of("vexessentials", "teleport.request-execution"),
      TeleportRequestExecution.class
  );
  public static final MessageType<TeleportRequestCompletion> REQUEST_COMPLETION = MessageType.json(
      MessageKey.of("vexessentials", "teleport.request-completion"),
      TeleportRequestCompletion.class
  );
  public static final MessageType<DirectTeleportExecution> DIRECT_EXECUTION = MessageType.json(
      MessageKey.of("vexessentials", "teleport.direct-execution"),
      DirectTeleportExecution.class
  );
  public static final MessageType<DirectTeleportCompletion> DIRECT_COMPLETION = MessageType.json(
      MessageKey.of("vexessentials", "teleport.direct-completion"),
      DirectTeleportCompletion.class
  );
}
