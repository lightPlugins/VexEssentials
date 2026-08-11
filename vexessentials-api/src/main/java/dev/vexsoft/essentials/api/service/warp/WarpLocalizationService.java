package dev.vexsoft.essentials.api.service.warp;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.essentials.api.warp.Warp;
import java.util.List;
import net.kyori.adventure.text.Component;

/** Resolves player-language-aware warp presentation values. */
public interface WarpLocalizationService extends VexService {

  /** Resolves the localized display name, falling back to the stable warp ID. */
  Component getName(VexPlayer player, Warp warp);

  /** Resolves the localized multi-line description, or an empty list when it is undefined. */
  List<Component> getDescription(VexPlayer player, Warp warp);
}
