package dev.vexsoft.essentials.paper.warp.messaging.handler;

import dev.vexsoft.core.api.messaging.MessageContext;
import dev.vexsoft.core.api.messaging.MessageHandler;
import dev.vexsoft.core.api.messaging.MessageType;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.api.service.warp.WarpService;
import dev.vexsoft.essentials.paper.warp.messaging.WarpMessages;
import dev.vexsoft.essentials.paper.warp.messaging.WarpRegistryChanged;
import java.util.Objects;

/** Refreshes the local immutable warp snapshot after a remote registry mutation. */
@Dependencies(WarpService.class)
public final class WarpRegistryChangedHandler implements MessageHandler<WarpRegistryChanged> {

  private final WarpService warps;

  public WarpRegistryChangedHandler(final VexServiceRegistry services) {
    warps = Objects.requireNonNull(services, "services").require(WarpService.class);
  }

  @Override
  public MessageType<WarpRegistryChanged> getMessageType() {
    return WarpMessages.REGISTRY_CHANGED;
  }

  @Override
  public void handle(final WarpRegistryChanged message, final MessageContext context) {
    warps.refresh();
  }
}
