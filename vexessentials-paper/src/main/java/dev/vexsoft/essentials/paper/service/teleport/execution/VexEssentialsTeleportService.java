package dev.vexsoft.essentials.paper.service.teleport.execution;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.core.api.teleport.TeleportResult;
import dev.vexsoft.core.paper.service.teleport.PlayerTeleportService;
import dev.vexsoft.essentials.api.service.teleport.EssentialsTeleportService;
import dev.vexsoft.essentials.api.teleport.TeleportOptions;
import dev.vexsoft.essentials.api.teleport.TeleportOutcome;
import dev.vexsoft.essentials.api.teleport.TeleportStatus;
import dev.vexsoft.essentials.api.teleport.container.TeleportContainer;
import dev.vexsoft.essentials.paper.service.teleport.position.NetworkPositionService;
import dev.vexsoft.essentials.paper.service.teleport.position.TeleportPositionService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Default controlled teleport engine built on VexCore's async transfer implementation. */
@Dependencies({
    PlayerService.class,
    PlayerTeleportService.class,
    TeleportPositionService.class,
    NetworkPositionService.class,
    TeleportWarmupService.class
})
public final class VexEssentialsTeleportService implements EssentialsTeleportService {

  private final PlayerService players;
  private final PlayerTeleportService teleports;
  private final TeleportPositionService localPositions;
  private final NetworkPositionService networkPositions;
  private final TeleportWarmupService warmups;
  private final Logger logger;

  /** Creates the teleport engine. */
  public VexEssentialsTeleportService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    teleports = checked.require(PlayerTeleportService.class);
    localPositions = checked.require(TeleportPositionService.class);
    networkPositions = checked.require(NetworkPositionService.class);
    warmups = checked.require(TeleportWarmupService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public CompletableFuture<TeleportOutcome> teleport(
      final UUID playerId,
      final ServerPosition destination,
      final TeleportOptions options
  ) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(destination, "destination");
    TeleportOptions checkedOptions = Objects.requireNonNull(options, "options");
    Optional<VexPlayer> player = players.find(playerId);
    if (player.isEmpty()) {
      return CompletableFuture.completedFuture(new TeleportOutcome(
          TeleportStatus.PLAYER_OFFLINE,
          "The player is not loaded on this server"
      ));
    }

    CompletableFuture<Boolean> ready = checkedOptions.applyWarmup()
        ? warmups.begin(player.get())
        : CompletableFuture.completedFuture(true);
    return ready.thenCompose(allowed -> allowed
        ? execute(player.get(), destination, checkedOptions)
        : CompletableFuture.completedFuture(new TeleportOutcome(
            TeleportStatus.FAILED,
            "The teleport warmup was cancelled"
        )));
  }

  private CompletableFuture<TeleportOutcome> execute(
      final VexPlayer player,
      final ServerPosition destination,
      final TeleportOptions options
  ) {
    CompletableFuture<Optional<ServerPosition>> origin = options.rememberOrigin()
        ? localPositions.capture(player)
        : CompletableFuture.completedFuture(Optional.empty());
    return origin.thenCompose(position -> {
      if (options.rememberOrigin()) {
        position.ifPresent(value -> player.getContainer(TeleportContainer.class)
            .setBackPosition(value));
      }
      return teleports.teleport(player, destination);
    }).handle((result, throwable) -> {
      if (throwable != null) {
        logger.log(
            Level.WARNING,
            "A teleport for player '" + player.getUniqueId()
                + "' could not be completed. The player was "
                + "kept at their current position.",
            throwable
        );
        return new TeleportOutcome(TeleportStatus.FAILED, throwable.getMessage());
      }
      return translate(result);
    });
  }

  @Override
  public CompletableFuture<TeleportOutcome> teleportToPlayer(
      final UUID playerId,
      final UUID targetId,
      final TeleportOptions options
  ) {
    Objects.requireNonNull(targetId, "targetId");
    return networkPositions.resolve(targetId).thenCompose(position -> position
        .map(value -> teleport(playerId, value, options))
        .orElseGet(() -> CompletableFuture.completedFuture(new TeleportOutcome(
            TeleportStatus.TARGET_OFFLINE,
            "The target player's position is not available"
        ))));
  }

  private TeleportOutcome translate(final TeleportResult result) {
    return switch (result.status()) {
      case SUCCESS -> TeleportOutcome.success();
      case PLAYER_OFFLINE -> new TeleportOutcome(TeleportStatus.PLAYER_OFFLINE, result.message());
      case WORLD_NOT_LOADED -> new TeleportOutcome(
          TeleportStatus.POSITION_UNAVAILABLE,
          result.message()
      );
      case SERVER_UNAVAILABLE, TRANSFER_REJECTED, TIMED_OUT -> new TeleportOutcome(
          TeleportStatus.TRANSFER_UNAVAILABLE,
          result.message()
      );
      case TELEPORT_REJECTED, FAILED -> new TeleportOutcome(
          TeleportStatus.FAILED,
          result.message()
      );
    };
  }
}
