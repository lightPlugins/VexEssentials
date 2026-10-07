package dev.vexsoft.essentials.paper.service.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vexsoft.core.api.world.WorldKey;
import dev.vexsoft.essentials.api.world.WorldGeneratorType;
import dev.vexsoft.essentials.paper.world.generator.VoidChunkGenerator;
import java.util.OptionalLong;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class WorldCreationProfilesTest {

    @ParameterizedTest
    @EnumSource(value = WorldGeneratorType.class, names = {"END", "END_VOID"})
    void preservesTheVanillaEndIdForClientShaderDetection(final WorldGeneratorType profile) {
        var creator = VexManagedWorldService.createWorldCreator(
            new WorldKey("minecraft", "the_end"),
            profile,
            OptionalLong.empty()
        );

        assertEquals(NamespacedKey.minecraft("the_end"), creator.key());
        assertEquals(World.Environment.THE_END, creator.environment());
    }

    @ParameterizedTest
    @EnumSource(value = WorldGeneratorType.class, names = {"NETHER", "NETHER_VOID"})
    void preservesTheVanillaNetherIdForClientShaderDetection(final WorldGeneratorType profile) {
        var creator = VexManagedWorldService.createWorldCreator(
            new WorldKey("minecraft", "the_nether"),
            profile,
            OptionalLong.empty()
        );

        assertEquals(NamespacedKey.minecraft("the_nether"), creator.key());
        assertEquals(World.Environment.NETHER, creator.environment());
    }

    @ParameterizedTest
    @CsvSource({
        "NORMAL, NORMAL, NORMAL, false",
        "FLAT, NORMAL, FLAT, false",
        "VOID, NORMAL, NORMAL, true",
        "NETHER, NETHER, NORMAL, false",
        "NETHER_VOID, NETHER, NORMAL, true",
        "END, THE_END, NORMAL, false",
        "END_VOID, THE_END, NORMAL, true"
    })
    void configuresDimensionAndTerrainWithoutChangingIdentityOrSeed(
        final WorldGeneratorType profile,
        final World.Environment environment,
        final WorldType terrain,
        final boolean empty
    ) {
        var creator = VexManagedWorldService.createWorldCreator(
            new WorldKey("vexessentials", "test_world"),
            profile,
            OptionalLong.of(12345L)
        );

        assertEquals(new NamespacedKey("vexessentials", "test_world"), creator.key());
        assertEquals(12345L, creator.seed());
        assertEquals(environment, creator.environment());
        assertEquals(terrain, creator.type());

        if (!empty) {
            assertNull(creator.generator());
            assertNull(creator.forcedSpawnPosition());
            assertTrue(creator.generateStructures());
            return;
        }

        var generator = assertInstanceOf(VoidChunkGenerator.class, creator.generator());
        assertFalse(creator.generateStructures());
        assertFalse(generator.shouldGenerateNoise());
        assertFalse(generator.shouldGenerateSurface());
        assertFalse(generator.shouldGenerateCaves());
        assertFalse(generator.shouldGenerateDecorations());
        assertFalse(generator.shouldGenerateStructures());
        assertFalse(generator.shouldGenerateMobs());
        assertEquals(0, creator.forcedSpawnPosition().blockX());
        assertEquals(64, creator.forcedSpawnPosition().blockY());
        assertEquals(0, creator.forcedSpawnPosition().blockZ());
    }
}
