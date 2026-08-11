package dev.vexsoft.essentials.paper.service.teleport.execution;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.core.api.world.ServerPosition;
import dev.vexsoft.essentials.paper.teleport.messaging.direct.DirectTeleportCompletion;
import dev.vexsoft.essentials.paper.teleport.messaging.direct.DirectTeleportExecution;
import java.util.concurrent.CompletableFuture;

/** Coordinates privileged direct teleports across backend server boundaries. */
public interface DirectTeleportService extends VexService {

  CompletableFuture<Boolean> teleport(
      VexPlayer actor,
      String movingPlayerName,
      String targetName
  );

  CompletableFuture<Boolean> teleportHere(VexPlayer actor, String movingPlayerName);

  CompletableFuture<Boolean> teleportToPosition(
      VexPlayer actor,
      String movingPlayerName,
      String destinationName,
      ServerPosition destination
  );

  void receive(DirectTeleportExecution execution);

  void receive(DirectTeleportCompletion completion);
}
