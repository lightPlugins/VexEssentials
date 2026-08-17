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

/** Shows managed dimension details below the shared world root. */
@CommandRoot(name = "world", description = "Manages server dimensions")
@Dependencies(WorldCommandService.class)
public final class WorldInfoCommand {

  private final WorldCommandService commands;

  public WorldInfoCommand(final VexServiceRegistry services) {
    commands = Objects.requireNonNull(services, "services").require(WorldCommandService.class);
  }

  /** Shows the generator, state, and startup behavior of one managed dimension. */
  @Command(value = "info <world>", permission = "vexessentials.command.world.info")
  public boolean info(
      final VexCommandSource source,
      @Argument("world") @Suggest(ManagedWorldSuggestionProvider.class) final String world
  ) {
    return commands.info(source, world);
  }
}
