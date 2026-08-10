package dev.vexsoft.essentials.paper.service.teleport.position;

import dev.vexsoft.core.api.messaging.DeliveryResult;
import dev.vexsoft.core.api.messaging.MessageTarget;
import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.messaging.MessagingService;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.core.paper.service.network.ServerIdentityService;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.essentials.paper.teleport.messaging.position.PlayerPositionRequest;
import dev.vexsoft.essentials.paper.teleport.messaging.position.PlayerPositionResponse;
import dev.vexsoft.essentials.paper.teleport.messaging.TeleportMessages;
import dev.vexsoft.essentials.paper.service.teleport.configuration.TeleportConfigurationService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/** Message-backed player position resolver with bounded, configurable requests. */
@Dependencies({
    PlayerService.class,
    TeleportPositionService.class,
    MessagingService.class,
    ServerIdentityService.class,
    ScheduleService.class,
    TeleportConfigurationService.class
})
public final class VexNetworkPositionService implements NetworkPositionService, AutoCloseable {

  private final PlayerService players;
  private final TeleportPositionService positions;
  private final MessagingService messages;
  private final ServerIdentityService serverIdentity;
  private final ScheduleService scheduler;
  private final Logger logger;
  private final TeleportConfigurationService configuration;
  private final Map<UUID, CompletableFuture<Optional<ServerPosition>>> pending =
      new ConcurrentHashMap<>();

  /** Creates the network resolver. */
  public VexNetworkPositionService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    players = checked.require(PlayerService.class);
    positions = checked.require(TeleportPositionService.class);
    messages = checked.require(MessagingService.class);
    serverIdentity = checked.require(ServerIdentityService.class);
    scheduler = checked.require(ScheduleService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
    configuration = checked.require(TeleportConfigurationService.class);
  }

  @Override
  public CompletableFuture<Optional<ServerPosition>> resolve(final UUID playerId) {
    UUID checkedPlayerId = Objects.requireNonNull(playerId, "playerId");
    Optional<VexPlayer> localPlayer = players.find(checkedPlayerId);
    if (localPlayer.isPresent()) {
      return positions.capture(localPlayer.get());
    }

    UUID requestId = UUID.randomUUID();
    CompletableFuture<Optional<ServerPosition>> future = new CompletableFuture<>();
    pending.put(requestId, future);
    DeliveryResult delivery;
    try {
      delivery = messages.send(
          MessageTarget.player(checkedPlayerId),
          TeleportMessages.POSITION_REQUEST,
          new PlayerPositionRequest(
              requestId,
              checkedPlayerId,
              serverIdentity.getServerId().value()
          )
      );
    } catch (RuntimeException exception) {
      logger.warning(
          "A cross-server player position request could not be sent, so the teleport was "
              + "stopped safely. Reason: " + exception.getMessage()
      );
      pending.remove(requestId);
      future.complete(Optional.empty());
      return future;
    }
    if (delivery != DeliveryResult.SENT && delivery != DeliveryResult.QUEUED) {
      pending.remove(requestId);
      future.complete(Optional.empty());
      return future;
    }
    scheduler.runAsyncLater(configuration.networkTimeout(), () -> {
      CompletableFuture<Optional<ServerPosition>> expired = pending.remove(requestId);
      if (expired != null) {
        expired.complete(Optional.empty());
      }
    });
    return future;
  }

  @Override
  public void accept(final PlayerPositionResponse response) {
    PlayerPositionResponse checked = Objects.requireNonNull(response, "response");
    CompletableFuture<Optional<ServerPosition>> future = pending.remove(checked.requestId());
    if (future != null) {
      future.complete(Optional.ofNullable(checked.position()));
    }
  }

  @Override
  public void close() {
    pending.values().forEach(future -> future.complete(Optional.empty()));
    pending.clear();
  }
}
