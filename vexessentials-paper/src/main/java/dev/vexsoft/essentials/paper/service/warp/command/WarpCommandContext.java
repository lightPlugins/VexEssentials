package dev.vexsoft.essentials.paper.service.warp.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.core.paper.command.VexCommandSource;

/** Resolves shared player context for the separate warp command binding classes. */
public interface WarpCommandContext extends VexService {

  VexPlayer player(VexCommandSource source);
}
