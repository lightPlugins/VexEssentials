package dev.vexsoft.essentials.paper.service.reload;

import dev.vexsoft.core.api.service.registry.VexService;

/** Reloads the runtime configuration owned by VexEssentials. */
public interface EssentialsReloadService extends VexService {

  /** Reloads every supported VexEssentials configuration and localization resource. */
  boolean reload();
}
