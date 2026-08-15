package dev.vexsoft.essentials.api.service.socialblock;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.player.identity.PlayerIdentity;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.essentials.api.socialblock.SocialBlockChangeStatus;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Manages general player block relations shared by VexEssentials features. */
public interface SocialBlockService extends VexService {

  CompletableFuture<SocialBlockChangeStatus> block(VexPlayer owner, String playerName);

  CompletableFuture<SocialBlockChangeStatus> unblock(VexPlayer owner, String playerName);

  CompletableFuture<List<PlayerIdentity>> list(VexPlayer owner);
}
