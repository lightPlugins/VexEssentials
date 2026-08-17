package dev.vexsoft.essentials.paper.world.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.Suggest;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.paper.service.world.command.WorldCommandService;
import dev.vexsoft.essentials.paper.world.command.suggestion.ManagedWorldSuggestionProvider;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Permanently deletes a managed dimension below the shared world root. */
@CommandRoot(name = "world", description = "Manages server dimensions")
@Dependencies(WorldCommandService.class)
public final class WorldDeleteCommand {

  private final WorldCommandService commands;

  public WorldDeleteCommand(final VexServiceRegistry services) {
    commands = Objects.requireNonNull(services, "services").require(WorldCommandService.class);
  }

  /** Deletes a managed dimension after an explicit textual confirmation. */
  @Command(value = "delete <world> <confirmation>", permission = "vexessentials.command.world.delete")
  public CompletableFuture<Boolean> delete(
      final VexCommandSource source,
      @Argument("world") @Suggest(ManagedWorldSuggestionProvider.class) final String world,
      @Argument("confirmation") final String confirmation
  ) {
    return commands.delete(source, world, confirmation);
  }
}
