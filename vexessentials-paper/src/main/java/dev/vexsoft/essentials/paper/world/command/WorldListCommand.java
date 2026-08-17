package dev.vexsoft.essentials.paper.world.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.paper.service.world.command.WorldCommandService;
import java.util.Objects;

/** Lists managed dimensions below the shared world root. */
@CommandRoot(name = "world", description = "Manages server dimensions")
@Dependencies(WorldCommandService.class)
public final class WorldListCommand {

  private final WorldCommandService commands;

  public WorldListCommand(final VexServiceRegistry services) {
    commands = Objects.requireNonNull(services, "services").require(WorldCommandService.class);
  }

  /** Lists every managed dimension and its runtime state. */
  @Command(value = "list", permission = "vexessentials.command.world.list")
  public boolean list(final VexCommandSource source) {
    return commands.list(source);
  }
}
