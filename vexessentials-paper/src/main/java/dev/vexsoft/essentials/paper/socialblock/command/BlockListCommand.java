package dev.vexsoft.essentials.paper.socialblock.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.api.service.socialblock.SocialBlockService;
import dev.vexsoft.essentials.paper.service.socialblock.presentation.SocialBlockPresentationService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/** Lists the executing player's general VexEssentials blocks. */
@CommandRoot(name = "blocklist", description = "Lists blocked players", playerOnly = true)
@Dependencies({PlayerService.class, SocialBlockService.class, SocialBlockPresentationService.class})
public final class BlockListCommand {

  private final PlayerService players;
  private final SocialBlockService blocks;
  private final SocialBlockPresentationService presentation;

  public BlockListCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    blocks = checked.require(SocialBlockService.class);
    presentation = checked.require(SocialBlockPresentationService.class);
  }

  @Command(value = "", permission = "vexessentials.command.blocklist")
  public CompletableFuture<Integer> list(final VexCommandSource source) {
    VexPlayer player = players.require(((Player) source.getSender()).getUniqueId());
    return blocks.list(player).thenApply(blocked -> {
      presentation.sendList(player, blocked);
      return blocked.size();
    });
  }
}
