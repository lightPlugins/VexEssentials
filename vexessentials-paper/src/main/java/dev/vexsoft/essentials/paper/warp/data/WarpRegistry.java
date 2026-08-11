package dev.vexsoft.essentials.paper.warp.data;

import dev.vexsoft.essentials.api.warp.Warp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** JSON-serializable immutable value stored under the global warp data key. */
public record WarpRegistry(Map<String, Warp> warps) {

  /** Creates a stable registry ordered by normalized warp identifier. */
  public WarpRegistry {
    Objects.requireNonNull(warps, "warps");
    Map<String, Warp> normalized = new TreeMap<>();
    warps.forEach((id, warp) -> {
      Warp checked = Objects.requireNonNull(warp, "warp");
      String normalizedId = Warp.normalizeId(id);
      if (!normalizedId.equals(checked.id())) {
        throw new IllegalArgumentException("Warp registry key does not match warp ID: " + id);
      }
      normalized.put(normalizedId, checked);
    });
    warps = Collections.unmodifiableMap(new LinkedHashMap<>(normalized));
  }

  /** Creates an empty persistent registry. */
  public static WarpRegistry empty() {
    return new WarpRegistry(Map.of());
  }

  /** Returns a copy containing the supplied warp. */
  public WarpRegistry with(final Warp warp) {
    Warp checked = Objects.requireNonNull(warp, "warp");
    Map<String, Warp> updated = new LinkedHashMap<>(warps);
    updated.put(checked.id(), checked);
    return new WarpRegistry(updated);
  }

  /** Returns a copy without the supplied warp identifier. */
  public WarpRegistry without(final String id) {
    Map<String, Warp> updated = new LinkedHashMap<>(warps);
    updated.remove(Warp.normalizeId(id));
    return new WarpRegistry(updated);
  }
}
