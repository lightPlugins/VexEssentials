package dev.vexsoft.essentials.paper.warp.command.suggestion;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.core.paper.command.suggestion.SuggestionProvider;
import dev.vexsoft.essentials.api.service.warp.WarpService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Suggests every registered warp for administrative subcommands. */
@Dependencies(WarpService.class)
public final class WarpSuggestionProvider implements SuggestionProvider {

  private final WarpService warps;

  public WarpSuggestionProvider(final VexServiceRegistry services) {
    warps = Objects.requireNonNull(services, "services").require(WarpService.class);
  }

  @Override
  public CompletableFuture<Suggestions> suggest(
      final VexCommandSource source,
      final SuggestionsBuilder builder
  ) {
    String remaining = builder.getRemainingLowerCase();
    warps.getWarps().stream()
        .map(warp -> warp.id())
        .filter(id -> id.startsWith(remaining))
        .forEach(builder::suggest);
    return builder.buildFuture();
  }
}
