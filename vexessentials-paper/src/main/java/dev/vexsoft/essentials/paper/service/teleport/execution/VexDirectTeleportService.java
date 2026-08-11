package dev.vexsoft.essentials.paper.service.teleport.execution;

import dev.vexsoft.core.api.messaging.DeliveryResult;
import dev.vexsoft.core.api.messaging.MessageTarget;
import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.player.identity.PlayerIdentity;
import dev.vexsoft.core.api.service.messaging.MessagingService;
import dev.vexsoft.core.api.service.network.PlayerDirectoryService;
import dev.vexsoft.core.api.service.player.PlayerIdentityService;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.essentials.api.service.teleport.EssentialsTeleportService;
import dev.vexsoft.essentials.api.teleport.TeleportOptions;
import dev.vexsoft.essentials.paper.service.teleport.position.NetworkPositionService;
import dev.vexsoft.essentials.paper.service.teleport.position.TeleportPositionService;
import dev.vexsoft.essentials.paper.service.teleport.presentation.TeleportPresentationService;
import dev.vexsoft.essentials.paper.teleport.messaging.direct.DirectTeleportCompletion;
import dev.vexsoft.essentials.paper.teleport.messaging.direct.DirectTeleportExecution;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Default cross-server implementation for privileged direct teleports. */
@Dependencies({
    PlayerService.class,
    PlayerIdentityService.class,
    NetworkPositionService.class,
    TeleportPositionService.class,
    EssentialsTeleportService.class,
    TeleportPresentationService.class,
    MessagingService.class,
    PlayerDirectoryService.class
})
public final class VexDirectTeleportService implements DirectTeleportService {

  private final PlayerService players;
  private final PlayerIdentityService identities;
  private final NetworkPositionService networkPositions;
  private final TeleportPositionService localPositions;
  private final EssentialsTeleportService teleports;
  private final TeleportPresentationService presentation;
  private final MessagingService messages;
  private final PlayerDirectoryService directory;
  private final Logger logger;

  /** Creates the direct teleport coordinator. */
  public VexDirectTeleportService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    identities = checked.require(PlayerIdentityService.class);
    networkPositions = checked.require(NetworkPositionService.class);
    localPositions = checked.require(TeleportPositionService.class);
    teleports = checked.require(EssentialsTeleportService.class);
    presentation = checked.require(TeleportPresentationService.class);
    messages = checked.require(MessagingService.class);
    directory = checked.require(PlayerDirectoryService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public CompletableFuture<Boolean> teleport(
      final VexPlayer actor,
      final String movingPlayerName,
      final String targetName
  ) {
    return identities.find(movingPlayerName).thenCombine(
        identities.find(targetName),
        (moving, target) -> new ResolvedPlayers(moving, target)
    ).thenCompose(resolved -> {
      if (resolved.moving().isEmpty()) {
        notFound(actor, movingPlayerName);
        return CompletableFuture.completedFuture(false);
      }
      if (resolved.target().isEmpty()) {
        notFound(actor, targetName);
        return CompletableFuture.completedFuture(false);
      }
      PlayerIdentity moving = resolved.moving().get();
      PlayerIdentity target = resolved.target().get();
      return networkPositions.resolve(target.uniqueId()).thenCompose(destination -> destination
          .map(position -> executeOrDeliver(actor, moving, target.name(), position))
          .orElseGet(() -> {
            offline(actor, target.name());
            return CompletableFuture.completedFuture(false);
          }));
    }).exceptionally(throwable -> failure(actor, throwable));
  }

  @Override
  public CompletableFuture<Boolean> teleportHere(
      final VexPlayer actor,
      final String movingPlayerName
  ) {
    return identities.find(movingPlayerName).thenCompose(identity -> {
      if (identity.isEmpty()) {
        notFound(actor, movingPlayerName);
        return CompletableFuture.completedFuture(false);
      }
      return localPositions.capture(actor).thenCompose(destination -> destination
          .map(position -> executeOrDeliver(actor, identity.get(), actor.getName(), position))
          .orElseGet(() -> {
            presentation.send(actor, "teleport.error.position", Map.of(), "teleport-failed");
            return CompletableFuture.completedFuture(false);
          }));
    }).exceptionally(throwable -> failure(actor, throwable));
  }

  @Override
  public CompletableFuture<Boolean> teleportToPosition(
      final VexPlayer actor,
      final String movingPlayerName,
      final String destinationName,
      final ServerPosition destination
  ) {
    Objects.requireNonNull(actor, "actor");
    String checkedDestinationName = Objects.requireNonNull(destinationName, "destinationName");
    ServerPosition checkedDestination = Objects.requireNonNull(destination, "destination");
    return identities.find(movingPlayerName).thenCompose(identity -> {
      if (identity.isEmpty()) {
        notFound(actor, movingPlayerName);
        return CompletableFuture.completedFuture(false);
      }
      return executeOrDeliver(
          actor,
          identity.get(),
          checkedDestinationName,
          checkedDestination
      );
    }).exceptionally(throwable -> failure(actor, throwable));
  }

  private CompletableFuture<Boolean> executeOrDeliver(
      final VexPlayer actor,
      final PlayerIdentity moving,
      final String destinationName,
      final ServerPosition destination
  ) {
    Optional<VexPlayer> local = players.find(moving.uniqueId());
    if (local.isPresent()) {
      return teleports.teleport(moving.uniqueId(), destination, TeleportOptions.immediate())
          .thenApply(outcome -> {
            showResult(actor, moving.name(), destinationName, outcome.successful());
            if (!moving.uniqueId().equals(actor.getUniqueId())) {
              presentation.send(
                  local.get(),
                  outcome.successful()
                      ? "teleport.direct.moved"
                      : "teleport.error.unavailable",
                  Map.of("player", destinationName),
                  outcome.successful() ? "teleport-success" : "teleport-failed"
              );
            }
            return outcome.successful();
          });
    }

    return directory.find(moving.uniqueId()).thenApply(networkPlayer -> {
      if (networkPlayer.isEmpty()) {
        offline(actor, moving.name());
        return false;
      }
      DirectTeleportExecution execution = new DirectTeleportExecution(
          UUID.randomUUID(), actor.getUniqueId(), moving.uniqueId(), moving.name(), destinationName,
          destination
      );
      try {
        DeliveryResult delivery = messages.send(
            MessageTarget.player(moving.uniqueId()),
            TeleportMessages.DIRECT_EXECUTION,
            execution
        );
        if (delivery == DeliveryResult.SENT || delivery == DeliveryResult.QUEUED) {
          presentation.send(
              actor,
              "teleport.direct.queued",
              Map.of("player", moving.name()),
              ""
          );
          return true;
        }
      } catch (RuntimeException exception) {
        logger.log(Level.WARNING, "A cross-server direct teleport message could not be sent.",
            exception);
      }
      offline(actor, moving.name());
      return false;
    });
  }

  @Override
  public void receive(final DirectTeleportExecution execution) {
    Optional<VexPlayer> moving = players.find(execution.playerId());
    if (moving.isEmpty()) {
      sendCompletion(execution, false);
      return;
    }
    teleports.teleport(
        execution.playerId(),
        execution.destination(),
        TeleportOptions.immediate()
    ).thenAccept(outcome -> {
      presentation.send(
          moving.get(),
          outcome.successful() ? "teleport.direct.moved" : "teleport.error.unavailable",
          Map.of("player", execution.destinationName()),
          outcome.successful() ? "teleport-success" : "teleport-failed"
      );
      sendCompletion(execution, outcome.successful());
    });
  }

  @Override
  public void receive(final DirectTeleportCompletion completion) {
    players.find(completion.actorId()).ifPresent(actor -> showResult(
        actor,
        completion.playerName(),
        completion.destinationName(),
        completion.successful()
    ));
  }

  private void sendCompletion(final DirectTeleportExecution execution, final boolean successful) {
    DirectTeleportCompletion completion = new DirectTeleportCompletion(
        execution.operationId(), execution.actorId(), execution.playerName(),
        execution.destinationName(), successful
    );
    if (players.find(execution.actorId()).isPresent()) {
      receive(completion);
      return;
    }
    try {
      messages.send(
          MessageTarget.player(execution.actorId()),
          TeleportMessages.DIRECT_COMPLETION,
          completion
      );
    } catch (RuntimeException exception) {
      logger.log(Level.WARNING, "A direct teleport completion message could not be sent.",
          exception);
    }
  }

  private void showResult(
      final VexPlayer actor,
      final String movingPlayer,
      final String destination,
      final boolean successful
  ) {
    presentation.send(
        actor,
        successful ? "teleport.direct.other-success" : "teleport.error.unavailable",
        Map.of("player", movingPlayer, "target", destination),
        successful ? "teleport-success" : "teleport-failed"
    );
  }

  private void notFound(final VexPlayer actor, final String name) {
    presentation.send(
        actor,
        "teleport.player-not-found",
        Map.of("player", name),
        "teleport-failed"
    );
  }

  private void offline(final VexPlayer actor, final String name) {
    presentation.send(
        actor,
        "teleport.player-offline",
        Map.of("player", name),
        "teleport-failed"
    );
  }

  private boolean failure(final VexPlayer actor, final Throwable throwable) {
    logger.log(
        Level.WARNING,
        "A direct teleport could not be prepared. No player was moved.",
        throwable
    );
    presentation.send(actor, "teleport.error.unavailable", Map.of(), "teleport-failed");
    return false;
  }

  private record ResolvedPlayers(
      Optional<PlayerIdentity> moving,
      Optional<PlayerIdentity> target
  ) {
  }
}
