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

/** Confirmed warp-deletion binding below the shared warp root. */
@CommandRoot(name = "warp", description = "Manages and uses server warps", playerOnly = true)
@Dependencies({WarpCommandContext.class, WarpCommandService.class})
public final class WarpDeleteCommand {

  private final WarpCommandContext context;
  private final WarpCommandService commands;

  public WarpDeleteCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    context = checked.require(WarpCommandContext.class);
    commands = checked.require(WarpCommandService.class);
  }

  /** Opens a confirmation dialog before deleting a global warp. */
  @Command(value = "delete <warp>", permission = "vexessentials.command.warp.delete")
  public CompletableFuture<Boolean> delete(
      final VexCommandSource source,
      @Argument("warp") @Suggest(WarpSuggestionProvider.class) final String warp
  ) {
    return commands.delete(context.player(source), warp);
  }
}
