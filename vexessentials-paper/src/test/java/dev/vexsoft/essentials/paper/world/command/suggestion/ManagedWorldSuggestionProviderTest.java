package dev.vexsoft.essentials.paper.world.command.suggestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.api.world.WorldKey;
import dev.vexsoft.essentials.api.service.world.ManagedWorldService;
import dev.vexsoft.essentials.api.world.ManagedWorld;
import dev.vexsoft.essentials.api.world.ManagedWorldState;
import dev.vexsoft.essentials.api.world.WorldGeneratorType;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;

class ManagedWorldSuggestionProviderTest {

    @Test
    void suggestsTheCommandAliasesForVanillaDimensionsAndKeepsCustomWorldNames() {
        var worlds = (ManagedWorldService) Proxy.newProxyInstance(
            ManagedWorldService.class.getClassLoader(),
            new Class<?>[] {ManagedWorldService.class},
            (instance, method, arguments) -> {
                if (method.getName().equals("getWorlds")) {
                    return List.of(
                        managed("minecraft", "the_nether"),
                        managed("vexessentials", "nether-isles"),
                        managed("vexessentials", "the_nether"),
                        managed("minecraft", "the_end"),
                        managed("vexessentials", "end-isles"),
                        managed("vexessentials", "the_end")
                    );
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
        var registry = (VexServiceRegistry) Proxy.newProxyInstance(
            VexServiceRegistry.class.getClassLoader(),
            new Class<?>[] {VexServiceRegistry.class},
            (instance, method, arguments) -> {
                if (method.getName().equals("require") && arguments[0] == ManagedWorldService.class) {
                    return worlds;
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
        var suggestions = new ManagedWorldSuggestionProvider(registry)
            .suggest(null, new SuggestionsBuilder("", 0)).join();

        assertEquals(List.of("end", "end-isles", "nether", "nether-isles", "the_end", "the_nether"),
            suggestions.getList().stream()
            .map(Suggestion::getText).toList());
    }

    private ManagedWorld managed(final String namespace, final String name) {
        return new ManagedWorld(
            new WorldKey(namespace, name), WorldGeneratorType.NETHER_VOID, ManagedWorldState.LOADED, true
        );
    }
}
