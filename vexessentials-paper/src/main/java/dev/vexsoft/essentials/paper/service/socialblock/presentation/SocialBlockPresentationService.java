package dev.vexsoft.essentials.paper.service.socialblock.presentation;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.player.identity.PlayerIdentity;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.essentials.api.socialblock.SocialBlockChangeStatus;
import java.util.List;

/** Presents localized block-command outcomes. */
public interface SocialBlockPresentationService extends VexService {

  void sendChange(VexPlayer player, String targetName, SocialBlockChangeStatus status);

  void sendList(VexPlayer player, List<PlayerIdentity> blockedPlayers);
}
