package dev.vexsoft.essentials.paper.service.warp.presentation;

import dev.vexsoft.core.api.localization.LanguageContainer;
import dev.vexsoft.core.api.localization.LocalizedMessage;
import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.localization.LocalizationService;
import dev.vexsoft.core.api.service.localization.LocalizedMessageService;
import dev.vexsoft.core.api.service.placeholder.PlaceholderService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.dialogs.DialogResult;
import dev.vexsoft.core.paper.service.dialogs.DialogService;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.essentials.api.service.warp.WarpLocalizationService;
import dev.vexsoft.essentials.api.warp.Warp;
import dev.vexsoft.essentials.paper.service.teleport.sound.TeleportSoundService;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

/** Default warp presentation following VexEssentials prefix and multi-line rules. */
@Dependencies({
    LocalizationService.class,
    LocalizedMessageService.class,
    PlaceholderService.class,
    WarpLocalizationService.class,
    DialogService.class,
    ScheduleService.class,
    TeleportSoundService.class
})
public final class VexWarpPresentationService implements WarpPresentationService {

  private final LocalizationService localization;
  private final LocalizedMessageService messages;
  private final PlaceholderService placeholders;
  private final WarpLocalizationService warpLocalization;
  private final DialogService dialogs;
  private final ScheduleService scheduler;
  private final TeleportSoundService sounds;
  private final Logger logger;

  /** Creates the localized presentation coordinator. */
  public VexWarpPresentationService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    localization = checked.require(LocalizationService.class);
    messages = checked.require(LocalizedMessageService.class);
    placeholders = checked.require(PlaceholderService.class);
    warpLocalization = checked.require(WarpLocalizationService.class);
    dialogs = checked.require(DialogService.class);
    scheduler = checked.require(ScheduleService.class);
    sounds = checked.require(TeleportSoundService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public void send(
      final VexPlayer player,
      final String key,
      final Map<String, String> replacements,
      final String soundEvent
  ) {
    Objects.requireNonNull(player, "player");
    if (player.findPlatformPlayer(Player.class).isEmpty()
        || player.findContainer(LanguageContainer.class).isEmpty()) {
      return;
    }
    try {
      boolean withPrefix = !localization.resolve(
          player.getContainer(LanguageContainer.class).getLanguage().getKey(),
          key,
          replacements
      ).isList();
      messages.send(player, key, withPrefix, replacements);
      if (soundEvent != null && !soundEvent.isBlank()) {
        sounds.play(player, soundEvent);
      }
    } catch (RuntimeException exception) {
      reportFailure(key, exception);
    }
  }

  @Override
  public void sendList(final VexPlayer vexPlayer, final Collection<Warp> warps) {
    Objects.requireNonNull(vexPlayer, "player");
    Collection<Warp> checkedWarps = List.copyOf(Objects.requireNonNull(warps, "warps"));
    Optional<Player> platformPlayer = vexPlayer.findPlatformPlayer(Player.class);
    if (platformPlayer.isEmpty()) {
      return;
    }
    scheduler.runFor(platformPlayer.get(), () -> {
      try {
        Component output = component(vexPlayer, "warp.list.header", Map.of());
        if (checkedWarps.isEmpty()) {
          output = output.append(Component.newline())
              .append(component(vexPlayer, "warp.list.empty", Map.of()));
        } else {
          for (Warp warp : checkedWarps) {
            Component name = warpLocalization.getName(vexPlayer, warp);
            List<Component> description = warpLocalization.getDescription(vexPlayer, warp);
            if (!description.isEmpty()) {
              name = name.hoverEvent(HoverEvent.showText(join(description)));
            }
            output = output.append(Component.newline())
                .append(component(vexPlayer, "warp.list.entry-prefix", Map.of()))
                .append(name);
          }
        }
        platformPlayer.get().sendMessage(output);
      } catch (RuntimeException exception) {
        reportFailure("warp.list", exception);
      }
    });
  }

  @Override
  public CompletableFuture<Boolean> confirmDelete(final VexPlayer vexPlayer, final Warp warp) {
    Objects.requireNonNull(vexPlayer, "player");
    Warp checkedWarp = Objects.requireNonNull(warp, "warp");
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    Optional<Player> platformPlayer = vexPlayer.findPlatformPlayer(Player.class);
    if (platformPlayer.isEmpty()) {
      result.complete(false);
      return result;
    }
    Player player = platformPlayer.get();
    scheduler.runFor(player, () -> {
      try {
        Map<String, String> replacements = Map.of("warp", checkedWarp.id());
        var builder = dialogs.confirmation(player)
            .title(component(vexPlayer, "warp.delete-dialog.title", replacements))
            .confirmButton(component(
                vexPlayer,
                "warp.delete-dialog.confirm-button",
                replacements
            ))
            .confirmTooltip(component(
                vexPlayer,
                "warp.delete-dialog.confirm-tooltip",
                replacements
            ))
            .cancelButton(component(
                vexPlayer,
                "warp.delete-dialog.cancel-button",
                replacements
            ))
            .cancelTooltip(component(
                vexPlayer,
                "warp.delete-dialog.cancel-tooltip",
                replacements
            ))
            .canCloseWithEscape(true)
            .timeout(Duration.ofSeconds(30));
        localized(vexPlayer, "warp.delete-dialog.body", replacements)
            .getLines()
            .forEach(builder::message);
        builder.open().whenComplete((dialogResult, throwable) -> {
          if (throwable != null) {
            reportFailure("warp.delete-dialog", throwable);
            result.complete(false);
            return;
          }
          result.complete(isConfirmed(dialogResult));
        });
      } catch (RuntimeException exception) {
        reportFailure("warp.delete-dialog", exception);
        result.complete(false);
      }
    }, () -> result.complete(false));
    return result;
  }

  private boolean isConfirmed(final DialogResult<Boolean> result) {
    return result.isConfirmed() && result.getValue().orElse(false);
  }

  private Component component(
      final VexPlayer player,
      final String key,
      final Map<String, String> replacements
  ) {
    return join(localized(player, key, replacements).getLines());
  }

  private LocalizedMessage localized(
      final VexPlayer player,
      final String key,
      final Map<String, String> replacements
  ) {
    LocalizedMessage resolved = localization.resolve(
        player.getContainer(LanguageContainer.class).getLanguage().getKey(),
        key,
        replacements
    );
    return resolved.isList()
        ? LocalizedMessage.list(resolved.getLines().stream()
            .map(line -> placeholders.resolve(player, line))
            .toList())
        : LocalizedMessage.single(placeholders.resolve(player, resolved.getComponent()));
  }

  private Component join(final Collection<Component> lines) {
    Component result = Component.empty();
    boolean first = true;
    for (Component line : lines) {
      if (!first) {
        result = result.append(Component.newline());
      }
      result = result.append(line);
      first = false;
    }
    return result;
  }

  private void reportFailure(final String key, final Throwable throwable) {
    logger.log(
        Level.WARNING,
        "The localized warp presentation '" + key + "' could not be shown. Check the matching "
            + "entry in 'languages/<language>/warps.yml'.",
        throwable
    );
  }
}
