package dev.vexsoft.essentials.api.service.teleport;

import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.essentials.api.teleport.TeleportOptions;
import dev.vexsoft.essentials.api.teleport.TeleportOutcome;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Executes local and network-aware player teleports through VexCore. */
public interface EssentialsTeleportService extends VexService {

  /** Teleports a loaded player to an exact server position. */
  CompletableFuture<TeleportOutcome> teleport(
      UUID playerId,
      ServerPosition destination,
      TeleportOptions options
  );

  /** Resolves an online target and teleports the player to the target's current position. */
  CompletableFuture<TeleportOutcome> teleportToPlayer(
      UUID playerId,
      UUID targetId,
      TeleportOptions options
  );
}
