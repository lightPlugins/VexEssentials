package dev.vexsoft.essentials.paper.teleport.configuration;

/** Stores one validated, namespaced sound configuration. */
public record SoundProfile(
    boolean enabled,
    String key,
    String source,
    float volume,
    float pitch
) {
}
