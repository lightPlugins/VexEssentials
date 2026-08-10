package dev.vexsoft.essentials.api.teleport;

import java.util.Objects;

/** Contains a stable status and optional diagnostic detail for one teleport. */
public record TeleportOutcome(TeleportStatus status, String detail) {

  /** Creates a validated outcome. */
  public TeleportOutcome {
    status = Objects.requireNonNull(status, "status");
    detail = Objects.requireNonNullElse(detail, "");
  }

  /** Returns whether the teleport completed successfully. */
  public boolean successful() {
    return status == TeleportStatus.SUCCESS;
  }

  /** Creates a successful outcome. */
  public static TeleportOutcome success() {
    return new TeleportOutcome(TeleportStatus.SUCCESS, "");
  }
}
