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
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestType;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/** Requests that another player teleport to the sender. */
@CommandRoot(name = "tpahere", description = "Requests a player to teleport to you", playerOnly = true)
@Dependencies({PlayerService.class, TeleportRequestService.class})
public final class TeleportAskHereCommand {

  private final PlayerService players;
  private final TeleportRequestService requests;

  public TeleportAskHereCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    requests = checked.require(TeleportRequestService.class);
  }

  @Command(value = "<player>", permission = "vexessentials.command.tpahere")
  public CompletableFuture<Boolean> request(
      final VexCommandSource source,
      @Argument("player") @Suggest(PlayerNameSuggestionProvider.class) final String targetName
  ) {
    return requests.send(player(source), targetName, TeleportRequestType.TARGET_HERE);
  }

  private VexPlayer player(final VexCommandSource source) {
    return players.require(((Player) source.getSender()).getUniqueId());
  }
}
