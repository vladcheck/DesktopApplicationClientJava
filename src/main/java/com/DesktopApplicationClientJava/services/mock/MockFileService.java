package com.DesktopApplicationClientJava.services.mock;

import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.FileService;
import com.DesktopApplicationClientJava.services.ServiceException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** In-memory {@link FileService}. Downloads produce a real file with placeholder content. */
public class MockFileService implements FileService, MockService {

  private final MockStore store;

  public MockFileService(MockStore store) {
    this.store = store;
  }

  @Override
  public FileInfo uploadFile(UUID resourceId, Path file) {
    Resource resource = findResource(resourceId);
    try {
      byte[] bytes = Files.readAllBytes(file);
      String contentType = Files.probeContentType(file);
      FileInfo info =
          new FileInfo(
              UUID.randomUUID(),
              file.getFileName().toString(),
              contentType == null ? "application/octet-stream" : contentType,
              (long) bytes.length);
      resource.getFiles().add(info);
      store.getFileBytes().put(info.getUuid(), bytes);
      return info;
    } catch (IOException e) {
      throw new ServiceException("Ошибка чтения файла");
    }
  }

  @Override
  public Path downloadFile(UUID resourceId, UUID fileId, Path destinationDir) {
    Resource resource = findResource(resourceId);
    FileInfo info =
        resource.getFiles().stream()
            .filter(file -> file.getUuid().equals(fileId))
            .findFirst()
            .orElseThrow(() -> new ServiceException("Файл не найден", 404));
    byte[] bytes =
        store
            .getFileBytes()
            .getOrDefault(
                fileId, ("mock content of " + info.getName()).getBytes(StandardCharsets.UTF_8));
    try {
      Files.createDirectories(destinationDir);
      Path saved = destinationDir.resolve(info.getName());
      Files.write(saved, bytes);
      return saved;
    } catch (IOException e) {
      throw new ServiceException("Ошибка записи файла");
    }
  }

  @Override
  public void deleteFile(UUID resourceId, UUID fileId) {
    Resource resource = findResource(resourceId);
    boolean removed = resource.getFiles().removeIf(file -> file.getUuid().equals(fileId));
    if (!removed) {
      throw new ServiceException("Файл не найден", 404);
    }
    store.getFileBytes().remove(fileId);
  }

  private Resource findResource(UUID resourceId) {
    Resource resource = store.getResources().get(resourceId);
    if (resource == null) {
      throw new ServiceException("Ресурс не найден", 404);
    }
    return resource;
  }
}
