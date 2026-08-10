package dev.vexsoft.essentials.paper.teleport.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerIdentityService;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.api.service.teleport.EssentialsTeleportService;
import dev.vexsoft.essentials.api.teleport.TeleportOptions;
import dev.vexsoft.essentials.paper.service.teleport.presentation.TeleportPresentationService;
import dev.vexsoft.essentials.paper.service.teleport.execution.DirectTeleportService;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/** Direct player teleport command for permitted players. */
@CommandRoot(
    name = "tp",
    description = "Teleports you directly to an online player",
    aliases = {"teleport"},
    playerOnly = true
)
@Dependencies({
    PlayerService.class,
    PlayerIdentityService.class,
    EssentialsTeleportService.class,
    DirectTeleportService.class,
    TeleportPresentationService.class
})
public final class TeleportCommand {

  private final PlayerService players;
  private final PlayerIdentityService identities;
  private final EssentialsTeleportService teleports;
  private final TeleportPresentationService presentation;
  private final DirectTeleportService directTeleports;

  /** Creates the direct teleport command. */
  public TeleportCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    identities = checked.require(PlayerIdentityService.class);
    teleports = checked.require(EssentialsTeleportService.class);
    presentation = checked.require(TeleportPresentationService.class);
    directTeleports = checked.require(DirectTeleportService.class);
  }

  /** Teleports the sender to another online player, including across backend servers. */
  @Command(value = "<player>", permission = "vexessentials.command.teleport")
  public CompletableFuture<Boolean> teleport(
      final VexCommandSource source,
      @Argument("player") final String targetName
  ) {
    Player platformPlayer = (Player) source.getSender();
    VexPlayer player = players.require(platformPlayer.getUniqueId());
    return identities.find(targetName).thenCompose(identity -> identity
        .map(target -> teleports.teleportToPlayer(
            player.getUniqueId(),
            target.uniqueId(),
            TeleportOptions.immediate()
        ).thenApply(outcome -> {
          presentation.send(
              player,
              outcome.successful() ? "teleport.direct.success" : "teleport.player-offline",
              Map.of("player", target.name()),
              outcome.successful() ? "teleport-success" : "teleport-failed"
          );
          return outcome.successful();
        }))
        .orElseGet(() -> {
          presentation.send(
              player,
              "teleport.player-not-found",
              Map.of("player", targetName),
              "teleport-failed"
          );
          return CompletableFuture.completedFuture(false);
        }));
  }

  /** Teleports one online player to another, even when either player is on another backend. */
  @Command(value = "<player> <target>", permission = "vexessentials.command.teleport.others")
  public CompletableFuture<Boolean> teleportOther(
      final VexCommandSource source,
      @Argument("player") final String playerName,
      @Argument("target") final String targetName
  ) {
    Player platformPlayer = (Player) source.getSender();
    VexPlayer actor = players.require(platformPlayer.getUniqueId());
    return directTeleports.teleport(actor, playerName, targetName);
  }
}
