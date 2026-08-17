package dev.vexsoft.essentials.paper.world.command.suggestion;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.core.paper.command.suggestion.SuggestionProvider;
import dev.vexsoft.essentials.api.service.world.ManagedWorldService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Suggests every world registered with the managed world service. */
@Dependencies(ManagedWorldService.class)
public final class ManagedWorldSuggestionProvider implements SuggestionProvider {

  private final ManagedWorldService worlds;

  public ManagedWorldSuggestionProvider(final VexServiceRegistry services) {
    worlds = Objects.requireNonNull(services, "services").require(ManagedWorldService.class);
  }

  @Override
  public CompletableFuture<Suggestions> suggest(
      final VexCommandSource source,
      final SuggestionsBuilder builder
  ) {
    String remaining = builder.getRemainingLowerCase();
    worlds.getWorlds().stream()
        .map(world -> world.key().value())
        .filter(world -> world.startsWith(remaining))
        .forEach(builder::suggest);
    return builder.buildFuture();
  }
}
