package dev.vexsoft.essentials.api.teleport.request;

/** Tracks the atomic runtime lifecycle of one teleport request. */
public enum TeleportRequestState {
  PENDING,
  ACCEPTING,
  ACCEPTED,
  DENIED,
  CANCELLED,
  EXPIRED,
  FAILED
}
