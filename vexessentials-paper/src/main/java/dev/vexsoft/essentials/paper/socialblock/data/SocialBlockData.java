package dev.vexsoft.essentials.paper.socialblock.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** JSON-persisted player block relations. */
public final class SocialBlockData {

  private Set<UUID> blockedPlayers = new HashSet<>();

  public Set<UUID> getBlockedPlayers() {
    return Set.copyOf(blockedPlayers);
  }

  public void setBlockedPlayers(final Set<UUID> blockedPlayers) {
    this.blockedPlayers = blockedPlayers == null
        ? new HashSet<>()
        : new HashSet<>(blockedPlayers);
  }

  public boolean hasBlocked(final UUID playerId) {
    return blockedPlayers.contains(playerId);
  }

  public boolean block(final UUID playerId) {
    return blockedPlayers.add(playerId);
  }

  public boolean unblock(final UUID playerId) {
    return blockedPlayers.remove(playerId);
  }
}
