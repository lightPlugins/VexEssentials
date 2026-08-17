package dev.vexsoft.essentials.api.world;

import dev.vexsoft.core.api.world.WorldKey;
import java.util.Objects;

/** Immutable public view of a managed Paper dimension. */
public record ManagedWorld(
    WorldKey key,
    WorldGeneratorType generator,
    ManagedWorldState state,
    boolean autoLoad
) {

  public ManagedWorld {
    key = Objects.requireNonNull(key, "key");
    generator = Objects.requireNonNull(generator, "generator");
    state = Objects.requireNonNull(state, "state");
  }
}
