package dev.vexsoft.essentials.paper.world.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.paper.service.world.command.WorldCommandService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Sets the central server spawn below the shared world root. */
@CommandRoot(name = "world", description = "Manages server dimensions")
@Dependencies(WorldCommandService.class)
public final class WorldSetServerSpawnCommand {

  private final WorldCommandService commands;

  public WorldSetServerSpawnCommand(final VexServiceRegistry services) {
    commands = Objects.requireNonNull(services, "services").require(WorldCommandService.class);
  }

  /** Uses the sender's exact current position as the server respawn location. */
  @Command(
      value = "setserverspawn",
      permission = "vexessentials.command.world.setserverspawn",
      playerOnly = true
  )
  public CompletableFuture<Boolean> setServerSpawn(final VexCommandSource source) {
    return commands.setServerSpawn(source);
  }
}
