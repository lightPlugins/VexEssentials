package dev.vexsoft.essentials.paper.service.teleport.execution;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.scheduler.VexTask;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.essentials.paper.service.teleport.configuration.TeleportConfigurationService;
import dev.vexsoft.essentials.paper.service.teleport.presentation.TeleportPresentationService;
import dev.vexsoft.essentials.paper.teleport.execution.WarmupCancelReason;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

/** Entity-scheduled warmup implementation with constant-time cancellation lookups. */
@Dependencies({
    TeleportConfigurationService.class,
    TeleportPresentationService.class,
    ScheduleService.class
})
public final class VexTeleportWarmupService implements TeleportWarmupService, AutoCloseable {

  private final TeleportConfigurationService configuration;
  private final TeleportPresentationService presentation;
  private final ScheduleService scheduler;
  private final Logger logger;
  private final Map<UUID, WarmupSession> sessions = new ConcurrentHashMap<>();

  /** Creates the warmup coordinator. */
  public VexTeleportWarmupService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    configuration = checked.require(TeleportConfigurationService.class);
    presentation = checked.require(TeleportPresentationService.class);
    scheduler = checked.require(ScheduleService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public CompletableFuture<Boolean> begin(final VexPlayer player) {
    Objects.requireNonNull(player, "player");
    Duration duration = configuration.warmup();
    if (duration.isZero()) {
      return CompletableFuture.completedFuture(true);
    }

    UUID playerId = player.getUniqueId();
    Optional<Player> platformPlayer = player.findPlatformPlayer(Player.class);
    if (platformPlayer.isEmpty()) {
      return CompletableFuture.completedFuture(false);
    }
    if (platformPlayer.get().hasPermission(configuration.warmupBypassPermission())) {
      return CompletableFuture.completedFuture(true);
    }
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    WarmupSession session = new WarmupSession(player, future, duration.toSeconds());
    WarmupSession previous = sessions.put(playerId, session);
    if (previous != null) {
      previous.cancel();
    }
    presentation.send(
        player,
        "teleport.warmup.started",
        Map.of("remaining_seconds", Long.toString(duration.toSeconds())),
        "warmup-start"
    );
    try {
      scheduler.runForTimer(
          platformPlayer.get(),
          20,
          20,
          () -> tick(playerId, session),
          () -> cancel(playerId, WarmupCancelReason.LEFT)
      ).ifPresentOrElse(session::setTask, () -> cancel(playerId, WarmupCancelReason.LEFT));
    } catch (RuntimeException exception) {
      sessions.remove(playerId, session);
      session.cancel();
      logger.warning(
          "The teleport warmup for player '" + player.getName() + "' could not be scheduled, "
              + "so the teleport was stopped safely. Reason: " + exception.getMessage()
      );
    }
    return future;
  }

  @Override
  public void cancel(final UUID playerId, final WarmupCancelReason reason) {
    WarmupSession session = sessions.remove(Objects.requireNonNull(playerId, "playerId"));
    if (session == null) {
      return;
    }
    session.cancel();
    String key = switch (Objects.requireNonNull(reason, "reason")) {
      case MOVED -> "teleport.warmup.cancelled-move";
      case DAMAGED -> "teleport.warmup.cancelled-damage";
      case LEFT, REPLACED -> null;
    };
    if (key != null) {
      presentation.send(session.player(), key, Map.of(), "teleport-cancelled");
    }
  }

  @Override
  public boolean hasWarmup(final UUID playerId) {
    return sessions.containsKey(Objects.requireNonNull(playerId, "playerId"));
  }

  private void complete(final UUID playerId, final WarmupSession expected) {
    if (sessions.remove(playerId, expected)) {
      expected.complete();
    }
  }

  private void tick(final UUID playerId, final WarmupSession expected) {
    if (sessions.get(playerId) != expected) {
      return;
    }
    long remaining = expected.decrementRemainingSeconds();
    if (remaining <= 0) {
      complete(playerId, expected);
      return;
    }
    presentation.send(
        expected.player(),
        "teleport.warmup.countdown",
        Map.of("remaining_seconds", Long.toString(remaining)),
        ""
    );
  }

  @Override
  public void close() {
    sessions.values().forEach(WarmupSession::cancel);
    sessions.clear();
  }

  private static final class WarmupSession {

    private final VexPlayer player;
    private final CompletableFuture<Boolean> future;
    private long remainingSeconds;
    private volatile VexTask task;

    private WarmupSession(
        final VexPlayer player,
        final CompletableFuture<Boolean> future,
        final long remainingSeconds
    ) {
      this.player = player;
      this.future = future;
      this.remainingSeconds = remainingSeconds;
    }

    private VexPlayer player() {
      return player;
    }

    private void setTask(final VexTask task) {
      this.task = Objects.requireNonNull(task, "task");
      if (future.isDone()) {
        task.cancel();
      }
    }

    private void complete() {
      cancelTask();
      future.complete(true);
    }

    private void cancel() {
      cancelTask();
      future.complete(false);
    }

    private long decrementRemainingSeconds() {
      return --remainingSeconds;
    }

    private void cancelTask() {
      VexTask currentTask = task;
      if (currentTask != null) {
        currentTask.cancel();
      }
    }
  }
}
