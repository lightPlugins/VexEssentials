package dev.vexsoft.essentials.paper.service.socialblock;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.player.identity.PlayerIdentity;
import dev.vexsoft.core.api.service.player.PlayerIdentityService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.api.service.socialblock.SocialBlockService;
import dev.vexsoft.essentials.api.socialblock.SocialBlockChangeStatus;
import dev.vexsoft.essentials.api.socialblock.SocialBlockContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Default identity-aware implementation of the general block service. */
@Dependencies(PlayerIdentityService.class)
public final class VexSocialBlockService implements SocialBlockService {

  private final PlayerIdentityService identities;

  public VexSocialBlockService(final VexServiceRegistry services) {
    identities = Objects.requireNonNull(services, "services")
        .require(PlayerIdentityService.class);
  }

  @Override
  public CompletableFuture<SocialBlockChangeStatus> block(
      final VexPlayer owner,
      final String playerName
  ) {
    return change(owner, playerName, true);
  }

  @Override
  public CompletableFuture<SocialBlockChangeStatus> unblock(
      final VexPlayer owner,
      final String playerName
  ) {
    return change(owner, playerName, false);
  }

  @Override
  public CompletableFuture<List<PlayerIdentity>> list(final VexPlayer owner) {
    Objects.requireNonNull(owner, "owner");
    List<UUID> blocked = List.copyOf(
        owner.getContainer(SocialBlockContainer.class).getBlockedPlayers()
    );
    CompletableFuture<List<PlayerIdentity>> result =
        CompletableFuture.completedFuture(new ArrayList<>());
    for (UUID playerId : blocked) {
      result = result.thenCombine(
          identities.find(playerId),
          (resolved, identity) -> {
            identity.ifPresent(resolved::add);
            return resolved;
          }
      );
    }
    return result.thenApply(List::copyOf);
  }

  private CompletableFuture<SocialBlockChangeStatus> change(
      final VexPlayer owner,
      final String playerName,
      final boolean block
  ) {
    Objects.requireNonNull(owner, "owner");
    String checkedName = Objects.requireNonNull(playerName, "playerName").trim();
    return identities.find(checkedName).thenApply(identity -> {
      if (identity.isEmpty()) {
        return SocialBlockChangeStatus.PLAYER_NOT_FOUND;
      }
      PlayerIdentity target = identity.get();
      if (target.uniqueId().equals(owner.getUniqueId())) {
        return SocialBlockChangeStatus.SELF;
      }
      SocialBlockContainer blocks = owner.getContainer(SocialBlockContainer.class);
      if (block) {
        return blocks.block(target.uniqueId())
            ? SocialBlockChangeStatus.BLOCKED
            : SocialBlockChangeStatus.ALREADY_BLOCKED;
      }
      return blocks.unblock(target.uniqueId())
          ? SocialBlockChangeStatus.UNBLOCKED
          : SocialBlockChangeStatus.NOT_BLOCKED;
    }).exceptionally(ignored -> SocialBlockChangeStatus.FAILED);
  }
}
