package dev.vexsoft.essentials.api.teleport.request;

import java.util.Objects;
import java.util.UUID;

/** Confirms or rejects an offered request after target-side privacy checks. */
public record TeleportRequestAdmission(
    UUID requestId,
    boolean accepted,
    TeleportRequestRejectionReason reason
) {

  public TeleportRequestAdmission {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(reason, "reason");
    if (accepted != (reason == TeleportRequestRejectionReason.NONE)) {
      throw new IllegalArgumentException("accepted admissions must use the NONE reason");
    }
  }
}
