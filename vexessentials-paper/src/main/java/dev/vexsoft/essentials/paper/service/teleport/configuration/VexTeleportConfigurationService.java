package dev.vexsoft.essentials.paper.service.teleport.configuration;

import dev.vexsoft.core.api.configuration.ConfigurationOwner;
import dev.vexsoft.core.api.configuration.VexConfiguration;
import dev.vexsoft.core.api.service.configuration.ConfigurationService;
import dev.vexsoft.core.api.service.registry.Dependencies;
import dev.vexsoft.core.api.service.registry.VexServiceRegistry;
import dev.vexsoft.essentials.paper.teleport.configuration.SoundProfile;
import java.time.Duration;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.kyori.adventure.sound.Sound;

/** Loads teleport settings with safe fallbacks and understandable console warnings. */
@Dependencies(ConfigurationService.class)
public final class VexTeleportConfigurationService implements TeleportConfigurationService {

  private static final Pattern SOUND_KEY = Pattern.compile(
      "[a-z0-9._-]+:[a-z0-9/._-]+"
  );
  private static final Map<String, SoundProfile> DEFAULT_SOUNDS = Map.of(
      "request-sent", profile("minecraft:block.note_block.pling", 0.8F, 1.2F),
      "request-received", profile("minecraft:block.note_block.pling", 0.9F, 1.0F),
      "request-accepted", profile("minecraft:entity.player.levelup", 0.8F, 1.1F),
      "request-denied", profile("minecraft:block.note_block.bass", 0.8F, 0.8F),
      "warmup-start", profile("minecraft:block.note_block.hat", 0.7F, 1.1F),
      "teleport-cancelled", profile("minecraft:block.note_block.bass", 0.8F, 0.7F),
      "teleport-success", profile("minecraft:entity.enderman.teleport", 0.8F, 1.0F),
      "teleport-failed", profile("minecraft:block.note_block.bass", 0.8F, 0.6F)
  );

  private final ConfigurationService configurations;
  private final ConfigurationOwner owner;
  private final Map<String, SoundProfile> sounds = new ConcurrentHashMap<>();
  private volatile Duration requestExpiration = Duration.ofSeconds(60);
  private volatile Duration requestCooldown = Duration.ofSeconds(5);
  private volatile int maximumRequests = 10_000;
  private volatile Duration networkTimeout = Duration.ofSeconds(5);
  private volatile Duration warmup = Duration.ofSeconds(3);
  private volatile boolean cancelWarmupOnMove = true;
  private volatile boolean cancelWarmupOnDamage = true;

  /** Creates and initially loads the configuration service. */
  public VexTeleportConfigurationService(final VexServiceRegistry services) {
    VexServiceRegistry checked = Objects.requireNonNull(services, "services");
    configurations = checked.require(ConfigurationService.class);
    owner = checked.getOwner() instanceof ConfigurationOwner configurationOwner
        ? configurationOwner
        : null;
    reload();
  }

  @Override
  public Duration requestExpiration() {
    return requestExpiration;
  }

  @Override
  public Duration requestCooldown() {
    return requestCooldown;
  }

  @Override
  public int maximumRequests() {
    return maximumRequests;
  }

  @Override
  public Duration networkTimeout() {
    return networkTimeout;
  }

  @Override
  public Duration warmup() {
    return warmup;
  }

  @Override
  public boolean cancelWarmupOnMove() {
    return cancelWarmupOnMove;
  }

  @Override
  public boolean cancelWarmupOnDamage() {
    return cancelWarmupOnDamage;
  }

  @Override
  public SoundProfile sound(final String event) {
    String checked = Objects.requireNonNull(event, "event").toLowerCase(Locale.ROOT);
    return sounds.getOrDefault(checked, DEFAULT_SOUNDS.getOrDefault(
        checked,
        new SoundProfile(false, "minecraft:block.note_block.pling", "player", 1, 1)
    ));
  }

  @Override
  public synchronized void reload() {
    try {
      VexConfiguration configuration = configurations.load("teleport.yml");
      Duration loadedExpiration = positiveDuration(
          configuration.getLong("requests.expiration-seconds", 60),
          "requests.expiration-seconds",
          Duration.ofSeconds(60)
      );
      Duration loadedCooldown = nonNegativeDuration(
          configuration.getLong("requests.cooldown-seconds", 5),
          "requests.cooldown-seconds",
          Duration.ofSeconds(5)
      );
      int loadedMaximum = positiveInt(
          configuration.getInt("requests.maximum-cached", 10_000),
          "requests.maximum-cached",
          10_000
      );
      Duration loadedNetworkTimeout = positiveDuration(
          configuration.getLong("network.timeout-seconds", 5),
          "network.timeout-seconds",
          Duration.ofSeconds(5)
      );
      Duration loadedWarmup = nonNegativeDuration(
          configuration.getLong("teleport.warmup-seconds", 3),
          "teleport.warmup-seconds",
          Duration.ofSeconds(3)
      );
      boolean loadedCancelOnMove = configuration.getBoolean(
          "teleport.cancel-on-move",
          true
      );
      boolean loadedCancelOnDamage = configuration.getBoolean(
          "teleport.cancel-on-damage",
          true
      );
      Map<String, SoundProfile> loadedSounds = new HashMap<>();
      for (Map.Entry<String, SoundProfile> entry : DEFAULT_SOUNDS.entrySet()) {
        loadedSounds.put(
            entry.getKey(),
            readSound(configuration, entry.getKey(), entry.getValue())
        );
      }
      requestExpiration = loadedExpiration;
      requestCooldown = loadedCooldown;
      maximumRequests = loadedMaximum;
      networkTimeout = loadedNetworkTimeout;
      warmup = loadedWarmup;
      cancelWarmupOnMove = loadedCancelOnMove;
      cancelWarmupOnDamage = loadedCancelOnDamage;
      sounds.clear();
      sounds.putAll(loadedSounds);
    } catch (RuntimeException exception) {
      requestExpiration = Duration.ofSeconds(60);
      requestCooldown = Duration.ofSeconds(5);
      maximumRequests = 10_000;
      networkTimeout = Duration.ofSeconds(5);
      warmup = Duration.ofSeconds(3);
      cancelWarmupOnMove = true;
      cancelWarmupOnDamage = true;
      sounds.clear();
      sounds.putAll(DEFAULT_SOUNDS);
      warn(
          "The file 'teleport.yml' could not be loaded. VexEssentials will use its safe "
              + "default teleport settings instead.",
          exception
      );
    }
  }

  private SoundProfile readSound(
      final VexConfiguration configuration,
      final String event,
      final SoundProfile fallback
  ) {
    String root = "sounds." + event;
    boolean enabled = configuration.getBoolean(root + ".enabled", fallback.enabled());
    String key = configuration.getString(root + ".key", fallback.key()).trim().toLowerCase(
        Locale.ROOT
    );
    if (!SOUND_KEY.matcher(key).matches()) {
      warn(
          "The sound configured at '" + root + ".key' in 'teleport.yml' could not be "
              + "used because '" + key + "' is not a valid namespaced key. Expected a value "
              + "such as 'minecraft:block.note_block.pling' or 'nexo:teleport_request'. "
              + "This sound has been disabled.",
          null
      );
      return new SoundProfile(false, fallback.key(), fallback.source(), fallback.volume(),
          fallback.pitch());
    }
    String source = configuration.getString(root + ".source", fallback.source())
        .trim().toLowerCase(Locale.ROOT);
    try {
      Sound.Source.valueOf(source.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      warn(
          "The sound source configured at '" + root + ".source' in 'teleport.yml' could not "
              + "be used because '" + source + "' is unknown. Expected a source such as "
              + "'player', 'master', or 'ambient'. This sound has been disabled.",
          null
      );
      return new SoundProfile(false, key, fallback.source(), fallback.volume(), fallback.pitch());
    }
    float volume = boundedFloat(
        configuration.getDouble(root + ".volume", fallback.volume()),
        root + ".volume",
        fallback.volume()
    );
    float pitch = boundedFloat(
        configuration.getDouble(root + ".pitch", fallback.pitch()),
        root + ".pitch",
        fallback.pitch()
    );
    return new SoundProfile(enabled, key, source, volume, pitch);
  }

  private Duration positiveDuration(
      final long seconds,
      final String path,
      final Duration fallback
  ) {
    if (seconds > 0) {
      return Duration.ofSeconds(seconds);
    }
    warnValue(path, seconds, "a number greater than zero", fallback.toSeconds());
    return fallback;
  }

  private Duration nonNegativeDuration(
      final long seconds,
      final String path,
      final Duration fallback
  ) {
    if (seconds >= 0) {
      return Duration.ofSeconds(seconds);
    }
    warnValue(path, seconds, "zero or a positive number", fallback.toSeconds());
    return fallback;
  }

  private int positiveInt(final int value, final String path, final int fallback) {
    if (value > 0) {
      return value;
    }
    warnValue(path, value, "a number greater than zero", fallback);
    return fallback;
  }

  private float boundedFloat(final double value, final String path, final float fallback) {
    if (Double.isFinite(value) && value >= 0 && value <= 2) {
      return (float) value;
    }
    warnValue(path, value, "a number between 0 and 2", fallback);
    return fallback;
  }

  private void warnValue(
      final String path,
      final Object value,
      final String expected,
      final Object fallback
  ) {
    warn(
        "The value '" + value + "' configured at '" + path + "' in 'teleport.yml' is "
            + "invalid. Expected " + expected + ". VexEssentials will use '" + fallback
            + "' instead.",
        null
    );
  }

  private void warn(final String message, final Throwable cause) {
    if (owner != null) {
      owner.reportConfigurationWarning(message, cause);
    }
  }

  private static SoundProfile profile(final String key, final float volume, final float pitch) {
    return new SoundProfile(true, key, "player", volume, pitch);
  }
}
