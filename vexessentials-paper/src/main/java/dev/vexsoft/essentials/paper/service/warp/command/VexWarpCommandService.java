package dev.vexsoft.essentials.paper.service.warp.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.essentials.api.service.teleport.EssentialsTeleportService;
import dev.vexsoft.essentials.api.service.warp.WarpLocalizationService;
import dev.vexsoft.essentials.api.service.warp.WarpService;
import dev.vexsoft.essentials.api.teleport.TeleportOptions;
import dev.vexsoft.essentials.api.warp.Warp;
import dev.vexsoft.essentials.api.warp.WarpChangeStatus;
import dev.vexsoft.essentials.paper.service.teleport.execution.DirectTeleportService;
import dev.vexsoft.essentials.paper.service.teleport.position.TeleportPositionService;
import dev.vexsoft.essentials.paper.service.warp.presentation.WarpPresentationService;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

/** Default asynchronous command workflow implementation for the warp feature. */
@Dependencies({
    WarpService.class,
    WarpLocalizationService.class,
    WarpPresentationService.class,
    EssentialsTeleportService.class,
    DirectTeleportService.class,
    TeleportPositionService.class
})
public final class VexWarpCommandService implements WarpCommandService {

  private final WarpService warps;
  private final WarpLocalizationService localization;
  private final WarpPresentationService presentation;
  private final EssentialsTeleportService teleports;
  private final DirectTeleportService directTeleports;
  private final TeleportPositionService positions;
  private final Logger logger;

  /** Creates the warp command coordinator. */
  public VexWarpCommandService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    warps = checked.require(WarpService.class);
    localization = checked.require(WarpLocalizationService.class);
    presentation = checked.require(WarpPresentationService.class);
    teleports = checked.require(EssentialsTeleportService.class);
    directTeleports = checked.require(DirectTeleportService.class);
    positions = checked.require(TeleportPositionService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public CompletableFuture<Boolean> teleport(final VexPlayer player, final String warpId) {
    return find(player, warpId).thenCompose(warp -> {
      if (warp.isEmpty()) {
        return CompletableFuture.completedFuture(false);
      }
      Warp destination = warp.get();
      Optional<Player> platformPlayer = player.findPlatformPlayer(Player.class);
      if (platformPlayer.isEmpty()) {
        return CompletableFuture.completedFuture(false);
      }
      if (!platformPlayer.get().hasPermission(destination.accessPermission())) {
        presentation.send(
            player,
            "warp.access-denied",
            Map.of("warp", displayName(player, destination)),
            "teleport-failed"
        );
        return CompletableFuture.completedFuture(false);
      }
      return teleports.teleport(
          player.getUniqueId(),
          destination.position(),
          TeleportOptions.defaults()
      ).thenApply(outcome -> {
        presentation.send(
            player,
            outcome.successful() ? "warp.teleport-success" : "warp.teleport-failed",
            Map.of("warp", displayName(player, destination)),
            outcome.successful() ? "teleport-success" : "teleport-failed"
        );
        return outcome.successful();
      });
    }).exceptionally(throwable -> failure(player, "teleport", throwable));
  }

  @Override
  public CompletableFuture<Boolean> teleportOther(
      final VexPlayer actor,
      final String playerName,
      final String warpId
  ) {
    return find(actor, warpId).thenCompose(warp -> warp
        .map(destination -> directTeleports.teleportToPosition(
            actor,
            playerName,
            displayName(actor, destination),
            destination.position()
        ))
        .orElseGet(() -> CompletableFuture.completedFuture(false)))
        .exceptionally(throwable -> failure(actor, "teleport another player", throwable));
  }

  @Override
  public CompletableFuture<Boolean> list(final VexPlayer player) {
    return warps.getWarpsAsync().thenApply(available -> {
      Optional<Player> platformPlayer = player.findPlatformPlayer(Player.class);
      if (platformPlayer.isEmpty()) {
        return false;
      }
      Collection<Warp> accessible = available.stream()
          .filter(warp -> platformPlayer.get().hasPermission(warp.accessPermission()))
          .toList();
      presentation.sendList(player, accessible);
      return true;
    }).exceptionally(throwable -> failure(player, "list warps", throwable));
  }

  @Override
  public CompletableFuture<Boolean> create(final VexPlayer player, final String warpId) {
    String normalized = normalize(player, warpId);
    if (normalized == null) {
      return CompletableFuture.completedFuture(false);
    }
    return positions.capture(player).thenCompose(position -> position
        .map(value -> create(player, normalized, value))
        .orElseGet(() -> {
          presentation.send(player, "warp.position-unavailable", Map.of(), "teleport-failed");
          return CompletableFuture.completedFuture(false);
        })).exceptionally(throwable -> failure(player, "create warp '" + normalized + "'", throwable));
  }

  @Override
  public CompletableFuture<Boolean> update(final VexPlayer player, final String warpId) {
    String normalized = normalize(player, warpId);
    if (normalized == null) {
      return CompletableFuture.completedFuture(false);
    }
    return positions.capture(player).thenCompose(position -> position
        .map(value -> update(player, normalized, value))
        .orElseGet(() -> {
          presentation.send(player, "warp.position-unavailable", Map.of(), "teleport-failed");
          return CompletableFuture.completedFuture(false);
        })).exceptionally(throwable -> failure(player, "update warp '" + normalized + "'", throwable));
  }

  @Override
  public CompletableFuture<Boolean> delete(final VexPlayer player, final String warpId) {
    return find(player, warpId).thenCompose(warp -> warp
        .map(value -> presentation.confirmDelete(player, value).thenCompose(confirmed -> confirmed
            ? deleteConfirmed(player, value)
            : CompletableFuture.completedFuture(false)))
        .orElseGet(() -> CompletableFuture.completedFuture(false)))
        .exceptionally(throwable -> failure(player, "delete warp '" + warpId + "'", throwable));
  }

  private CompletableFuture<Optional<Warp>> find(
      final VexPlayer player,
      final String warpId
  ) {
    String normalized = normalize(player, warpId);
    if (normalized == null) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    return warps.find(normalized).thenApply(warp -> {
      if (warp.isEmpty()) {
        presentation.send(
            player,
            "warp.not-found",
            Map.of("warp", normalized),
            "teleport-failed"
        );
      }
      return warp;
    });
  }

  private CompletableFuture<Boolean> create(
      final VexPlayer player,
      final String id,
      final ServerPosition position
  ) {
    return warps.create(id, position, player.getUniqueId()).thenApply(status -> {
      String key = status == WarpChangeStatus.CREATED
          ? "warp.created"
          : "warp.already-exists";
      presentation.send(player, key, Map.of("warp", id), "");
      return status == WarpChangeStatus.CREATED;
    });
  }

  private CompletableFuture<Boolean> update(
      final VexPlayer player,
      final String id,
      final ServerPosition position
  ) {
    return warps.update(id, position).thenApply(status -> {
      String key = status == WarpChangeStatus.UPDATED ? "warp.updated" : "warp.not-found";
      presentation.send(player, key, Map.of("warp", id), "");
      return status == WarpChangeStatus.UPDATED;
    });
  }

  private CompletableFuture<Boolean> deleteConfirmed(
      final VexPlayer player,
      final Warp warp
  ) {
    return warps.delete(warp.id()).thenApply(status -> {
      String key = status == WarpChangeStatus.DELETED ? "warp.deleted" : "warp.not-found";
      presentation.send(player, key, Map.of("warp", warp.id()), "");
      return status == WarpChangeStatus.DELETED;
    });
  }

  private String normalize(final VexPlayer player, final String id) {
    try {
      return Warp.normalizeId(id);
    } catch (IllegalArgumentException exception) {
      presentation.send(player, "warp.invalid-id", Map.of("warp", id), "teleport-failed");
      return null;
    }
  }

  private String displayName(final VexPlayer player, final Warp warp) {
    return PlainTextComponentSerializer.plainText().serialize(localization.getName(player, warp));
  }

  private boolean failure(
      final VexPlayer player,
      final String operation,
      final Throwable throwable
  ) {
    logger.log(
        Level.WARNING,
        "VexEssentials could not " + operation + ". The stored warp data was left unchanged.",
        throwable
    );
    presentation.send(player, "warp.storage-error", Map.of(), "teleport-failed");
    return false;
  }
}
