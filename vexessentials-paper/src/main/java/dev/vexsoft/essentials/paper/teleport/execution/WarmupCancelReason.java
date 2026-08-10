package dev.vexsoft.essentials.paper.teleport.execution;

/** Identifies the player action that interrupted a pending teleport. */
public enum WarmupCancelReason {
  MOVED,
  DAMAGED,
  LEFT,
  REPLACED
}
