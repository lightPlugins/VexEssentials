package dev.vexsoft.essentials.paper.socialblock.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.Suggest;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.core.paper.command.suggestion.PlayerNameSuggestionProvider;
import dev.vexsoft.essentials.api.service.socialblock.SocialBlockService;
import dev.vexsoft.essentials.api.socialblock.SocialBlockChangeStatus;
import dev.vexsoft.essentials.paper.service.socialblock.presentation.SocialBlockPresentationService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/** Blocks a previously seen player for all supported VexEssentials features. */
@CommandRoot(name = "block", description = "Blocks a player", playerOnly = true)
@Dependencies({PlayerService.class, SocialBlockService.class, SocialBlockPresentationService.class})
public final class BlockCommand {

  private final PlayerService players;
  private final SocialBlockService blocks;
  private final SocialBlockPresentationService presentation;

  public BlockCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    blocks = checked.require(SocialBlockService.class);
    presentation = checked.require(SocialBlockPresentationService.class);
  }

  @Command(value = "<player>", permission = "vexessentials.command.block")
  public CompletableFuture<Boolean> block(
      final VexCommandSource source,
      @Argument("player") @Suggest(PlayerNameSuggestionProvider.class) final String playerName
  ) {
    VexPlayer player = player(source);
    return blocks.block(player, playerName).thenApply(status -> {
      presentation.sendChange(player, playerName, status);
      return status == SocialBlockChangeStatus.BLOCKED;
    });
  }

  private VexPlayer player(final VexCommandSource source) {
    return players.require(((Player) source.getSender()).getUniqueId());
  }
}
