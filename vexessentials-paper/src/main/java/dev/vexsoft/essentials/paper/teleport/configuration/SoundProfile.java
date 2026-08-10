package dev.vexsoft.essentials.paper.teleport.configuration;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/** Stores one validated, namespaced sound configuration. */
public record SoundProfile(
    boolean enabled,
    Key key,
    Sound.Source source,
    float volume,
    float pitch
) {
}
