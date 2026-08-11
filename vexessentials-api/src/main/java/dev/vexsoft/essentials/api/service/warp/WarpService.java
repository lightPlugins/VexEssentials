package dev.vexsoft.essentials.api.service.warp;

import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.essentials.api.warp.Warp;
import dev.vexsoft.essentials.api.warp.WarpChangeStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Manages the shared, persistent warp registry for the complete server network. */
public interface WarpService extends VexService {

  /** Starts the initial load after the plugin has registered its global-data definition. */
  void initialize();

  /** Resolves a warp after the initial global registry load has completed. */
  CompletableFuture<Optional<Warp>> find(String id);

  /** Returns the current immutable runtime snapshot for fast suggestions and display. */
  Collection<Warp> getWarps();

  /** Returns the immutable runtime snapshot once its initial global load has completed. */
  CompletableFuture<Collection<Warp>> getWarpsAsync();

  /** Creates a new warp without replacing an existing identifier. */
  CompletableFuture<WarpChangeStatus> create(
      String id,
      ServerPosition position,
      UUID creator
  );

  /** Updates the position of an existing warp. */
  CompletableFuture<WarpChangeStatus> update(String id, ServerPosition position);

  /** Deletes an existing warp from persistent and runtime storage. */
  CompletableFuture<WarpChangeStatus> delete(String id);

  /** Reloads the authoritative registry after a remote server changed it. */
  CompletableFuture<Void> refresh();

  /** Registers the deterministic access permissions for the current runtime snapshot. */
  void synchronizePermissions();
}
