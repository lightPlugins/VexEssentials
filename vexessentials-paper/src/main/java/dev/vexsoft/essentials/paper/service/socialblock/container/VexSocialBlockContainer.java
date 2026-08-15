package dev.vexsoft.essentials.paper.service.socialblock.container;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.essentials.api.socialblock.SocialBlockContainer;
import dev.vexsoft.essentials.paper.socialblock.data.SocialBlockData;
import dev.vexsoft.essentials.paper.teleport.data.VexEssentialsPlayerData;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Default player-bound facade for persistent block relations. */
public final class VexSocialBlockContainer implements SocialBlockContainer {

  private final VexPlayer player;

  public VexSocialBlockContainer(final VexPlayer player) {
    this.player = Objects.requireNonNull(player, "player");
  }

  @Override
  public boolean hasBlocked(final UUID playerId) {
    UUID checked = Objects.requireNonNull(playerId, "playerId");
    return player.read(
        VexEssentialsPlayerData.SOCIAL_BLOCKS,
        data -> data.hasBlocked(checked)
    );
  }

  @Override
  public Set<UUID> getBlockedPlayers() {
    return player.read(
        VexEssentialsPlayerData.SOCIAL_BLOCKS,
        data -> Set.copyOf(data.getBlockedPlayers())
    );
  }

  @Override
  public boolean block(final UUID playerId) {
    UUID checked = Objects.requireNonNull(playerId, "playerId");
    AtomicBoolean changed = new AtomicBoolean();
    player.update(
        VexEssentialsPlayerData.SOCIAL_BLOCKS,
        (SocialBlockData data) -> changed.set(data.block(checked))
    );
    return changed.get();
  }

  @Override
  public boolean unblock(final UUID playerId) {
    UUID checked = Objects.requireNonNull(playerId, "playerId");
    AtomicBoolean changed = new AtomicBoolean();
    player.update(
        VexEssentialsPlayerData.SOCIAL_BLOCKS,
        (SocialBlockData data) -> changed.set(data.unblock(checked))
    );
    return changed.get();
  }
}
