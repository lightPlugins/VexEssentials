package dev.vexsoft.essentials.paper.service.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VoidEndWorldConfigurationTest {

    @TempDir
    Path directory;

    @Test
    void disablesAutomaticDragonFightForANewVoidEnd() {
        Path dimension = directory.resolve("dimensions/minecraft/the_end");

        VoidEndWorldConfiguration.prepare(dimension);

        Path configFile = dimension.resolve("paper-world.yml");
        assertTrue(Files.isRegularFile(configFile));
        var configuration = YamlConfiguration.loadConfiguration(configFile.toFile());
        assertFalse(configuration.getBoolean("entities.spawning.scan-for-legacy-ender-dragon", true));
    }

    @Test
    void preservesExistingWorldOverridesAndDoesNotRewriteAlreadyConfiguredFiles() throws Exception {
        Path configFile = directory.resolve("paper-world.yml");
        Files.writeString(configFile, "_version: 31\nenvironment:\n  disable-ice-and-snow: true\n");

        VoidEndWorldConfiguration.prepare(directory);

        var configuration = YamlConfiguration.loadConfiguration(configFile.toFile());
        assertEquals(31, configuration.getInt("_version"));
        assertTrue(configuration.getBoolean("environment.disable-ice-and-snow"));
        assertFalse(configuration.getBoolean("entities.spawning.scan-for-legacy-ender-dragon", true));
        String prepared = Files.readString(configFile);
        VoidEndWorldConfiguration.prepare(directory);
        assertEquals(prepared, Files.readString(configFile));
    }

    @Test
    void refusesToOverwriteMalformedWorldConfiguration() throws Exception {
        Path configFile = directory.resolve("paper-world.yml");
        String malformed = "entities: [broken\n";
        Files.writeString(configFile, malformed);

        assertThrows(IllegalStateException.class, () -> VoidEndWorldConfiguration.prepare(directory));
        assertEquals(malformed, Files.readString(configFile));
    }
}
