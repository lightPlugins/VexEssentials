package dev.vexsoft.essentials.paper.teleport.messaging.handler.direct;

import dev.vexsoft.core.api.messaging.MessageContext;
import dev.vexsoft.core.api.messaging.MessageHandler;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.service.teleport.execution.DirectTeleportService;
import dev.vexsoft.essentials.paper.teleport.messaging.direct.DirectTeleportCompletion;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import java.util.Objects;

/** Shows a completed remote direct teleport to its command actor. */
@Dependencies(DirectTeleportService.class)
public final class DirectTeleportCompletionHandler
    implements MessageHandler<DirectTeleportCompletion> {

  private final DirectTeleportService teleports;

  public DirectTeleportCompletionHandler(final VexServiceRegistry services) {
    teleports = Objects.requireNonNull(services, "services").require(DirectTeleportService.class);
  }

  @Override
  public MessageType<DirectTeleportCompletion> getMessageType() {
    return TeleportMessages.DIRECT_COMPLETION;
  }

  @Override
  public void handle(final DirectTeleportCompletion completion, final MessageContext context) {
    teleports.receive(completion);
  }
}
