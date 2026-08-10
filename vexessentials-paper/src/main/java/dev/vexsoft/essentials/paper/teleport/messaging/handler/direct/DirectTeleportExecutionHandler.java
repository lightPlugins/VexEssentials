package dev.vexsoft.essentials.paper.teleport.messaging.handler.direct;

import dev.vexsoft.core.api.messaging.MessageContext;
import dev.vexsoft.core.api.messaging.MessageHandler;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.service.teleport.execution.DirectTeleportService;
import dev.vexsoft.essentials.paper.teleport.messaging.direct.DirectTeleportExecution;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import java.util.Objects;

/** Executes a privileged teleport delivered to a remote player backend. */
@Dependencies(DirectTeleportService.class)
public final class DirectTeleportExecutionHandler
    implements MessageHandler<DirectTeleportExecution> {

  private final DirectTeleportService teleports;

  public DirectTeleportExecutionHandler(final VexServiceRegistry services) {
    teleports = Objects.requireNonNull(services, "services").require(DirectTeleportService.class);
  }

  @Override
  public MessageType<DirectTeleportExecution> getMessageType() {
    return TeleportMessages.DIRECT_EXECUTION;
  }

  @Override
  public void handle(final DirectTeleportExecution execution, final MessageContext context) {
    teleports.receive(execution);
  }
}
