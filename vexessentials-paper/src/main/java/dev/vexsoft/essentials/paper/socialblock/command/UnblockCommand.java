package dev.vexsoft.essentials.paper.socialblock.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.api.service.socialblock.SocialBlockService;
import dev.vexsoft.essentials.api.socialblock.SocialBlockChangeStatus;
import dev.vexsoft.essentials.paper.service.socialblock.presentation.SocialBlockPresentationService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/** Removes a general VexEssentials player block. */
@CommandRoot(name = "unblock", description = "Unblocks a player", playerOnly = true)
@Dependencies({PlayerService.class, SocialBlockService.class, SocialBlockPresentationService.class})
public final class UnblockCommand {

  private final PlayerService players;
  private final SocialBlockService blocks;
  private final SocialBlockPresentationService presentation;

  public UnblockCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    blocks = checked.require(SocialBlockService.class);
    presentation = checked.require(SocialBlockPresentationService.class);
  }

  @Command(value = "<player>", permission = "vexessentials.command.unblock")
  public CompletableFuture<Boolean> unblock(
      final VexCommandSource source,
      @Argument("player") final String playerName
  ) {
    VexPlayer player = players.require(((Player) source.getSender()).getUniqueId());
    return blocks.unblock(player, playerName).thenApply(status -> {
      presentation.sendChange(player, playerName, status);
      return status == SocialBlockChangeStatus.UNBLOCKED;
    });
  }
}
