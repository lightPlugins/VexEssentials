package dev.vexsoft.essentials.paper.warp.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.Suggest;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.essentials.paper.service.warp.command.WarpCommandContext;
import dev.vexsoft.essentials.paper.service.warp.command.WarpCommandService;
import dev.vexsoft.essentials.paper.warp.command.suggestion.WarpSuggestionProvider;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Warp-position update binding below the shared warp root. */
@CommandRoot(name = "warp", description = "Manages and uses server warps", playerOnly = true)
@Dependencies({WarpCommandContext.class, WarpCommandService.class})
public final class WarpUpdateCommand {

  private final WarpCommandContext context;
  private final WarpCommandService commands;

  public WarpUpdateCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    context = checked.require(WarpCommandContext.class);
    commands = checked.require(WarpCommandService.class);
  }

  /** Updates a global warp to the sender's exact position. */
  @Command(value = "update <warp>", permission = "vexessentials.command.warp.update")
  public CompletableFuture<Boolean> update(
      final VexCommandSource source,
      @Argument("warp") @Suggest(WarpSuggestionProvider.class) final String warp
  ) {
    return commands.update(context.player(source), warp);
  }
}
