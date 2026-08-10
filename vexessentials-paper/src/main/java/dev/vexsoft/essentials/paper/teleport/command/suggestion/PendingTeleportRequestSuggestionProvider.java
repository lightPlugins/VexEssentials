package dev.vexsoft.essentials.paper.teleport.command.suggestion;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.core.paper.command.suggestion.SuggestionProvider;
import dev.vexsoft.essentials.paper.service.teleport.request.TeleportRequestService;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/** Suggests requester names for the command sender's pending teleport requests. */
@Dependencies(TeleportRequestService.class)
public final class PendingTeleportRequestSuggestionProvider implements SuggestionProvider {

  private final TeleportRequestService requests;

  public PendingTeleportRequestSuggestionProvider(final VexServiceRegistry services) {
    requests = Objects.requireNonNull(services, "services")
        .require(TeleportRequestService.class);
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
    requests.getIncomingSuggestions(player.getUniqueId()).stream()
        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(remaining))
        .forEach(builder::suggest);
    return builder.buildFuture();
  }
}
