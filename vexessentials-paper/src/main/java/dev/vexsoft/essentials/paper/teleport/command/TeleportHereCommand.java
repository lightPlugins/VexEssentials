package dev.vexsoft.essentials.paper.teleport.command;

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
import dev.vexsoft.essentials.paper.service.teleport.execution.DirectTeleportService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/** Teleports another online player to the executing player. */
@CommandRoot(
    name = "tphere",
    description = "Teleports an online player to you",
    playerOnly = true
)
@Dependencies({PlayerService.class, DirectTeleportService.class})
public final class TeleportHereCommand {

  private final PlayerService players;
  private final DirectTeleportService teleports;

  /** Creates the teleport-here command. */
  public TeleportHereCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    teleports = checked.require(DirectTeleportService.class);
  }

  /** Teleports the selected player from any backend server to the sender. */
  @Command(value = "<player>", permission = "vexessentials.command.teleport.here")
  public CompletableFuture<Boolean> teleportHere(
      final VexCommandSource source,
      @Argument("player") @Suggest(PlayerNameSuggestionProvider.class) final String playerName
  ) {
    Player player = (Player) source.getSender();
    VexPlayer actor = players.require(player.getUniqueId());
    return teleports.teleportHere(actor, playerName);
  }
}
