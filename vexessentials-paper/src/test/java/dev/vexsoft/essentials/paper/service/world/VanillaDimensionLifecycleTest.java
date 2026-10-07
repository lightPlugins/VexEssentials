package dev.vexsoft.essentials.paper.service.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vexsoft.core.api.configuration.VexConfiguration;
import dev.vexsoft.core.api.service.configuration.ConfigurationService;
import dev.vexsoft.core.api.world.WorldKey;
import dev.vexsoft.core.paper.service.network.ServerIdentityService;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.core.paper.service.world.WorldService;
import dev.vexsoft.essentials.api.world.ManagedWorldState;
import dev.vexsoft.essentials.api.world.WorldGeneratorType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.logging.Logger;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VanillaDimensionLifecycleTest {

    private static final WorldKey NETHER = new WorldKey("minecraft", "the_nether");

    @TempDir
    Path levelDirectory;

    @ParameterizedTest
    @CsvSource({"the_nether, NETHER", "the_nether, NETHER_VOID", "the_end, END", "the_end, END_VOID"})
    void schedulesCreationOfAnUnusedCanonicalDimension(final String name, final WorldGeneratorType profile) {
        List<Runnable> tasks = new ArrayList<>();
        var service = service(List.of(), Optional.empty(), tasks);

        var creation = service.create(new WorldKey("minecraft", name), profile, OptionalLong.empty());

        assertFalse(creation.isDone());
        assertEquals(1, tasks.size());
    }

    @ParameterizedTest
    @CsvSource({
        "the_nether, NORMAL, nether-generator",
        "the_nether, FLAT, nether-generator",
        "the_nether, VOID, nether-generator",
        "the_nether, END, nether-generator",
        "the_nether, END_VOID, nether-generator",
        "the_end, NORMAL, end-generator",
        "the_end, FLAT, end-generator",
        "the_end, VOID, end-generator",
        "the_end, NETHER, end-generator",
        "the_end, NETHER_VOID, end-generator"
    })
    void rejectsMismatchedProfilesForCanonicalDimensions(
        final String name,
        final WorldGeneratorType profile,
        final String expectedReason
    ) {
        List<Runnable> tasks = new ArrayList<>();
        var service = service(List.of(), Optional.empty(), tasks);

        var result = service.create(new WorldKey("minecraft", name), profile, OptionalLong.empty()).join();

        assertFalse(result.successful());
        assertEquals(expectedReason, result.reason());
        assertTrue(tasks.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"the_nether, NETHER_VOID", "the_end, END_VOID"})
    void preservesExistingDimensionDataInsteadOfCreatingOverIt(
        final String name,
        final WorldGeneratorType profile
    ) throws Exception {
        Path dimension = levelDirectory.resolve("dimensions/minecraft").resolve(name);
        Files.createDirectories(dimension);
        Path existingData = dimension.resolve("existing-data.txt");
        Files.writeString(existingData, "keep existing nether");
        List<Runnable> tasks = new ArrayList<>();
        var service = service(List.of(), Optional.empty(), tasks);

        var result = service.create(new WorldKey("minecraft", name), profile, OptionalLong.empty()).join();

        assertEquals("folder-exists", result.reason());
        assertEquals("keep existing nether", Files.readString(existingData));
        assertTrue(tasks.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"the_nether, NETHER_VOID", "the_end, END_VOID"})
    void refusesToReplaceAnAlreadyLoadedDimension(final String name, final WorldGeneratorType profile) {
        World loaded = proxy(World.class, (instance, method, arguments) -> {
            throw new UnsupportedOperationException(method.getName());
        });
        List<Runnable> tasks = new ArrayList<>();
        var service = service(List.of(), Optional.of(loaded), tasks);

        var result = service.create(new WorldKey("minecraft", name), profile, OptionalLong.empty()).join();

        assertEquals("already-exists", result.reason());
        assertTrue(tasks.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"the_nether, nether_void, NETHER_VOID", "the_end, end_void, END_VOID"})
    void restoresCanonicalVoidDimensionsAndSchedulesStartupLoading(
        final String name,
        final String savedProfile,
        final WorldGeneratorType profile
    ) throws Exception {
        var key = new WorldKey("minecraft", name);
        Files.createDirectories(levelDirectory.resolve("dimensions/minecraft").resolve(name));
        List<Runnable> tasks = new ArrayList<>();
        var service = service(List.of(Map.of(
            "key", key.asString(),
            "generator", savedProfile,
            "auto-load", true,
            "seed", 12345L
        )), Optional.empty(), tasks);

        service.initialize();

        var managed = service.find(key).orElseThrow();
        assertEquals(profile, managed.generator());
        assertTrue(managed.autoLoad());
        assertEquals(ManagedWorldState.LOADING, managed.state());
        assertEquals(1, tasks.size());
        assertEquals("protected", service.delete(key).join().reason());
        assertEquals("not-loaded", service.unload(key).join().reason());
    }

    @Test
    void keepsTheOverworldAndUnmanagedVanillaDimensionsProtected() {
        List<Runnable> tasks = new ArrayList<>();
        var service = service(List.of(), Optional.empty(), tasks);

        assertEquals("protected", service.create(new WorldKey("minecraft", "overworld"),
            WorldGeneratorType.END_VOID, OptionalLong.empty()).join().reason());
        assertEquals("protected", service.unload(NETHER).join().reason());
        assertEquals("protected", service.unload(new WorldKey("minecraft", "the_end")).join().reason());
        assertTrue(tasks.isEmpty());
    }

    private VexManagedWorldService service(
        final List<Map<String, Object>> definitions,
        final Optional<World> loadedWorld,
        final List<Runnable> tasks
    ) {
        var configuration = proxy(VexConfiguration.class, (instance, method, arguments) -> switch (method.getName()) {
            case "get" -> definitions;
            case "contains" -> false;
            case "getBoolean" -> arguments[1];
            default -> throw new UnsupportedOperationException(method.getName());
        });
        var configurations = proxy(ConfigurationService.class, (instance, method, arguments) -> {
            if (method.getName().equals("load")) {
                return configuration;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        var schedules = proxy(ScheduleService.class, (instance, method, arguments) -> {
            if (method.getName().equals("runGlobal")) {
                tasks.add((Runnable) arguments[0]);
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        var worlds = proxy(WorldService.class, (instance, method, arguments) -> {
            if (method.getName().equals("find")) {
                return loadedWorld;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        var identity = proxy(ServerIdentityService.class, (instance, method, arguments) -> {
            throw new UnsupportedOperationException(method.getName());
        });
        return new VexManagedWorldService(
            configurations,
            schedules,
            worlds,
            identity,
            levelDirectory,
            Logger.getLogger(VanillaDimensionLifecycleTest.class.getName())
        );
    }

    private static <T> T proxy(final Class<T> type, final InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
