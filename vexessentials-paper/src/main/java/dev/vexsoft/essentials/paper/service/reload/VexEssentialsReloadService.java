package dev.vexsoft.essentials.paper.service.reload;

import dev.vexsoft.core.api.service.localization.LocalizationService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.service.teleport.configuration.TeleportConfigurationService;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Coordinates safe runtime reloads for all VexEssentials features. */
@Dependencies({LocalizationService.class, TeleportConfigurationService.class})
public final class VexEssentialsReloadService implements EssentialsReloadService {

  private final LocalizationService localization;
  private final TeleportConfigurationService teleportConfiguration;
  private final Logger logger;

  /** Creates the owner-scoped reload coordinator. */
  public VexEssentialsReloadService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    localization = checked.require(LocalizationService.class);
    teleportConfiguration = checked.require(TeleportConfigurationService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public boolean reload() {
    boolean successful = teleportConfiguration.reload();
    try {
      localization.reload();
    } catch (RuntimeException exception) {
      logger.log(
          Level.WARNING,
          "VexEssentials could not reload its language files. Check the files in "
              + "'plugins/VexSoft/VexEssentials/languages' and try again.",
          exception
      );
      successful = false;
    }
    return successful;
  }
}
