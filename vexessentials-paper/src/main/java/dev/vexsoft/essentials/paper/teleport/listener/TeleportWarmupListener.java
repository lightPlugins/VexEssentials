package dev.vexsoft.essentials.paper.teleport.listener;

import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.service.teleport.configuration.TeleportConfigurationService;
import dev.vexsoft.essentials.paper.service.teleport.execution.TeleportWarmupService;
import dev.vexsoft.essentials.paper.teleport.execution.WarmupCancelReason;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Cancels active teleport warmups when their configured player actions occur. */
@Dependencies({TeleportWarmupService.class, TeleportConfigurationService.class})
public final class TeleportWarmupListener implements Listener {

  private final TeleportWarmupService warmups;
  private final TeleportConfigurationService configuration;

  /** Creates the warmup listener. */
  public TeleportWarmupListener(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    warmups = checked.require(TeleportWarmupService.class);
    configuration = checked.require(TeleportConfigurationService.class);
  }

  /** Cancels a warmup after an actual position change, while allowing head rotation. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMove(final PlayerMoveEvent event) {
    if (!configuration.cancelWarmupOnMove()) {
      return;
    }
    Location from = event.getFrom();
    Location to = event.getTo();
    if (to != null && samePosition(from, to)) {
      return;
    }
    warmups.cancel(event.getPlayer().getUniqueId(), WarmupCancelReason.MOVED);
  }

  /** Cancels a warmup when the player receives non-cancelled damage. */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDamage(final EntityDamageEvent event) {
    if (configuration.cancelWarmupOnDamage() && event.getEntity() instanceof Player player) {
      warmups.cancel(player.getUniqueId(), WarmupCancelReason.DAMAGED);
    }
  }

  /** Releases a pending warmup immediately when its player disconnects. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(final PlayerQuitEvent event) {
    warmups.cancel(event.getPlayer().getUniqueId(), WarmupCancelReason.LEFT);
  }

  private boolean samePosition(final Location first, final Location second) {
    return first.getWorld() == second.getWorld()
        && Double.compare(first.getX(), second.getX()) == 0
        && Double.compare(first.getY(), second.getY()) == 0
        && Double.compare(first.getZ(), second.getZ()) == 0;
  }
}
