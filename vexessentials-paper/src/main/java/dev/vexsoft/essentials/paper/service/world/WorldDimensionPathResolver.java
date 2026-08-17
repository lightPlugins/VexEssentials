package dev.vexsoft.essentials.paper.service.world;

import dev.vexsoft.core.api.world.WorldKey;
import java.nio.file.Path;
import java.util.Objects;

/** Resolves custom world keys below a server's canonical dimensions directory. */
final class WorldDimensionPathResolver {

  private WorldDimensionPathResolver() {
  }

  static Path resolve(final Path levelDirectory, final WorldKey key) {
    Path dimensions = Objects.requireNonNull(levelDirectory, "levelDirectory")
        .resolve("dimensions")
        .toAbsolutePath()
        .normalize();
    WorldKey checkedKey = Objects.requireNonNull(key, "key");
    Path target = dimensions.resolve(checkedKey.namespace()).resolve(checkedKey.value()).normalize();
    if (!target.startsWith(dimensions)) {
      throw new IllegalArgumentException("World key escapes the dimensions directory");
    }
    return target;
  }
}
