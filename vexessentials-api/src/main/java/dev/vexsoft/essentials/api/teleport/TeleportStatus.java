package dev.vexsoft.essentials.api.teleport;

/** Describes the controlled result of an Essentials teleport operation. */
public enum TeleportStatus {
  SUCCESS,
  PLAYER_OFFLINE,
  TARGET_OFFLINE,
  POSITION_UNAVAILABLE,
  TRANSFER_UNAVAILABLE,
  FAILED
}
