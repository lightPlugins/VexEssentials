package dev.vexsoft.essentials.api.socialblock;

import dev.vexsoft.core.api.player.PlayerContainer;
import java.util.Set;
import java.util.UUID;

/** Exposes the persistent player blocks owned by one loaded Vex player. */
public interface SocialBlockContainer extends PlayerContainer {

  /** Returns whether this player has blocked the supplied player. */
  boolean hasBlocked(UUID playerId);

  /** Returns an immutable snapshot of all blocked player identities. */
  Set<UUID> getBlockedPlayers();

  /** Blocks a player and returns whether the relation was newly added. */
  boolean block(UUID playerId);

  /** Unblocks a player and returns whether the relation existed. */
  boolean unblock(UUID playerId);
}
