package com.DesktopApplicationClientJava.services;

import java.nio.file.Path;
import java.util.UUID;

public interface ExportService {

    Path exportUsers(Path destinationDir);

    Path exportResources(Path destinationDir);

    Path exportResourceFiles(UUID resourceUuid, Path destinationDir);
}