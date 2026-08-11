package dev.vexsoft.essentials.paper.service.warp.localization;

import dev.vexsoft.core.api.localization.LanguageContainer;
import dev.vexsoft.core.api.localization.LanguageKey;
import dev.vexsoft.core.api.localization.LocalizedMessage;
import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.localization.LocalizationService;
import dev.vexsoft.core.api.service.placeholder.PlaceholderService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.api.service.warp.WarpLocalizationService;
import dev.vexsoft.essentials.api.warp.Warp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** VexCore localization-backed warp name and description resolver. */
@Dependencies({LocalizationService.class, PlaceholderService.class})
public final class VexWarpLocalizationService implements WarpLocalizationService {

  private final LocalizationService localization;
  private final PlaceholderService placeholders;
  private final Logger logger;
  private final Set<String> reportedMissingValues = ConcurrentHashMap.newKeySet();

  /** Creates the owner-scoped resolver. */
  public VexWarpLocalizationService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    localization = checked.require(LocalizationService.class);
    placeholders = checked.require(PlaceholderService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public Component getName(final VexPlayer player, final Warp warp) {
    VexPlayer checkedPlayer = Objects.requireNonNull(player, "player");
    Warp checkedWarp = Objects.requireNonNull(warp, "warp");
    LanguageKey language = language(checkedPlayer);
    LocalizedMessage resolved = localization.resolve(language, checkedWarp.nameKey(), Map.of());
    Component name = resolved.getLines().getFirst();
    if (isMissing(name)) {
      reportMissing(checkedWarp, language, "name");
      return Component.text(checkedWarp.id());
    }
    return placeholders.resolve(checkedPlayer, name);
  }

  @Override
  public List<Component> getDescription(final VexPlayer player, final Warp warp) {
    VexPlayer checkedPlayer = Objects.requireNonNull(player, "player");
    Warp checkedWarp = Objects.requireNonNull(warp, "warp");
    LanguageKey language = language(checkedPlayer);
    LocalizedMessage resolved = localization.resolve(
        language,
        checkedWarp.descriptionKey(),
        Map.of()
    );
    if (isMissing(resolved.getLines().getFirst())) {
      reportMissing(checkedWarp, language, "description");
      return List.of();
    }
    return resolved.getLines().stream()
        .map(line -> placeholders.resolve(checkedPlayer, line))
        .toList();
  }

  private LanguageKey language(final VexPlayer player) {
    return player.getContainer(LanguageContainer.class).getLanguage().getKey();
  }

  private boolean isMissing(final Component component) {
    return PlainTextComponentSerializer.plainText().serialize(component)
        .startsWith("Missing localization:");
  }

  private void reportMissing(
      final Warp warp,
      final LanguageKey language,
      final String value
  ) {
    String reportKey = language + ":" + warp.id() + ":" + value;
    if (reportedMissingValues.add(reportKey)) {
      logger.warning(
          "Warp '" + warp.id() + "' has no localized " + value + " for language '"
              + language + "'. Add '" + (value.equals("name")
              ? warp.nameKey()
              : warp.descriptionKey()) + "' to 'languages/" + language
              + "/warps.yml'."
      );
    }
  }
}
