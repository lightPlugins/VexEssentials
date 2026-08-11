package dev.vexsoft.essentials.paper.service.warp.presentation;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.essentials.api.warp.Warp;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Centralizes localized warp chat, hover, dialog, and sound presentation. */
public interface WarpPresentationService extends VexService {

  void send(VexPlayer player, String key, Map<String, String> replacements, String soundEvent);

  void sendList(VexPlayer player, Collection<Warp> warps);

  CompletableFuture<Boolean> confirmDelete(VexPlayer player, Warp warp);
}
