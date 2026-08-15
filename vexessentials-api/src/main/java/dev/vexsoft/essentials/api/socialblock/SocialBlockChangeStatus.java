package dev.vexsoft.essentials.api.socialblock;

/** Describes the result of changing one player block relation. */
public enum SocialBlockChangeStatus {
  BLOCKED,
  UNBLOCKED,
  ALREADY_BLOCKED,
  NOT_BLOCKED,
  SELF,
  PLAYER_NOT_FOUND,
  FAILED
}
