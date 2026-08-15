package dev.vexsoft.essentials.paper.teleport.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.OptionalArgument;
import dev.vexsoft.core.paper.command.Suggest;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import dev.vexsoft.essentials.paper.teleport.command.suggestion.PendingTeleportRequestSuggestionProvider;
import java.util.Objects;
import org.bukkit.entity.Player;

/** Denies a selected incoming teleport request. */
@CommandRoot(name = "tpdeny", description = "Denies a teleport request", playerOnly = true)
@Dependencies({PlayerService.class, TeleportRequestService.class})
public final class TeleportDenyCommand {

  private final PlayerService players;
  private final TeleportRequestService requests;

  public TeleportDenyCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    requests = checked.require(TeleportRequestService.class);
  }

  @Command(value = "[request]", permission = "vexessentials.command.tpdeny")
  public int deny(
      final VexCommandSource source,
      @OptionalArgument("request")
      @Suggest(PendingTeleportRequestSuggestionProvider.class) final String selector
  ) {
    return requests.deny(player(source), selector) ? 1 : 0;
  }

  private VexPlayer player(final VexCommandSource source) {
    return players.require(((Player) source.getSender()).getUniqueId());
  }
}
