package dev.vexsoft.essentials.api.teleport.request;

/** Safe cross-server reasons for refusing a teleport request offer. */
public enum TeleportRequestRejectionReason {
  NONE,
  BLOCKED,
  REQUESTS_DISABLED,
  DUPLICATE,
  TARGET_UNAVAILABLE
}
