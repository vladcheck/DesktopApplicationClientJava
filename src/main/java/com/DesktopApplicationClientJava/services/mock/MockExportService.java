package com.DesktopApplicationClientJava.services.mock;

import com.DesktopApplicationClientJava.services.ExportService;
import com.DesktopApplicationClientJava.services.ServiceException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** In-memory {@link ExportService}. Writes small placeholder files. */
public class MockExportService implements ExportService, MockService {

  @Override
  public Path exportUsers(Path destinationDir) {
    return write(destinationDir, "export-users.txt", "mock users export\n");
  }

  @Override
  public Path exportResources(Path destinationDir) {
    return write(destinationDir, "export-resources.txt", "mock resources export\n");
  }

  @Override
  public Path exportResourceFiles(UUID resourceUuid, Path destinationDir) {
    return write(destinationDir, "export-resource-" + resourceUuid + ".txt", "mock files export\n");
  }

  private static Path write(Path destinationDir, String fileName, String content) {
    try {
      Files.createDirectories(destinationDir);
      Path saved = destinationDir.resolve(fileName);
      Files.write(saved, content.getBytes(StandardCharsets.UTF_8));
      return saved;
    } catch (IOException e) {
      throw new ServiceException("Ошибка записи файла");
    }
  }
}
