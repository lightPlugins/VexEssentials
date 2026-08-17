package dev.vexsoft.essentials.paper.service.world.listener;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.core.paper.service.world.WorldService;
import dev.vexsoft.essentials.api.service.world.ManagedWorldService;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Moves joining players to the configured server spawn when enabled. */
@Dependencies({ManagedWorldService.class, ScheduleService.class, WorldService.class})
public final class WorldSpawnJoinListener implements Listener {

  private final ManagedWorldService managedWorlds;
  private final ScheduleService schedules;
  private final WorldService worlds;

  /** Creates the server-spawn join listener. */
  public WorldSpawnJoinListener(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    managedWorlds = checked.require(ManagedWorldService.class);
    schedules = checked.require(ScheduleService.class);
    worlds = checked.require(WorldService.class);
  }

  /** Teleports the player on the next entity tick after a completed join. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(final PlayerJoinEvent event) {
    if (!managedWorlds.teleportToServerSpawnOnJoin()) {
      return;
    }
    Player player = event.getPlayer();
    schedules.runFor(player, () -> managedWorlds.getServerSpawn()
        .flatMap(worlds::createLocation)
        .ifPresent(player::teleportAsync));
  }
}
