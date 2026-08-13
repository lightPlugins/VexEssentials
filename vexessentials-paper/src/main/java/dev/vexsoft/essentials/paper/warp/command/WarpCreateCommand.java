package dev.vexsoft.essentials.paper.warp.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.core.paper.command.Greedy;
import dev.vexsoft.core.paper.command.OptionalArgument;
import dev.vexsoft.essentials.paper.service.warp.command.WarpCommandContext;
import dev.vexsoft.essentials.paper.service.warp.command.WarpCommandService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Warp-creation binding below the shared warp root. */
@CommandRoot(name = "warp", description = "Manages and uses server warps", playerOnly = true)
@Dependencies({WarpCommandContext.class, WarpCommandService.class})
public final class WarpCreateCommand {

  private final WarpCommandContext context;
  private final WarpCommandService commands;

  public WarpCreateCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    context = checked.require(WarpCommandContext.class);
    commands = checked.require(WarpCommandService.class);
  }

  /** Creates a global warp at the sender's exact position. */
  @Command(
      value = "create <warp> [display-name]",
      permission = "vexessentials.command.warp.create"
  )
  public CompletableFuture<Boolean> create(
      final VexCommandSource source,
      @Argument("warp") final String warp,
      @OptionalArgument("display-name") @Greedy final String displayName
  ) {
    return commands.create(context.player(source), warp, displayName);
  }
}
