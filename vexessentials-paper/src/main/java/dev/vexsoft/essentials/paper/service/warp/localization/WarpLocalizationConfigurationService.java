package dev.vexsoft.essentials.paper.service.warp.localization;

import dev.vexsoft.core.api.service.registry.VexService;

/** Creates presentation entries for newly created warps in every local language. */
public interface WarpLocalizationConfigurationService extends VexService {

  void createDefaults(String warpId, String displayName);
}
