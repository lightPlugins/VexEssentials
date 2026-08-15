package dev.vexsoft.essentials.api.teleport.request;

/** Tracks the atomic runtime lifecycle of one teleport request. */
public enum TeleportRequestState {
  AWAITING_ADMISSION,
  PENDING,
  ACCEPTING,
  EXECUTING,
  ACCEPTED,
  DENIED,
  CANCELLED,
  EXPIRED,
  FAILED
}
