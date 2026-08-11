package dev.vexsoft.essentials.paper.service.warp.command;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.player.PlayerService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.command.VexCommandSource;
import java.util.Objects;
import org.bukkit.entity.Player;

/** Default VexPlayer resolver for player-only warp command roots. */
@Dependencies(PlayerService.class)
public final class VexWarpCommandContext implements WarpCommandContext {

  private final PlayerService players;

  public VexWarpCommandContext(final VexServiceRegistry services) {
    players = Objects.requireNonNull(services, "services").require(PlayerService.class);
  }

  @Override
  public VexPlayer player(final VexCommandSource source) {
    Player player = (Player) Objects.requireNonNull(source, "source").getSender();
    return players.require(player.getUniqueId());
  }
}
