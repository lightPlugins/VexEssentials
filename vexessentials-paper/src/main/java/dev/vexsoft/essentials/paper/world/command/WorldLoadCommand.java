package dev.vexsoft.essentials.paper.world.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.core.paper.command.Suggest;
import dev.vexsoft.essentials.paper.service.world.command.WorldCommandService;
import dev.vexsoft.essentials.paper.world.command.suggestion.ManagedWorldSuggestionProvider;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Loads a managed dimension below the shared world root. */
@CommandRoot(name = "world", description = "Manages server dimensions")
@Dependencies(WorldCommandService.class)
public final class WorldLoadCommand {

  private final WorldCommandService commands;

  public WorldLoadCommand(final VexServiceRegistry services) {
    commands = Objects.requireNonNull(services, "services").require(WorldCommandService.class);
  }

  /** Loads a currently unloaded managed dimension. */
  @Command(value = "load <world>", permission = "vexessentials.command.world.load")
  public CompletableFuture<Boolean> load(
      final VexCommandSource source,
      @Argument("world") @Suggest(ManagedWorldSuggestionProvider.class) final String world
  ) {
    return commands.load(source, world);
  }
}
