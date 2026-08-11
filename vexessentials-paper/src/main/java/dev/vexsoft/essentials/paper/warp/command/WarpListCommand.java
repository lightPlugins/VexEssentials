package dev.vexsoft.essentials.paper.warp.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.paper.service.warp.command.WarpCommandContext;
import dev.vexsoft.essentials.paper.service.warp.command.WarpCommandService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Accessible warp-list binding below the shared warp root. */
@CommandRoot(name = "warp", description = "Manages and uses server warps", playerOnly = true)
@Dependencies({WarpCommandContext.class, WarpCommandService.class})
public final class WarpListCommand {

  private final WarpCommandContext context;
  private final WarpCommandService commands;

  public WarpListCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    context = checked.require(WarpCommandContext.class);
    commands = checked.require(WarpCommandService.class);
  }

  /** Shows every warp the sender may access. */
  @Command(value = "list", permission = "vexessentials.command.warp.list")
  public CompletableFuture<Boolean> list(final VexCommandSource source) {
    return commands.list(context.player(source));
  }
}
