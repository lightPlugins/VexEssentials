package dev.vexsoft.essentials.paper.warp.messaging;

import java.util.Objects;
import java.util.UUID;

/** Signals that another backend should refresh its cached warp registry. */
public record WarpRegistryChanged(UUID changeId) {

  /** Creates a validated change notification. */
  public WarpRegistryChanged {
    changeId = Objects.requireNonNull(changeId, "changeId");
  }
}
