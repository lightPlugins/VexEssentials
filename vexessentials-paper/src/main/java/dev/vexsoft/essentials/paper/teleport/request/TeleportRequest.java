package dev.vexsoft.essentials.paper.teleport.request;

import dev.vexsoft.essentials.api.teleport.request.TeleportRequestState;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** Runtime-only teleport request with an atomic state transition. */
public final class TeleportRequest {

  private final UUID requestId;
  private final UUID requesterId;
  private final String requesterName;
  private final UUID targetId;
  private final String targetName;
  private final TeleportRequestType type;
  private final Instant createdAt;
  private final Instant expiresAt;
  private final AtomicReference<TeleportRequestState> state;

  /** Creates a validated request. */
  public TeleportRequest(
      final UUID requestId,
      final UUID requesterId,
      final String requesterName,
      final UUID targetId,
      final String targetName,
      final TeleportRequestType type,
      final Instant createdAt,
      final Instant expiresAt,
      final TeleportRequestState initialState
  ) {
    this.requestId = Objects.requireNonNull(requestId, "requestId");
    this.requesterId = Objects.requireNonNull(requesterId, "requesterId");
    this.requesterName = Objects.requireNonNull(requesterName, "requesterName");
    this.targetId = Objects.requireNonNull(targetId, "targetId");
    this.targetName = Objects.requireNonNull(targetName, "targetName");
    this.type = Objects.requireNonNull(type, "type");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    state = new AtomicReference<>(Objects.requireNonNull(initialState, "initialState"));
  }

  public UUID requestId() {
    return requestId;
  }

  public UUID requesterId() {
    return requesterId;
  }

  public String requesterName() {
    return requesterName;
  }

  public UUID targetId() {
    return targetId;
  }

  public String targetName() {
    return targetName;
  }

  public TeleportRequestType type() {
    return type;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  public TeleportRequestState state() {
    return state.get();
  }

  public boolean transition(
      final TeleportRequestState expected,
      final TeleportRequestState replacement
  ) {
    return state.compareAndSet(expected, replacement);
  }

  public boolean expired(final Instant now) {
    return !Objects.requireNonNull(now, "now").isBefore(expiresAt);
  }
}
