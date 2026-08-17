package dev.vexsoft.essentials.paper.world.generator;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

/** Thread-safe empty generator with a stable spawn above the origin. */
public final class VoidChunkGenerator extends ChunkGenerator {

  @Override
  public void generateNoise(
      final @NotNull WorldInfo worldInfo,
      final @NotNull Random random,
      final int chunkX,
      final int chunkZ,
      final @NotNull ChunkData chunkData
  ) {
    if (chunkX == 0 && chunkZ == 0) {
      chunkData.setBlock(0, 63, 0, Material.BEDROCK);
    }
  }

  @Override
  public Location getFixedSpawnLocation(
      final @NotNull World world,
      final @NotNull Random random
  ) {
    return new Location(world, 0.5, 64, 0.5);
  }
}
