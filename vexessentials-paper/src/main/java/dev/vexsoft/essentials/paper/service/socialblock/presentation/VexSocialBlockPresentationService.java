package dev.vexsoft.essentials.paper.service.socialblock.presentation;

import dev.vexsoft.core.api.localization.LanguageContainer;
import dev.vexsoft.core.api.localization.LocalizedMessage;
import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.player.identity.PlayerIdentity;
import dev.vexsoft.core.api.service.localization.LocalizationService;
import dev.vexsoft.core.api.service.localization.LocalizedMessageService;
import dev.vexsoft.core.api.service.placeholder.PlaceholderService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.essentials.api.socialblock.SocialBlockChangeStatus;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Default localized block presentation. */
@Dependencies({
    LocalizationService.class,
    LocalizedMessageService.class,
    PlaceholderService.class,
    ScheduleService.class
})
public final class VexSocialBlockPresentationService implements SocialBlockPresentationService {

  private final LocalizationService localization;
  private final LocalizedMessageService messages;
  private final PlaceholderService placeholders;
  private final ScheduleService scheduler;

  public VexSocialBlockPresentationService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    localization = checked.require(LocalizationService.class);
    messages = checked.require(LocalizedMessageService.class);
    placeholders = checked.require(PlaceholderService.class);
    scheduler = checked.require(ScheduleService.class);
  }

  @Override
  public void sendChange(
      final VexPlayer player,
      final String targetName,
      final SocialBlockChangeStatus status
  ) {
    String key = switch (status) {
      case BLOCKED -> "social-block.blocked";
      case UNBLOCKED -> "social-block.unblocked";
      case ALREADY_BLOCKED -> "social-block.already-blocked";
      case NOT_BLOCKED -> "social-block.not-blocked";
      case SELF -> "social-block.self";
      case PLAYER_NOT_FOUND -> "social-block.player-not-found";
      case FAILED -> "social-block.failed";
    };
    send(player, key, Map.of("player", targetName));
  }

  @Override
  public void sendList(final VexPlayer player, final List<PlayerIdentity> blockedPlayers) {
    List<PlayerIdentity> checkedPlayers = List.copyOf(blockedPlayers);
    player.findPlatformPlayer(Player.class).ifPresent(platformPlayer -> scheduler.runFor(
        platformPlayer,
        () -> {
          Component entries = join(checkedPlayers.stream()
              .map(identity -> component(
                  player,
                  "social-block.list-entry",
                  Map.of("player", identity.name())
              ))
              .toList());
          Component output = component(
              player,
              "social-block.list",
              Map.of("amount", Integer.toString(checkedPlayers.size()))
          ).replaceText(builder -> builder
              .matchLiteral("%blocked_players%")
              .replacement(entries));
          platformPlayer.sendMessage(output);
        }
    ));
  }

  private void send(
      final VexPlayer player,
      final String key,
      final Map<String, String> replacements
  ) {
    player.findPlatformPlayer(Player.class).ifPresent(platformPlayer -> scheduler.runFor(
        platformPlayer,
        () -> messages.send(player, key, true, replacements)
    ));
  }

  private Component component(
      final VexPlayer player,
      final String key,
      final Map<String, String> replacements
  ) {
    LocalizedMessage resolved = localization.resolve(
        player.getContainer(LanguageContainer.class).getLanguage().getKey(),
        key,
        replacements
    );
    return join(resolved.getLines().stream()
        .map(line -> placeholders.resolve(player, line))
        .toList());
  }

  private Component join(final List<Component> lines) {
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
}
