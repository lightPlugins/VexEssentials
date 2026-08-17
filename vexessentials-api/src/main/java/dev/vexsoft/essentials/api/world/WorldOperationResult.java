package dev.vexsoft.essentials.api.world;

import java.util.Objects;

/** Stable result returned by a managed world lifecycle operation. */
public record WorldOperationResult(boolean successful, String reason) {

  public WorldOperationResult {
    reason = Objects.requireNonNullElse(reason, "");
  }

  public static WorldOperationResult success() {
    return new WorldOperationResult(true, "");
  }

  public static WorldOperationResult failed(final String reason) {
    return new WorldOperationResult(false, reason);
  }
}
