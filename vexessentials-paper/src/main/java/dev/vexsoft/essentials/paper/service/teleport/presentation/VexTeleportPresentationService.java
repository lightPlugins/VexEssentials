package dev.vexsoft.essentials.paper.service.teleport.presentation;

import dev.vexsoft.core.api.localization.LanguageContainer;
import dev.vexsoft.core.api.localization.LocalizedMessage;
import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.localization.LocalizationService;
import dev.vexsoft.core.api.service.localization.LocalizedMessageService;
import dev.vexsoft.core.api.service.placeholder.PlaceholderService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.dialogs.ConfirmationDialogBuilder;
import dev.vexsoft.core.paper.dialogs.DialogResult;
import dev.vexsoft.core.paper.service.dialogs.DialogService;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.essentials.api.teleport.request.TeleportRequestType;
import dev.vexsoft.essentials.paper.service.teleport.sound.TeleportSoundService;
import dev.vexsoft.essentials.paper.teleport.presentation.RequestDialogChoice;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

/** Default localized teleport presentation with separately configurable hover lines. */
@Dependencies({
    LocalizationService.class,
    LocalizedMessageService.class,
    PlaceholderService.class,
    DialogService.class,
    ScheduleService.class,
    TeleportSoundService.class
})
public final class VexTeleportPresentationService implements TeleportPresentationService {

  private final LocalizationService localization;
  private final LocalizedMessageService messages;
  private final PlaceholderService placeholders;
  private final DialogService dialogs;
  private final ScheduleService scheduler;
  private final TeleportSoundService sounds;
  private final Logger logger;

  /** Creates the presentation service. */
  public VexTeleportPresentationService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    localization = checked.require(LocalizationService.class);
    messages = checked.require(LocalizedMessageService.class);
    placeholders = checked.require(PlaceholderService.class);
    dialogs = checked.require(DialogService.class);
    scheduler = checked.require(ScheduleService.class);
    sounds = checked.require(TeleportSoundService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public void send(
      final VexPlayer vexPlayer,
      final String key,
      final Map<String, String> replacements,
      final String soundEvent
  ) {
    Objects.requireNonNull(vexPlayer, "player");
    Optional<Player> player = vexPlayer.findPlatformPlayer(Player.class);
    if (player.isEmpty()) {
      return;
    }
    scheduler.runFor(player.get(), () -> {
      try {
        messages.send(vexPlayer, key, true, prepare(vexPlayer, replacements));
        if (soundEvent != null && !soundEvent.isBlank()) {
          sounds.play(vexPlayer, soundEvent);
        }
      } catch (RuntimeException exception) {
        reportPresentationFailure(key, exception);
      }
    });
  }

  @Override
  public void sendWithHover(
      final VexPlayer vexPlayer,
      final String messageKey,
      final String hoverKey,
      final Map<String, String> replacements,
      final String soundEvent
  ) {
    Objects.requireNonNull(vexPlayer, "player");
    Optional<Player> player = vexPlayer.findPlatformPlayer(Player.class);
    if (player.isEmpty()) {
      return;
    }
    scheduler.runFor(player.get(), () -> {
      try {
        Map<String, String> prepared = prepare(vexPlayer, replacements);
        Component prefix = component(vexPlayer, "general.prefix", Map.of());
        Component message = component(vexPlayer, messageKey, prepared).hoverEvent(
            HoverEvent.showText(component(vexPlayer, hoverKey, prepared))
        );
        player.get().sendMessage(prefix.append(message));
        if (soundEvent != null && !soundEvent.isBlank()) {
          sounds.play(vexPlayer, soundEvent);
        }
      } catch (RuntimeException exception) {
        reportPresentationFailure(messageKey, exception);
      }
    });
  }

  @Override
  public void sendInteractiveRequest(
      final VexPlayer vexPlayer,
      final UUID requestId,
      final TeleportRequestType type,
      final Map<String, String> replacements
  ) {
    Objects.requireNonNull(vexPlayer, "player");
    Optional<Player> player = vexPlayer.findPlatformPlayer(Player.class);
    if (player.isEmpty()) {
      return;
    }
    scheduler.runFor(player.get(), () -> {
      try {
        String root = type == TeleportRequestType.TO_TARGET
            ? "teleport.request.received.to-you"
            : "teleport.request.received.teleport-you";
        Map<String, String> prepared = prepare(vexPlayer, replacements);
        Component visible = component(vexPlayer, root + ".message", prepared);
        Component hover = component(vexPlayer, root + ".hover-text", prepared);
        Component prefix = component(vexPlayer, "general.prefix", Map.of());
        Component interactive = visible
            .clickEvent(ClickEvent.runCommand("/tpa accept " + requestId))
            .hoverEvent(HoverEvent.showText(hover));
        player.get().sendMessage(prefix.append(interactive));
        sounds.play(vexPlayer, "request-received");
      } catch (RuntimeException exception) {
        reportPresentationFailure("teleport.request.received", exception);
        messages.send(vexPlayer, "teleport.error.presentation", true, Map.of());
      }
    });
  }

  @Override
  public CompletableFuture<RequestDialogChoice> openRequestDialog(
      final VexPlayer vexPlayer,
      final String requesterName,
      final TeleportRequestType type,
      final Duration remaining
  ) {
    Objects.requireNonNull(vexPlayer, "player");
    CompletableFuture<RequestDialogChoice> result = new CompletableFuture<>();
    Optional<Player> player = vexPlayer.findPlatformPlayer(Player.class);
    if (player.isEmpty() || remaining.isZero() || remaining.isNegative()) {
      result.complete(RequestDialogChoice.UNAVAILABLE);
      return result;
    }
    scheduler.runFor(
        player.get(),
        () -> openDialog(vexPlayer, player.get(), requesterName, type, remaining, result),
        () -> result.complete(RequestDialogChoice.UNAVAILABLE)
    );
    return result;
  }

  private void openDialog(
      final VexPlayer vexPlayer,
      final Player player,
      final String requesterName,
      final TeleportRequestType type,
      final Duration remaining,
      final CompletableFuture<RequestDialogChoice> result
  ) {
    Map<String, String> replacements = Map.of(
        "player", requesterName,
        "remaining_seconds", Long.toString(Math.max(0, remaining.toSeconds()))
    );
    replacements = prepare(vexPlayer, replacements);
    try {
      String bodyKey = type == TeleportRequestType.TO_TARGET
          ? "teleport.request.dialog.teleport-to-you"
          : "teleport.request.dialog.teleport-you-there";
      ConfirmationDialogBuilder builder = dialogs.confirmation(player)
          .title(component(vexPlayer, "teleport.request.dialog.title", replacements))
          .confirmButton(component(
              vexPlayer,
              "teleport.request.dialog.confirm-button",
              replacements
          ))
          .confirmTooltip(component(
              vexPlayer,
              "teleport.request.dialog.confirm-tooltip",
              replacements
          ))
          .cancelButton(component(
              vexPlayer,
              "teleport.request.dialog.deny-button",
              replacements
          ))
          .cancelTooltip(component(
              vexPlayer,
              "teleport.request.dialog.deny-tooltip",
              replacements
          ))
          .canCloseWithEscape(true)
          .timeout(remaining);
      localized(vexPlayer, bodyKey, replacements).getComponents().forEach(builder::message);
      builder.open().whenComplete((dialogResult, throwable) -> {
        if (throwable != null) {
          reportPresentationFailure("teleport.request.dialog", throwable);
          result.complete(RequestDialogChoice.UNAVAILABLE);
          return;
        }
        result.complete(toChoice(dialogResult));
      });
    } catch (RuntimeException exception) {
      reportPresentationFailure("teleport.request.dialog", exception);
      result.complete(RequestDialogChoice.UNAVAILABLE);
    }
  }

  private RequestDialogChoice toChoice(final DialogResult<Boolean> result) {
    if (result.isConfirmed() && result.getValue().orElse(false)) {
      return RequestDialogChoice.ACCEPT;
    }
    return switch (result.getType()) {
      case CANCELLED -> RequestDialogChoice.DENY;
      case CLOSED, REPLACED, TIMED_OUT -> RequestDialogChoice.CLOSED;
      case PLAYER_LEFT, PLUGIN_DISABLED, UNAVAILABLE -> RequestDialogChoice.UNAVAILABLE;
      case CONFIRMED -> RequestDialogChoice.DENY;
    };
  }

  private Component component(
      final VexPlayer player,
      final String key,
      final Map<String, String> replacements
  ) {
    return join(localized(player, key, replacements));
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
        ? LocalizedMessage.list(resolved.getComponents().stream()
            .map(component -> placeholders.resolve(player, component))
            .toList())
        : LocalizedMessage.single(placeholders.resolve(player, resolved.getComponent()));
  }

  private Component join(final LocalizedMessage message) {
    Component result = Component.empty();
    boolean first = true;
    for (Component line : message.getComponents()) {
      if (!first) {
        result = result.append(Component.newline());
      }
      result = result.append(line);
      first = false;
    }
    return result;
  }

  private Map<String, String> prepare(
      final VexPlayer player,
      final Map<String, String> replacements
  ) {
    String rawSeconds = replacements.get("remaining_seconds");
    if (rawSeconds == null) {
      return replacements;
    }
    long seconds;
    try {
      seconds = Math.max(0, Long.parseLong(rawSeconds));
    } catch (NumberFormatException exception) {
      seconds = 0;
    }
    String durationKey = seconds == 1
        ? "teleport.duration.second"
        : "teleport.duration.seconds";
    Component duration = component(
        player,
        durationKey,
        Map.of("seconds", Long.toString(seconds))
    );
    Map<String, String> prepared = new HashMap<>(replacements);
    prepared.put(
        "remaining_time",
        PlainTextComponentSerializer.plainText().serialize(duration)
    );
    return Map.copyOf(prepared);
  }

  private void reportPresentationFailure(final String key, final Throwable throwable) {
    logger.log(
        Level.WARNING,
        "The localized teleport presentation '" + key + "' could not be shown. Check the "
            + "matching entry in 'languages/<language>/teleport.yml'.",
        throwable
    );
  }
}
