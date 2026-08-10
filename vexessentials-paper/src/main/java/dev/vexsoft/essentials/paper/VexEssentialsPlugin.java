package dev.vexsoft.essentials.paper;

import dev.vexsoft.core.api.service.messaging.MessagingService;
import dev.vexsoft.core.api.service.player.DataService;
import dev.vexsoft.core.api.service.player.PlayerContainerService;
import dev.vexsoft.core.paper.plugin.VexPlugin;
import dev.vexsoft.core.paper.service.commands.CommandService;
import dev.vexsoft.core.paper.service.listeners.ListenerService;
import dev.vexsoft.essentials.api.service.teleport.EssentialsTeleportService;
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
import dev.vexsoft.essentials.paper.teleport.command.BackCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportRequestCommand;
import dev.vexsoft.essentials.paper.teleport.command.TeleportHereCommand;
import dev.vexsoft.essentials.paper.teleport.data.VexEssentialsPlayerData;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.position.PlayerPositionRequestHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.position.PlayerPositionResponseHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.direct.DirectTeleportCompletionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.direct.DirectTeleportExecutionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.request.TeleportRequestCompletionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.request.TeleportRequestDecisionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.request.TeleportRequestExecutionHandler;
import dev.vexsoft.essentials.paper.teleport.messaging.handler.request.TeleportRequestOfferHandler;
import dev.vexsoft.essentials.paper.teleport.listener.TeleportWarmupListener;

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
  protected void registerMessages(final MessagingService messages) {
    messages.register(PlayerPositionRequestHandler.class);
    messages.register(PlayerPositionResponseHandler.class);
    messages.register(TeleportRequestOfferHandler.class);
    messages.register(TeleportRequestDecisionHandler.class);
    messages.register(TeleportRequestExecutionHandler.class);
    messages.register(TeleportRequestCompletionHandler.class);
    messages.register(DirectTeleportExecutionHandler.class);
    messages.register(DirectTeleportCompletionHandler.class);
  }

  @Override
  protected void registerCommands(final CommandService commands) {
    commands.register(VexEssentialsReloadCommand.class);
    commands.register(TeleportCommand.class);
    commands.register(TeleportHereCommand.class);
    commands.register(TeleportRequestCommand.class);
    commands.register(BackCommand.class);
  }

  @Override
  protected void registerListeners(final ListenerService listeners) {
    listeners.register(TeleportWarmupListener.class, getServices());
  }

  @Override
  protected void onVexEnable() {
    getLogger().info("Teleport services started successfully");
    getLogger().info("VexEssentials successfully enabled");
  }

  @Override
  protected String getConsolePrefix() {
    return "<dark_gray>[<gradient:#8A2BE2:#00BFFF>VexEssentials</gradient>]</dark_gray> ";
  }
}
