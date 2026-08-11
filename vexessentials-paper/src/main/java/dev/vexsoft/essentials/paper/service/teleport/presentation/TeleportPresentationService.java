package dev.vexsoft.essentials.paper.service.teleport.presentation;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestType;
import dev.vexsoft.essentials.paper.teleport.presentation.RequestDialogChoice;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Centralizes localized chat, hover, dialog, and sound presentation for teleports. */
public interface TeleportPresentationService extends VexService {

  void send(VexPlayer player, String key, Map<String, String> replacements, String soundEvent);

  void sendWithHover(
      VexPlayer player,
      String messageKey,
      String hoverKey,
      Map<String, String> replacements,
      String soundEvent
  );

  void sendInteractiveRequest(
      VexPlayer player,
      TeleportRequestType type,
      String requesterName,
      Duration expiration,
      Duration lifetime,
      Runnable reviewAction
  );

  CompletableFuture<RequestDialogChoice> openRequestDialog(
      VexPlayer player,
      String requesterName,
      TeleportRequestType type,
      Duration expiration,
      Duration remaining
  );
}
