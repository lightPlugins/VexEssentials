package dev.vexsoft.essentials.paper.warp.command;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.Argument;
import dev.vexsoft.core.paper.command.Command;
import dev.vexsoft.core.paper.command.CommandRoot;
import dev.vexsoft.core.paper.command.Suggest;
import dev.vexsoft.core.paper.command.VexCommandSource;
import dev.vexsoft.core.paper.command.suggestion.PlayerNameSuggestionProvider;
import dev.vexsoft.essentials.paper.service.warp.command.WarpCommandContext;
import dev.vexsoft.essentials.paper.service.warp.command.WarpCommandService;
import dev.vexsoft.essentials.paper.warp.command.suggestion.AccessibleWarpSuggestionProvider;
import dev.vexsoft.essentials.paper.warp.command.suggestion.WarpSuggestionProvider;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Player and administrative teleport bindings below the shared warp root. */
@CommandRoot(name = "warp", description = "Manages and uses server warps", playerOnly = true)
@Dependencies({WarpCommandContext.class, WarpCommandService.class})
public final class WarpTeleportCommand {

  private final WarpCommandContext context;
  private final WarpCommandService commands;

  public WarpTeleportCommand(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    context = checked.require(WarpCommandContext.class);
    commands = checked.require(WarpCommandService.class);
  }

  /** Teleports the sender to an accessible warp. */
  @Command(value = "<warp>", permission = "vexessentials.command.warp")
  public CompletableFuture<Boolean> teleport(
      final VexCommandSource source,
      @Argument("warp") @Suggest(AccessibleWarpSuggestionProvider.class) final String warp
  ) {
    return commands.teleport(context.player(source), warp);
  }

  /** Teleports another network player to a warp without applying their access permission. */
  @Command(value = "teleport <player> <warp>", permission = "vexessentials.command.warp.teleport")
  public CompletableFuture<Boolean> teleportOther(
      final VexCommandSource source,
      @Argument("player") @Suggest(PlayerNameSuggestionProvider.class) final String player,
      @Argument("warp") @Suggest(WarpSuggestionProvider.class) final String warp
  ) {
    return commands.teleportOther(context.player(source), player, warp);
  }
}
