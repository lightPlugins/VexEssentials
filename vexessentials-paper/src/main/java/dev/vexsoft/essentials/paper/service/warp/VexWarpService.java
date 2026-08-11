package dev.vexsoft.essentials.paper.service.warp;

import dev.vexsoft.core.api.messaging.DeliveryResult;
import dev.vexsoft.core.api.messaging.MessageTarget;
import dev.vexsoft.core.api.service.globaldata.GlobalDataService;
import dev.vexsoft.core.api.service.messaging.MessagingService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.essentials.api.service.warp.WarpService;
import dev.vexsoft.essentials.api.warp.Warp;
import dev.vexsoft.essentials.api.warp.WarpChangeStatus;
import dev.vexsoft.essentials.paper.warp.data.VexEssentialsGlobalData;
import dev.vexsoft.essentials.paper.warp.data.WarpRegistry;
import dev.vexsoft.essentials.paper.warp.messaging.WarpMessages;
import dev.vexsoft.essentials.paper.warp.messaging.WarpRegistryChanged;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

/** Global-data backed warp registry with immutable reads and cross-server refreshes. */
@Dependencies({GlobalDataService.class, MessagingService.class, ScheduleService.class})
public final class VexWarpService implements WarpService, AutoCloseable {

  private final GlobalDataService globalData;
  private final MessagingService messages;
  private final ScheduleService scheduler;
  private final Logger logger;
  private final Set<String> ownedPermissions = new HashSet<>();
  private final CompletableFuture<Void> initialLoad = new CompletableFuture<>();
  private final AtomicBoolean initialized = new AtomicBoolean();
  private volatile Map<String, Warp> snapshot = Map.of();

  /** Creates the registry and begins its non-blocking initial load. */
  public VexWarpService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    globalData = checked.require(GlobalDataService.class);
    messages = checked.require(MessagingService.class);
    scheduler = checked.require(ScheduleService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public void initialize() {
    if (!initialized.compareAndSet(false, true)) {
      return;
    }
    refresh().whenComplete((ignored, throwable) -> {
      if (throwable == null) {
        initialLoad.complete(null);
      } else {
        initialLoad.completeExceptionally(throwable);
      }
    });
  }

  @Override
  public CompletableFuture<Optional<Warp>> find(final String id) {
    String normalized = Warp.normalizeId(id);
    return initialLoad.thenApply(ignored -> Optional.ofNullable(snapshot.get(normalized)));
  }

  @Override
  public Collection<Warp> getWarps() {
    return List.copyOf(snapshot.values());
  }

  @Override
  public CompletableFuture<Collection<Warp>> getWarpsAsync() {
    return initialLoad.thenApply(ignored -> getWarps());
  }

  @Override
  public CompletableFuture<WarpChangeStatus> create(
      final String id,
      final ServerPosition position,
      final UUID creator
  ) {
    String normalized = Warp.normalizeId(id);
    ServerPosition checkedPosition = Objects.requireNonNull(position, "position");
    UUID checkedCreator = Objects.requireNonNull(creator, "creator");
    AtomicReference<WarpChangeStatus> status = new AtomicReference<>();
    return initialLoad.thenCompose(ignored -> globalData.update(
        VexEssentialsGlobalData.WARPS,
        registry -> {
          if (registry.warps().containsKey(normalized)) {
            status.set(WarpChangeStatus.ALREADY_EXISTS);
            return registry;
          }
          Instant now = Instant.now();
          status.set(WarpChangeStatus.CREATED);
          return registry.with(new Warp(normalized, checkedPosition, checkedCreator, now, now));
        }
    )).thenApply(registry -> completeMutation(registry, status.get()));
  }

  @Override
  public CompletableFuture<WarpChangeStatus> update(
      final String id,
      final ServerPosition position
  ) {
    String normalized = Warp.normalizeId(id);
    ServerPosition checkedPosition = Objects.requireNonNull(position, "position");
    AtomicReference<WarpChangeStatus> status = new AtomicReference<>();
    return initialLoad.thenCompose(ignored -> globalData.update(
        VexEssentialsGlobalData.WARPS,
        registry -> {
          Warp current = registry.warps().get(normalized);
          if (current == null) {
            status.set(WarpChangeStatus.NOT_FOUND);
            return registry;
          }
          status.set(WarpChangeStatus.UPDATED);
          return registry.with(new Warp(
              current.id(),
              checkedPosition,
              current.createdBy(),
              current.createdAt(),
              Instant.now()
          ));
        }
    )).thenApply(registry -> completeMutation(registry, status.get()));
  }

  @Override
  public CompletableFuture<WarpChangeStatus> delete(final String id) {
    String normalized = Warp.normalizeId(id);
    AtomicReference<WarpChangeStatus> status = new AtomicReference<>();
    return initialLoad.thenCompose(ignored -> globalData.update(
        VexEssentialsGlobalData.WARPS,
        registry -> {
          if (!registry.warps().containsKey(normalized)) {
            status.set(WarpChangeStatus.NOT_FOUND);
            return registry;
          }
          status.set(WarpChangeStatus.DELETED);
          return registry.without(normalized);
        }
    )).thenApply(registry -> completeMutation(registry, status.get()));
  }

  @Override
  public CompletableFuture<Void> refresh() {
    if (!initialized.get()) {
      return CompletableFuture.failedFuture(new IllegalStateException(
          "The warp service has not been initialized"
      ));
    }
    return globalData.get(VexEssentialsGlobalData.WARPS)
        .thenAccept(this::applySnapshot)
        .whenComplete((ignored, throwable) -> {
          if (throwable != null) {
            logger.log(
                Level.WARNING,
                "The shared warp registry could not be loaded. Existing runtime warps remain "
                    + "available, but remote changes may be delayed.",
                throwable
            );
          }
        });
  }

  @Override
  public void synchronizePermissions() {
    synchronizePermissions(snapshot);
  }

  private WarpChangeStatus completeMutation(
      final WarpRegistry registry,
      final WarpChangeStatus status
  ) {
    WarpChangeStatus checkedStatus = Objects.requireNonNull(status, "status");
    applySnapshot(registry);
    if (checkedStatus == WarpChangeStatus.CREATED
        || checkedStatus == WarpChangeStatus.UPDATED
        || checkedStatus == WarpChangeStatus.DELETED) {
      broadcastChange();
    }
    return checkedStatus;
  }

  private void applySnapshot(final WarpRegistry registry) {
    snapshot = Objects.requireNonNull(registry, "registry").warps();
    if (!scheduler.getOwner().isEnabled()) {
      return;
    }
    try {
      scheduler.runGlobal(() -> synchronizePermissions(snapshot));
    } catch (RuntimeException exception) {
      logger.log(
          Level.WARNING,
          "Warp access permissions could not be synchronized with the current registry.",
          exception
      );
    }
  }

  private void synchronizePermissions(final Map<String, Warp> current) {
    PluginManager plugins = Bukkit.getPluginManager();
    Set<String> required = new HashSet<>();
    current.values().forEach(warp -> {
      String permission = warp.accessPermission();
      required.add(permission);
      if (plugins.getPermission(permission) == null) {
        plugins.addPermission(new Permission(
            permission,
            "Allows access to the VexEssentials warp '" + warp.id() + "'",
            PermissionDefault.OP
        ));
        ownedPermissions.add(permission);
      }
    });
    ownedPermissions.removeIf(permission -> {
      if (required.contains(permission)) {
        return false;
      }
      plugins.removePermission(permission);
      return true;
    });
  }

  private void broadcastChange() {
    try {
      DeliveryResult result = messages.send(
          MessageTarget.allServers(),
          WarpMessages.REGISTRY_CHANGED,
          new WarpRegistryChanged(UUID.randomUUID())
      );
      if (result != DeliveryResult.SENT && result != DeliveryResult.QUEUED) {
        logger.warning(
            "The warp change was saved, but other servers could not be notified immediately. "
                + "Delivery result: " + result
        );
      }
    } catch (RuntimeException exception) {
      logger.log(
          Level.WARNING,
          "The warp change was saved, but other servers could not be notified immediately.",
          exception
      );
    }
  }

  @Override
  public void close() {
    PluginManager plugins = Bukkit.getPluginManager();
    ownedPermissions.forEach(plugins::removePermission);
    ownedPermissions.clear();
    snapshot = Map.of();
  }
}
