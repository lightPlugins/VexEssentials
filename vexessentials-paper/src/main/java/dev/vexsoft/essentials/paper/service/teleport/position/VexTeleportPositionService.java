package dev.vexsoft.essentials.paper.service.teleport.position;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.core.paper.service.network.ServerIdentityService;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.core.paper.service.world.WorldService;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Folia-safe position capture backed by VexCore world and server identities. */
@Dependencies({ServerIdentityService.class, WorldService.class, ScheduleService.class})
public final class VexTeleportPositionService implements TeleportPositionService {

  private final ServerIdentityService serverIdentity;
  private final WorldService worlds;
  private final ScheduleService scheduler;
  private final Logger logger;

  /** Creates the position service. */
  public VexTeleportPositionService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    serverIdentity = checked.require(ServerIdentityService.class);
    worlds = checked.require(WorldService.class);
    scheduler = checked.require(ScheduleService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public CompletableFuture<Optional<ServerPosition>> capture(final VexPlayer vexPlayer) {
    Objects.requireNonNull(vexPlayer, "player");
    CompletableFuture<Optional<ServerPosition>> result = new CompletableFuture<>();
    Optional<Player> platformPlayer = vexPlayer.findPlatformPlayer(Player.class);
    if (platformPlayer.isEmpty()) {
      result.complete(Optional.empty());
      return result;
    }
    Player player = platformPlayer.get();
    try {
      scheduler.runFor(
          player,
          () -> {
            try {
              Location location = player.getLocation();
              result.complete(Optional.of(new ServerPosition(
                  serverIdentity.getServerId(),
                  worlds.getKey(location.getWorld()),
                  location.getX(),
                  location.getY(),
                  location.getZ(),
                  location.getYaw(),
                  location.getPitch()
              )));
            } catch (RuntimeException exception) {
              reportFailure(player, exception);
              result.complete(Optional.empty());
            }
          },
          () -> result.complete(Optional.empty())
      );
    } catch (RuntimeException exception) {
      reportFailure(player, exception);
      result.complete(Optional.empty());
    }
    return result;
  }

  private void reportFailure(final Player player, final RuntimeException exception) {
    logger.warning(
        "The current position of player '" + player.getName() + "' could not be captured, so "
            + "the teleport was stopped safely. Reason: " + exception.getMessage()
    );
  }
}
