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

/** Teleports a player below the shared world root. */
@CommandRoot(name = "world", description = "Manages server dimensions")
@Dependencies(WorldCommandService.class)
public final class WorldTeleportCommand {

  private final WorldCommandService commands;

  public WorldTeleportCommand(final VexServiceRegistry services) {
    commands = Objects.requireNonNull(services, "services").require(WorldCommandService.class);
  }

  /** Teleports the sender to the selected world's spawn. */
  @Command(
      value = "teleport <world>",
      permission = "vexessentials.command.world.teleport",
      playerOnly = true
  )
  public CompletableFuture<Boolean> teleport(
      final VexCommandSource source,
      @Argument("world") @Suggest(ManagedWorldSuggestionProvider.class) final String world
  ) {
    return commands.teleport(source, world);
  }
}
