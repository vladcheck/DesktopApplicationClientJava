package com.DesktopApplicationClientJava.services;

import com.DesktopApplicationClientJava.entities.FileInfo;
import java.nio.file.Path;
import java.util.UUID;

public interface FileService {
  FileInfo uploadFile(UUID resourceId, Path file);

  Path downloadFile(UUID resourceId, UUID fileId, Path destinationDir);

  void deleteFile(UUID resourceId, UUID fileId);
}
