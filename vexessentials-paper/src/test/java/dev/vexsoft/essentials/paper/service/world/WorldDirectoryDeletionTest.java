package dev.vexsoft.essentials.paper.service.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldDirectoryDeletionTest {

  @TempDir
  Path temporaryDirectory;

  @Test
  void deletesOnlyTheSelectedDimensionTree() throws IOException {
    Path dimensions = Files.createDirectories(temporaryDirectory.resolve("dimensions"));
    Path target = Files.createDirectories(dimensions.resolve("vexessentials/farm/region"));
    Files.writeString(target.resolve("r.0.0.mca"), "test");
    Path sibling = Files.createDirectories(dimensions.resolve("vexessentials/build"));

    WorldDirectoryDeletion.delete(dimensions, dimensions.resolve("vexessentials/farm"));

    assertFalse(Files.exists(dimensions.resolve("vexessentials/farm")));
    assertTrue(Files.isDirectory(sibling));
  }

  @Test
  void rejectsDeletingTheDimensionsRoot() throws IOException {
    Path dimensions = Files.createDirectories(temporaryDirectory.resolve("dimensions"));

    assertThrows(IOException.class, () -> WorldDirectoryDeletion.delete(dimensions, dimensions));
  }
}
