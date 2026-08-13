package dev.vexsoft.essentials.paper.service.warp.localization;

import dev.vexsoft.core.api.configuration.VexConfiguration;
import dev.vexsoft.core.api.localization.LanguageKey;
import dev.vexsoft.core.api.service.configuration.ConfigurationService;
import dev.vexsoft.core.api.service.localization.LocalizationService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** YAML-backed writer for dynamic warp localization entries. */
@Dependencies({ConfigurationService.class, LocalizationService.class})
public final class VexWarpLocalizationConfigurationService implements
    WarpLocalizationConfigurationService {

  private static final Path LANGUAGES_DIRECTORY = Path.of("languages");
  private static final String DEFAULT_DESCRIPTION =
      "<tailwind:zinc:7>No description has been set for this warp yet.";

  private final ConfigurationService configurations;
  private final LocalizationService localization;

  public VexWarpLocalizationConfigurationService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    configurations = checked.require(ConfigurationService.class);
    localization = checked.require(LocalizationService.class);
  }

  VexWarpLocalizationConfigurationService(
      final ConfigurationService configurations,
      final LocalizationService localization
  ) {
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.localization = Objects.requireNonNull(localization, "localization");
  }

  @Override
  public synchronized void createDefaults(final String warpId, final String displayName) {
    String checkedWarpId = Objects.requireNonNull(warpId, "warpId");
    String checkedDisplayName = Objects.requireNonNull(displayName, "displayName").trim();
    if (checkedDisplayName.isEmpty()) {
      throw new IllegalArgumentException("Warp display name must not be empty");
    }

    Map<Path, VexConfiguration> languageFiles = configurations.loadDirectory(
        LANGUAGES_DIRECTORY
    );
    for (LanguageKey language : languages(languageFiles.keySet())) {
      Path languageDirectory = LANGUAGES_DIRECTORY.resolve(language.getValue());
      VexConfiguration warps = configurations.load(
          languageDirectory.resolve("warps.yml"),
          false
      );
      VexConfiguration messages = configurations.load(
          languageDirectory.resolve("warp.yml"),
          false
      );
      String root = "warps." + checkedWarpId;
      warps.set(root + ".name", checkedDisplayName);
      if (!warps.contains(root + ".description")) {
        warps.set(
            root + ".description",
            new ArrayList<>(description(messages))
        );
      }
      warps.save();
    }
    localization.reload();
  }

  private Collection<LanguageKey> languages(final Collection<Path> files) {
    Set<LanguageKey> languages = new LinkedHashSet<>();
    for (Path file : files) {
      if (file.getNameCount() < 2) {
        continue;
      }
      try {
        languages.add(LanguageKey.of(file.getName(0).toString()));
      } catch (IllegalArgumentException ignored) {
        // Configuration loading reports malformed language directories separately.
      }
    }
    languages.add(LanguageKey.EN_EN);
    return languages;
  }

  private Collection<String> description(final VexConfiguration messages) {
    List<String> configured = messages.getStringList("default-warp-description").stream()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .toList();
    return configured.isEmpty() ? List.of(DEFAULT_DESCRIPTION) : configured;
  }
}
