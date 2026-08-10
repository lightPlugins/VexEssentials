package dev.vexsoft.essentials.paper.service.teleport.sound;

import dev.vexsoft.core.api.player.VexPlayer;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.core.paper.service.scheduler.ScheduleService;
import dev.vexsoft.essentials.paper.service.teleport.configuration.TeleportConfigurationService;
import dev.vexsoft.essentials.paper.teleport.configuration.SoundProfile;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;

/** Adventure-based sound player supporting Minecraft and resource-pack namespaces. */
@Dependencies({TeleportConfigurationService.class, ScheduleService.class})
public final class VexTeleportSoundService implements TeleportSoundService {

  private final TeleportConfigurationService configuration;
  private final ScheduleService scheduler;
  private final Logger logger;

  /** Creates the sound service. */
  public VexTeleportSoundService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    configuration = checked.require(TeleportConfigurationService.class);
    scheduler = checked.require(ScheduleService.class);
    logger = Logger.getLogger(checked.getOwner().getServiceOwnerName());
  }

  @Override
  public void play(final VexPlayer vexPlayer, final String event) {
    Objects.requireNonNull(vexPlayer, "player");
    SoundProfile profile = configuration.sound(event);
    if (!profile.enabled()) {
      return;
    }
    Optional<Player> platformPlayer = vexPlayer.findPlatformPlayer(Player.class);
    if (platformPlayer.isEmpty()) {
      return;
    }
    try {
      Sound sound = Sound.sound(
          Key.key(profile.key()),
          Sound.Source.valueOf(profile.source().toUpperCase(Locale.ROOT)),
          profile.volume(),
          profile.pitch()
      );
      scheduler.runFor(platformPlayer.get(), () -> platformPlayer.get().playSound(sound));
    } catch (IllegalArgumentException exception) {
      logger.warning(
          "The configured sound event '" + event + "' could not be played because its key or "
              + "source is invalid. Check 'sounds." + event + "' in 'teleport.yml'."
      );
    }
  }
}
