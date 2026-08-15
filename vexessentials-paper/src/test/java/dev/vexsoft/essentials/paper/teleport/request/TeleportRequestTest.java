package dev.vexsoft.essentials.paper.teleport.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vexsoft.essentials.api.teleport.request.TeleportRequestState;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestType;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifies admission remains an atomic part of the request lifecycle. */
final class TeleportRequestTest {

  @Test
  void admissionCanOnlyCompleteOnce() {
    Instant now = Instant.now();
    TeleportRequest request = new TeleportRequest(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Requester",
        UUID.randomUUID(),
        "Target",
        TeleportRequestType.TO_TARGET,
        now,
        now.plus(Duration.ofMinutes(1)),
        TeleportRequestState.AWAITING_ADMISSION
    );

    assertTrue(request.transition(
        TeleportRequestState.AWAITING_ADMISSION,
        TeleportRequestState.PENDING
    ));
    assertFalse(request.transition(
        TeleportRequestState.AWAITING_ADMISSION,
        TeleportRequestState.FAILED
    ));
  }
}
