package dev.vexsoft.essentials.paper.teleport.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.OptionalArgument;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import java.util.Objects;
import org.bukkit.entity.Player;

/** Enables, disables, or toggles incoming teleport requests. */
@CommandRoot(name = "tptoggle", description = "Toggles teleport requests", playerOnly = true)
@Dependencies({PlayerService.class, TeleportRequestService.class})
public final class TeleportToggleCommand {

  private final PlayerService players;
  private final TeleportRequestService requests;

  public TeleportToggleCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    requests = checked.require(TeleportRequestService.class);
  }

  @Command(value = "[enabled]", permission = "vexessentials.command.tptoggle")
  public int toggle(
      final VexCommandSource source,
      @OptionalArgument("enabled") final Boolean enabled
  ) {
    VexPlayer player = players.require(((Player) source.getSender()).getUniqueId());
    requests.toggle(player, enabled);
    return 1;
  }
}
