package dev.vexsoft.essentials.paper.service.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Prepares Paper's per-world override before an empty end dimension is loaded. */
final class VoidEndWorldConfiguration {

    private static final String DRAGON_SCAN = "entities.spawning.scan-for-legacy-ender-dragon";

    private VoidEndWorldConfiguration() {
    }

    static void prepare(final Path dimensionDirectory) {
        Path configFile = dimensionDirectory.resolve("paper-world.yml");
        var configuration = new YamlConfiguration();
        try {
            if (Files.exists(configFile)) {
                configuration.load(configFile.toFile());
            }
            if (configuration.isBoolean(DRAGON_SCAN) && !configuration.getBoolean(DRAGON_SCAN)) {
                return;
            }
            configuration.set(DRAGON_SCAN, false);
            Files.createDirectories(dimensionDirectory);
            configuration.save(configFile.toFile());
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Unable to disable the dragon fight for " + dimensionDirectory, exception);
        }
    }
}
