package dev.vexsoft.essentials.api.teleport;

/** Controls the optional behavior applied to one Essentials teleport. */
public record TeleportOptions(boolean rememberOrigin, boolean applyWarmup) {

  /** Creates the default options used by player-facing teleports. */
  public static TeleportOptions defaults() {
    return new TeleportOptions(true, true);
  }

  /** Creates options for privileged teleports that must execute immediately. */
  public static TeleportOptions immediate() {
    return new TeleportOptions(true, false);
  }
}
