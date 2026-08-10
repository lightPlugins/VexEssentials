package dev.vexsoft.essentials.paper.service.teleport.configuration;

import dev.vexsoft.core.api.service.registry.VexService;
import dev.vexsoft.essentials.paper.teleport.configuration.SoundProfile;
import java.time.Duration;

/** Supplies the validated runtime configuration used by the teleport feature. */
public interface TeleportConfigurationService extends VexService {

  Duration requestExpiration();

  Duration requestCooldown();

  int maximumRequests();

  Duration networkTimeout();

  Duration warmup();

  boolean cancelWarmupOnMove();

  boolean cancelWarmupOnDamage();

  SoundProfile sound(String event);

  void reload();
}
