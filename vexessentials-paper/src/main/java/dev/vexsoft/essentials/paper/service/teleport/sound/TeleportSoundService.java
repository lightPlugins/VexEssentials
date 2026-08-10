package dev.vexsoft.essentials.paper.service.teleport.sound;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.VexService;

/** Plays configured teleport sound events for loaded players. */
public interface TeleportSoundService extends VexService {

  void play(VexPlayer player, String event);
}
