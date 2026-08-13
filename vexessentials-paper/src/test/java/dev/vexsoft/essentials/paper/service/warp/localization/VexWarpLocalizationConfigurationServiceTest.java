package dev.vexsoft.essentials.paper.service.warp.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.vexsoft.core.api.configuration.ConfigurationOwner;
import dev.vexsoft.core.api.configuration.ConfigurationSection;
import dev.vexsoft.core.api.configuration.VexConfiguration;
import dev.vexsoft.core.api.localization.LanguageKey;
import dev.vexsoft.core.api.localization.LocalizedMessage;
import dev.vexsoft.core.api.service.configuration.ConfigurationService;
import dev.vexsoft.core.api.service.localization.LocalizationService;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class VexWarpLocalizationConfigurationServiceTest {

  @Test
  void createsPresentationDefaultsForEveryDiscoveredLanguageAndReloads() {
    TestConfigurationService configurations = new TestConfigurationService();
    configurations.file("en_EN/warp.yml").set(
        "default-warp-description",
        List.of("<gray>No description.", "<dark_gray>Configure this warp in warps.yml.")
    );
    configurations.file("de_DE/warp.yml").set(
        "default-warp-description",
        List.of("<gray>Keine Beschreibung.")
    );
    AtomicInteger reloads = new AtomicInteger();
    LocalizationService localization = new TestLocalizationService(reloads);
    VexWarpLocalizationConfigurationService service =
        new VexWarpLocalizationConfigurationService(configurations, localization);

    service.createDefaults("spawn", "<tailwind:red:4>Super Spawn");

    assertEquals(
        "<tailwind:red:4>Super Spawn",
        configurations.file("en_EN/warps.yml").getString("warps.spawn.name")
    );
    assertEquals(
        List.of("<gray>No description.", "<dark_gray>Configure this warp in warps.yml."),
        configurations.file("en_EN/warps.yml").getStringList("warps.spawn.description")
    );
    assertEquals(
        List.of("<gray>Keine Beschreibung."),
        configurations.file("de_DE/warps.yml").getStringList("warps.spawn.description")
    );
    assertEquals(1, reloads.get());
  }

  private record TestLocalizationService(AtomicInteger reloads) implements LocalizationService {

    @Override
    public LocalizedMessage resolve(
        final LanguageKey language,
        final String key,
        final Map<String, String> replacements
    ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void reload() {
      reloads.incrementAndGet();
    }
  }

  private static final class TestConfigurationService implements ConfigurationService {

    private final Map<Path, TestConfiguration> files = new LinkedHashMap<>();

    TestConfiguration file(final String relativePath) {
      return files.computeIfAbsent(Path.of(relativePath), TestConfiguration::new);
    }

    @Override
    public ConfigurationOwner getOwner() {
      return new TestOwner();
    }

    @Override
    public VexConfiguration load(final String relativePath) {
      return loadFile(Path.of(relativePath));
    }

    @Override
    public VexConfiguration load(final Path relativePath) {
      return loadFile(relativePath);
    }

    @Override
    public VexConfiguration load(final Path relativePath, final boolean loadDefaults) {
      return loadFile(relativePath);
    }

    @Override
    public VexConfiguration load(final Path relativePath, final String defaultsResource) {
      return loadFile(relativePath);
    }

    @Override
    public Map<Path, VexConfiguration> loadDirectory(final Path relativeDirectory) {
      Map<Path, VexConfiguration> loaded = new LinkedHashMap<>();
      files.forEach((path, configuration) -> loaded.put(path, configuration));
      return Map.copyOf(loaded);
    }

    private TestConfiguration loadFile(final Path path) {
      Path relative = path.getNameCount() > 1 && path.getName(0).toString().equals("languages")
          ? path.subpath(1, path.getNameCount())
          : path;
      return file(relative.toString());
    }
  }

  private static final class TestConfiguration implements VexConfiguration {

    private final Path file;
    private final Map<String, Object> values = new LinkedHashMap<>();

    private TestConfiguration(final Path file) {
      this.file = file;
    }

    @Override
    public Path getFile() {
      return file;
    }

    @Override
    public void reload() {
    }

    @Override
    public void save() {
    }

    @Override
    public boolean contains(final String path) {
      return values.containsKey(path);
    }

    @Override
    public Object get(final String path) {
      return values.get(path);
    }

    @Override
    public String getString(final String path) {
      return getString(path, "");
    }

    @Override
    public String getString(final String path, final String defaultValue) {
      return values.get(path) instanceof String value ? value : defaultValue;
    }

    @Override
    public int getInt(final String path, final int defaultValue) {
      return defaultValue;
    }

    @Override
    public long getLong(final String path, final long defaultValue) {
      return defaultValue;
    }

    @Override
    public double getDouble(final String path, final double defaultValue) {
      return defaultValue;
    }

    @Override
    public boolean getBoolean(final String path, final boolean defaultValue) {
      return defaultValue;
    }

    @Override
    public List<String> getStringList(final String path) {
      Object value = values.get(path);
      return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of();
    }

    @Override
    public ConfigurationSection getSection(final String path) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getKeys(final boolean deep) {
      return Set.copyOf(values.keySet());
    }

    @Override
    public Map<String, Object> getValues(final boolean deep) {
      return Map.copyOf(values);
    }

    @Override
    public void set(final String path, final Object value) {
      values.put(path, value);
    }
  }

  private static final class TestOwner implements ConfigurationOwner {

    @Override
    public Path getConfigurationDirectory() {
      return Path.of(".");
    }

    @Override
    public Optional<InputStream> getConfigurationResource(final String resourcePath) {
      return Optional.empty();
    }

    @Override
    public void reportConfigurationWarning(final String message, final Throwable cause) {
    }

    @Override
    public String getServiceOwnerName() {
      return "Test";
    }
  }
}
