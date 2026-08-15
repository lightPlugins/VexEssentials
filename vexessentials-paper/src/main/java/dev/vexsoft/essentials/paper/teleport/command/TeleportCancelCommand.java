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
import dev.vexsoft.essentials.paper.teleport.command.suggestion.OutgoingTeleportRequestSuggestionProvider;
import java.util.Objects;
import org.bukkit.entity.Player;

/** Cancels a selected outgoing teleport request. */
@CommandRoot(name = "tpcancel", description = "Cancels a teleport request", playerOnly = true)
@Dependencies({PlayerService.class, TeleportRequestService.class})
public final class TeleportCancelCommand {

  private final PlayerService players;
  private final TeleportRequestService requests;

  public TeleportCancelCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    requests = checked.require(TeleportRequestService.class);
  }

  @Command(value = "[request]", permission = "vexessentials.command.tpcancel")
  public int cancel(
      final VexCommandSource source,
      @OptionalArgument("request")
      @Suggest(OutgoingTeleportRequestSuggestionProvider.class) final String selector
  ) {
    VexPlayer player = players.require(((Player) source.getSender()).getUniqueId());
    return requests.cancel(player, selector) ? 1 : 0;
  }
}
