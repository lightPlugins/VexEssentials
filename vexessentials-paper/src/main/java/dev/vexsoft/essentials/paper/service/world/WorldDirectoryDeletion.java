package dev.vexsoft.essentials.paper.service.world;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/** Deletes one validated dimension tree without following symbolic links. */
final class WorldDirectoryDeletion {

  private WorldDirectoryDeletion() {
  }

  static void delete(final Path dimensionsDirectory, final Path target) throws IOException {
    Path dimensions = Objects.requireNonNull(dimensionsDirectory, "dimensionsDirectory")
        .toAbsolutePath()
        .normalize();
    Path checkedTarget = Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
    rejectUnsafePath(dimensions, checkedTarget);
    if (!Files.exists(checkedTarget)) {
      return;
    }
    Files.walkFileTree(checkedTarget, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(
          final Path file,
          final BasicFileAttributes attributes
      ) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(
          final Path directory,
          final IOException exception
      ) throws IOException {
        if (exception != null) {
          throw exception;
        }
        Files.delete(directory);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private static void rejectUnsafePath(final Path dimensions, final Path target) throws IOException {
    if (!target.startsWith(dimensions) || target.equals(dimensions)) {
      throw new IOException("Refusing to delete a path outside the dimensions directory");
    }
    Path cursor = dimensions;
    if (Files.isSymbolicLink(cursor)) {
      throw new IOException("The dimensions directory is a symbolic link");
    }
    for (Path segment : dimensions.relativize(target)) {
      cursor = cursor.resolve(segment);
      if (Files.isSymbolicLink(cursor)) {
        throw new IOException("World dimension path contains a symbolic link: " + cursor);
      }
    }
  }
}
