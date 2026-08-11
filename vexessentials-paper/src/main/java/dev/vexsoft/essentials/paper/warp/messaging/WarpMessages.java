package dev.vexsoft.essentials.paper.warp.messaging;

import dev.vexsoft.core.api.messaging.MessageKey;
import dev.vexsoft.core.api.messaging.MessageType;
import lombok.experimental.UtilityClass;

/** Stable network message identifiers used by the warp feature. */
@UtilityClass
public class WarpMessages {

  public static final MessageType<WarpRegistryChanged> REGISTRY_CHANGED = MessageType.json(
      MessageKey.of("vexessentials", "warp.registry-changed"),
      WarpRegistryChanged.class
  );
}
