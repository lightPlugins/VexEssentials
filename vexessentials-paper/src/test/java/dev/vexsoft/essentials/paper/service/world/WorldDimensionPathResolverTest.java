package dev.vexsoft.essentials.paper.service.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.vexsoft.core.api.world.WorldKey;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WorldDimensionPathResolverTest {

  @Test
  void resolvesWorldBelowNamespacedDimensionsDirectory() {
    Path level = Path.of("build", "test-level").toAbsolutePath();

    Path resolved = WorldDimensionPathResolver.resolve(
        level,
        new WorldKey("vexessentials", "farmworld")
    );

    assertEquals(
        level.resolve("dimensions").resolve("vexessentials").resolve("farmworld").normalize(),
        resolved
    );
  }

  @Test
  void preservesNestedWorldKeyPaths() {
    Path level = Path.of("build", "test-level").toAbsolutePath();

    Path resolved = WorldDimensionPathResolver.resolve(
        level,
        new WorldKey("vexessentials", "season/summer")
    );

    assertEquals(
        level.resolve("dimensions/vexessentials/season/summer").normalize(),
        resolved
    );
  }
}
