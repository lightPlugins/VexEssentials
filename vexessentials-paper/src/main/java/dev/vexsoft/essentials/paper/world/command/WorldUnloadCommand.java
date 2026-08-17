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

/** Unloads a managed dimension below the shared world root. */
@CommandRoot(name = "world", description = "Manages server dimensions")
@Dependencies(WorldCommandService.class)
public final class WorldUnloadCommand {

  private final WorldCommandService commands;

  public WorldUnloadCommand(final VexServiceRegistry services) {
    commands = Objects.requireNonNull(services, "services").require(WorldCommandService.class);
  }

  /** Safely unloads a dimension when it contains no players. */
  @Command(value = "unload <world>", permission = "vexessentials.command.world.unload")
  public CompletableFuture<Boolean> unload(
      final VexCommandSource source,
      @Argument("world") @Suggest(ManagedWorldSuggestionProvider.class) final String world
  ) {
    return commands.unload(source, world);
  }
}
