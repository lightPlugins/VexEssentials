package dev.vexsoft.essentials.paper.teleport.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.api.service.teleport.EssentialsTeleportService;
import dev.vexsoft.essentials.api.teleport.TeleportOptions;
import dev.vexsoft.essentials.api.teleport.container.TeleportContainer;
import dev.vexsoft.essentials.paper.service.teleport.presentation.TeleportPresentationService;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/** Returns a player to the last position remembered by the teleport engine. */
@CommandRoot(
    name = "back",
    description = "Returns you to your previous position",
    playerOnly = true
)
@Dependencies({
    PlayerService.class,
    EssentialsTeleportService.class,
    TeleportPresentationService.class
})
public final class BackCommand {

  private final PlayerService players;
  private final EssentialsTeleportService teleports;
  private final TeleportPresentationService presentation;

  /** Creates the back command. */
  public BackCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    teleports = checked.require(EssentialsTeleportService.class);
    presentation = checked.require(TeleportPresentationService.class);
  }

  /** Executes the back teleport asynchronously. */
  @Command(value = "back", permission = "vexessentials.command.back")
  public CompletableFuture<Boolean> back(final VexCommandSource source) {
    Player platformPlayer = (Player) source.getSender();
    VexPlayer player = players.require(platformPlayer.getUniqueId());
    return player.getContainer(TeleportContainer.class).getBackPosition()
        .map(position -> teleports.teleport(
            player.getUniqueId(),
            position,
            TeleportOptions.defaults()
        ).thenApply(outcome -> {
          presentation.send(
              player,
              outcome.successful() ? "teleport.back.success" : "teleport.error.unavailable",
              Map.of(),
              outcome.successful() ? "teleport-success" : "teleport-failed"
          );
          return outcome.successful();
        }))
        .orElseGet(() -> {
          presentation.send(player, "teleport.back.empty", Map.of(), "teleport-failed");
          return CompletableFuture.completedFuture(false);
        });
  }
}
