package dev.vexsoft.essentials.paper.teleport.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.OptionalArgument;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestType;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/** Player-facing teleport request commands. */
@CommandRoot(
    name = "tpa",
    description = "Requests a teleport from another player",
    aliases = {"teleportask"},
    playerOnly = true
)
@Dependencies({PlayerService.class, TeleportRequestService.class})
public final class TeleportRequestCommand {

  private final PlayerService players;
  private final TeleportRequestService requests;

  /** Creates the request command. */
  public TeleportRequestCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    requests = checked.require(TeleportRequestService.class);
  }

  /** Requests permission to teleport to another player. */
  @Command(value = "<player>", permission = "vexessentials.command.tpa")
  public CompletableFuture<Boolean> request(
      final VexCommandSource source,
      @Argument("player") final String targetName
  ) {
    return requests.send(player(source), targetName, TeleportRequestType.TO_TARGET);
  }

  /** Requests that another player teleport to the sender. */
  @Command(value = "here <player>", permission = "vexessentials.command.tpa.here")
  public CompletableFuture<Boolean> requestHere(
      final VexCommandSource source,
      @Argument("player") final String targetName
  ) {
    return requests.send(player(source), targetName, TeleportRequestType.TARGET_HERE);
  }

  /** Opens the confirmation dialog for a pending request. */
  @Command(value = "accept [request]", permission = "vexessentials.command.tpa.accept")
  public CompletableFuture<Boolean> accept(
      final VexCommandSource source,
      @OptionalArgument("request") final String selector
  ) {
    return requests.review(player(source), selector);
  }

  /** Denies a pending request without opening the dialog. */
  @Command(value = "deny [request]", permission = "vexessentials.command.tpa.deny")
  public int deny(
      final VexCommandSource source,
      @OptionalArgument("request") final String selector
  ) {
    return requests.deny(player(source), selector) ? 1 : 0;
  }

  /** Cancels the sender's latest pending outgoing request. */
  @Command(value = "cancel", permission = "vexessentials.command.tpa.cancel")
  public int cancel(final VexCommandSource source) {
    return requests.cancel(player(source)) ? 1 : 0;
  }

  private VexPlayer player(final VexCommandSource source) {
    Player player = (Player) source.getSender();
    return players.require(player.getUniqueId());
  }
}
