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
import org.bukkit.entity.Player;

/** Suggests only warps the current player is allowed to access. */
@Dependencies(WarpService.class)
public final class AccessibleWarpSuggestionProvider implements SuggestionProvider {

  private final WarpService warps;

  public AccessibleWarpSuggestionProvider(final VexServiceRegistry services) {
    warps = Objects.requireNonNull(services, "services").require(WarpService.class);
  }

  @Override
  public CompletableFuture<Suggestions> suggest(
      final VexCommandSource source,
      final SuggestionsBuilder builder
  ) {
    if (!(source.getSender() instanceof Player player)) {
      return builder.buildFuture();
    }
    String remaining = builder.getRemainingLowerCase();
    warps.getWarps().stream()
        .filter(warp -> warp.id().startsWith(remaining))
        .filter(warp -> player.hasPermission(warp.accessPermission()))
        .forEach(warp -> builder.suggest(warp.id()));
    return builder.buildFuture();
  }
}
