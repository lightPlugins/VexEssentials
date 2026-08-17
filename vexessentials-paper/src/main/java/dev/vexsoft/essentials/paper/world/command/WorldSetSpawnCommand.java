package dev.vexsoft.essentials.paper.world.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.paper.service.world.command.WorldCommandService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Sets the current dimension's spawn below the shared world root. */
@CommandRoot(name = "world", description = "Manages server dimensions")
@Dependencies(WorldCommandService.class)
public final class WorldSetSpawnCommand {

  private final WorldCommandService commands;

  public WorldSetSpawnCommand(final VexServiceRegistry services) {
    commands = Objects.requireNonNull(services, "services").require(WorldCommandService.class);
  }

  /** Uses the sender's exact position as the current world's spawn. */
  @Command(
      value = "setspawn",
      permission = "vexessentials.command.world.setspawn",
      playerOnly = true
  )
  public CompletableFuture<Boolean> setSpawn(final VexCommandSource source) {
    return commands.setSpawn(source);
  }
}
