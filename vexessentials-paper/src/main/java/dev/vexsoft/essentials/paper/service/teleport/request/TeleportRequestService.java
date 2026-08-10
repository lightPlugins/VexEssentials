package dev.vexsoft.essentials.paper.service.teleport.request;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestType;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestCompletion;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestDecision;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestExecution;
import dev.vexsoft.essentials.paper.teleport.messaging.request.TeleportRequestOffer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Owns bounded, runtime-only teleport requests and their atomic state transitions. */
public interface TeleportRequestService extends VexService {

  CompletableFuture<Boolean> send(VexPlayer requester, String targetName, TeleportRequestType type);

  CompletableFuture<Boolean> review(VexPlayer target, String selector);

  boolean deny(VexPlayer target, String selector);

  boolean cancel(VexPlayer requester);

  /** Returns pending requester names suitable for command suggestions. */
  List<String> getIncomingSuggestions(UUID targetId);

  void receive(TeleportRequestOffer offer);

  void receive(TeleportRequestDecision decision);

  void receive(TeleportRequestExecution execution);

  void receive(TeleportRequestCompletion completion);
}
