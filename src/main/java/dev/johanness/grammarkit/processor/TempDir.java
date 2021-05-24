package dev.johanness.grammarkit.processor;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class TempDir implements AutoCloseable {
  private final @NotNull Path path;

  public TempDir() throws IOException {
    this.path = Files.createTempDirectory("grammar-kit");
  }

  public @NotNull Path getPath() {
    return path;
  }

  @Override
  public void close() throws IOException {
    Files.walkFileTree(path, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null)
          throw exc;
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
