package dev.vexsoft.essentials.paper;

import dev.vexsoft.core.api.service.globaldata.GlobalDataService;
import dev.vexsoft.core.api.service.messaging.MessagingService;
import dev.vexsoft.core.api.service.player.DataService;
import dev.vexsoft.core.api.service.player.PlayerContainerService;
import dev.vexsoft.core.paper.plugin.VexPlugin;
import dev.vexsoft.core.paper.service.commands.CommandService;
import dev.vexsoft.core.paper.service.listeners.ListenerService;
import dev.vexsoft.essentials.api.service.teleport.EssentialsTeleportService;
import dev.vexsoft.essentials.api.service.warp.WarpLocalizationService;
import dev.vexsoft.essentials.api.service.warp.WarpService;
import dev.vexsoft.essentials.api.teleport.container.TeleportContainer;
import dev.vexsoft.essentials.paper.command.VexEssentialsReloadCommand;
import dev.vexsoft.essentials.paper.service.reload.EssentialsReloadService;
import dev.vexsoft.essentials.paper.service.reload.VexEssentialsReloadService;
import dev.vexsoft.essentials.paper.service.teleport.configuration.TeleportConfigurationService;
import dev.vexsoft.essentials.paper.service.teleport.configuration.VexTeleportConfigurationService;
import dev.vexsoft.essentials.paper.service.teleport.container.VexTeleportContainer;
import dev.vexsoft.essentials.paper.service.teleport.execution.VexEssentialsTeleportService;
import dev.vexsoft.essentials.paper.service.teleport.execution.DirectTeleportService;
import dev.vexsoft.essentials.paper.service.teleport.execution.VexDirectTeleportService;
import dev.vexsoft.essentials.paper.service.teleport.execution.TeleportWarmupService;
import dev.vexsoft.essentials.paper.service.teleport.execution.VexTeleportWarmupService;
import dev.vexsoft.essentials.paper.service.teleport.position.NetworkPositionService;
import dev.vexsoft.essentials.paper.service.teleport.position.TeleportPositionService;
import dev.vexsoft.essentials.paper.service.teleport.position.VexNetworkPositionService;
import dev.vexsoft.essentials.paper.service.teleport.position.VexTeleportPositionService;
import dev.vexsoft.essentials.paper.service.teleport.presentation.TeleportPresentationService;
import dev.vexsoft.essentials.paper.service.teleport.presentation.VexTeleportPresentationService;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import dev.vexsoft.essentials.paper.service.teleport.request.VexTeleportRequestService;
import dev.vexsoft.essentials.paper.service.teleport.sound.TeleportSoundService;
import dev.vexsoft.essentials.paper.service.teleport.sound.VexTeleportSoundService;
import dev.vexsoft.essentials.paper.service.warp.VexWarpService;
import dev.vexsoft.essentials.paper.service.warp.command.VexWarpCommandContext;
import dev.vexsoft.essentials.paper.service.warp.command.VexWarpCommandService;
import dev.vexsoft.essentials.paper.service.warp.command.WarpCommandContext;
import dev.vexsoft.essentials.paper.service.warp.command.WarpCommandService;
import dev.vexsoft.essentials.paper.service.warp.localization.VexWarpLocalizationService;
import dev.vexsoft.essentials.paper.service.warp.localization.WarpLocalizationConfigurationService;
import dev.vexsoft.essentials.paper.service.warp.localization.VexWarpLocalizationConfigurationService;
import dev.vexsoft.essentials.paper.service.warp.presentation.VexWarpPresentationService;
import dev.vexsoft.essentials.paper.service.warp.presentation.WarpPresentationService;
import dev.vexsoft.essentials.paper.teleport.command.BackCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportRequestCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportAcceptCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportAskHereCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportCancelCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportDenyCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportToggleCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportHereCommand;
import dev.vexsoft.essentials.paper.teleport.data.VexEssentialsPlayerData;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.position.PlayerPositionRequestHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.position.PlayerPositionResponseHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.direct.DirectTeleportCompletionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.direct.DirectTeleportExecutionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.request.TeleportRequestCompletionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.request.TeleportRequestAdmissionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.request.TeleportRequestDecisionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.request.TeleportRequestExecutionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.request.TeleportRequestOfferHandler;
import dev.vexsoft.essentials.paper.teleport.listener.TeleportWarmupListener;
import dev.vexsoft.essentials.paper.warp.command.WarpCreateCommand;
import dev.vexsoft.essentials.paper.warp.command.WarpDeleteCommand;
import dev.vexsoft.essentials.paper.warp.command.WarpListCommand;
import dev.vexsoft.essentials.paper.warp.command.WarpTeleportCommand;
import dev.vexsoft.essentials.paper.warp.command.WarpUpdateCommand;
import dev.vexsoft.essentials.paper.warp.data.VexEssentialsGlobalData;
import dev.vexsoft.essentials.paper.warp.messaging.handler.WarpRegistryChangedHandler;

/**
 * Starts VexEssentials and connects it to the shared VexCore infrastructure
 */
public final class VexEssentialsPlugin extends VexPlugin {

  @Override
  protected void registerServices() {
    getServices().register(
        TeleportConfigurationService.class,
        VexTeleportConfigurationService.class
    );
    getServices().register(TeleportPositionService.class, VexTeleportPositionService.class);
    getServices().register(NetworkPositionService.class, VexNetworkPositionService.class);
    getServices().register(TeleportSoundService.class, VexTeleportSoundService.class);
    getServices().register(
        TeleportPresentationService.class,
        VexTeleportPresentationService.class
    );
    getServices().register(TeleportWarmupService.class, VexTeleportWarmupService.class);
    getServices().register(EssentialsTeleportService.class, VexEssentialsTeleportService.class);
    getServices().register(DirectTeleportService.class, VexDirectTeleportService.class);
    getServices().register(TeleportRequestService.class, VexTeleportRequestService.class);
    getServices().register(WarpService.class, VexWarpService.class);
    getServices().register(
        WarpLocalizationService.class,
        VexWarpLocalizationService.class
    );
    getServices().register(
        WarpLocalizationConfigurationService.class,
        VexWarpLocalizationConfigurationService.class
    );
    getServices().register(
        WarpPresentationService.class,
        VexWarpPresentationService.class
    );
    getServices().register(WarpCommandContext.class, VexWarpCommandContext.class);
    getServices().register(WarpCommandService.class, VexWarpCommandService.class);
    getServices().register(EssentialsReloadService.class, VexEssentialsReloadService.class);
  }

  @Override
  protected void registerData(final DataService data) {
    data.register(VexEssentialsPlayerData.class);
  }

  @Override
  protected void registerContainers(final PlayerContainerService containers) {
    containers.register(
        TeleportContainer.class,
        player -> new VexTeleportContainer(player, getLogger())
    );
  }

  @Override
  protected void registerGlobalData(final GlobalDataService data) {
    data.register(VexEssentialsGlobalData.class);
    getServices().require(WarpService.class).initialize();
  }

  @Override
  protected void registerMessages(final MessagingService messages) {
    messages.register(PlayerPositionRequestHandler.class);
    messages.register(PlayerPositionResponseHandler.class);
    messages.register(TeleportRequestOfferHandler.class);
    messages.register(TeleportRequestAdmissionHandler.class);
    messages.register(TeleportRequestDecisionHandler.class);
    messages.register(TeleportRequestExecutionHandler.class);
    messages.register(TeleportRequestCompletionHandler.class);
    messages.register(DirectTeleportExecutionHandler.class);
    messages.register(DirectTeleportCompletionHandler.class);
    messages.register(WarpRegistryChangedHandler.class);
  }

  @Override
  protected void registerCommands(final CommandService commands) {
    commands.register(VexEssentialsReloadCommand.class);
    commands.register(TeleportCommand.class);
    commands.register(TeleportHereCommand.class);
    commands.register(TeleportRequestCommand.class);
    commands.register(TeleportAskHereCommand.class);
    commands.register(TeleportAcceptCommand.class);
    commands.register(TeleportDenyCommand.class);
    commands.register(TeleportCancelCommand.class);
    commands.register(TeleportToggleCommand.class);
    commands.register(BackCommand.class);
    commands.register(WarpTeleportCommand.class);
    commands.register(WarpListCommand.class);
    commands.register(WarpCreateCommand.class);
    commands.register(WarpUpdateCommand.class);
    commands.register(WarpDeleteCommand.class);
  }

  @Override
  protected void registerListeners(final ListenerService listeners) {
    listeners.register(TeleportWarmupListener.class, getServices());
  }

  @Override
  protected void onVexEnable() {
    getServices().require(WarpService.class).synchronizePermissions();
    getLogger().info("Teleport services started successfully");
    getLogger().info("Warp services started successfully");
    getLogger().info("VexEssentials successfully enabled");
  }

  @Override
  protected String getConsolePrefix() {
    return "<dark_gray>[<gradient:#8A2BE2:#00BFFF>VexEssentials</gradient>]</dark_gray> ";
  }
}
