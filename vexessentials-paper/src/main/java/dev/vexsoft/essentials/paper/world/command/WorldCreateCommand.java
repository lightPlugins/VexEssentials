package dev.vexsoft.essentials.paper.world.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.OptionalArgument;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.api.world.WorldGeneratorType;
import dev.vexsoft.essentials.paper.service.world.command.WorldCommandService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Creates one managed dimension below the shared world root. */
@CommandRoot(name = "world", description = "Manages server dimensions")
@Dependencies(WorldCommandService.class)
public final class WorldCreateCommand {

  private final WorldCommandService commands;

  public WorldCreateCommand(final VexServiceRegistry services) {
    commands = Objects.requireNonNull(services, "services").require(WorldCommandService.class);
  }

  /** Creates a normal, flat, or void dimension and teleports the creator to its spawn. */
  @Command(
      value = "create <world> <generator> [seed]",
      permission = "vexessentials.command.world.create",
      playerOnly = true
  )
  public CompletableFuture<Boolean> create(
      final VexCommandSource source,
      @Argument("world") final String world,
      @Argument("generator") final WorldGeneratorType generator,
      @OptionalArgument("seed") final String seed
  ) {
    return commands.create(source, world, generator, seed);
  }
}
