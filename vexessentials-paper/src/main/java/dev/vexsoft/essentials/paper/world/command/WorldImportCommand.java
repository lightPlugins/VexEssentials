package dev.vexsoft.essentials.paper.world.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.paper.service.world.command.WorldCommandService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Imports an existing dimension below the shared world root. */
@CommandRoot(name = "world", description = "Manages server dimensions")
@Dependencies(WorldCommandService.class)
public final class WorldImportCommand {

  private final WorldCommandService commands;

  public WorldImportCommand(final VexServiceRegistry services) {
    commands = Objects.requireNonNull(services, "services").require(WorldCommandService.class);
  }

  /** Registers an existing dimensions directory without changing its files. */
  @Command(value = "import <world>", permission = "vexessentials.command.world.import")
  public CompletableFuture<Boolean> importWorld(
      final VexCommandSource source,
      @Argument("world") final String world
  ) {
    return commands.importWorld(source, world);
  }
}
