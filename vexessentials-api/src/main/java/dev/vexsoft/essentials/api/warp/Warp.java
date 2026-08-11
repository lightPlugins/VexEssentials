package dev.vexsoft.essentials.api.warp;

import dev.vexsoft.core.api.world.ServerPosition;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Persisted global warp with deterministic localization and permission identifiers. */
public record Warp(
    String id,
    ServerPosition position,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt
) {

  /** Creates a validated warp. */
  public Warp {
    id = normalizeId(id);
    position = Objects.requireNonNull(position, "position");
    createdBy = Objects.requireNonNull(createdBy, "createdBy");
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
    updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }

  /** Returns the localization key used for this warp's display name. */
  public String nameKey() {
    return "warps." + id + ".name";
  }

  /** Returns the localization key used for this warp's multi-line description. */
  public String descriptionKey() {
    return "warps." + id + ".description";
  }

  /** Returns the permission required to use this warp. */
  public String accessPermission() {
    return "vexessentials.warps." + id + ".access";
  }

  /** Normalizes and validates an end-user warp identifier. */
  public static String normalizeId(final String value) {
    String normalized = Objects.requireNonNull(value, "id").trim().toLowerCase(Locale.ROOT);
    if (!normalized.matches("[a-z][a-z0-9_-]{0,62}")) {
      throw new IllegalArgumentException(
          "Warp IDs must start with a letter and only contain letters, numbers, '_' or '-'"
      );
    }
    return normalized;
  }
}
