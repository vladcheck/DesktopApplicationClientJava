package com.DesktopApplicationClientJava.services.impl;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.Api.ApiException;
import com.DesktopApplicationClientJava.services.ExportService;
import com.DesktopApplicationClientJava.services.ServiceException;

import java.nio.file.Path;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;

public class ExportServiceImpl implements ExportService {

    private final ApiClient apiClient;

    public ExportServiceImpl(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Path exportUsers(Path destinationDir) {
        return saveTo(destinationDir, "users.xlsx", apiClient::exportUsers);
    }

    @Override
    public Path exportResources(Path destinationDir) {
        return saveTo(destinationDir, "resources.xlsx", apiClient::exportResources);
    }

    @Override
    public Path exportResourceFiles(UUID resourceUuid, Path destinationDir) {
        String filename = "files-" + resourceUuid + ".xlsx";
        return saveTo(destinationDir, filename, () -> apiClient.exportResourceFiles(resourceUuid));
    }

    @FunctionalInterface
    private interface ByteSupplier {
        byte[] get() throws IOException, InterruptedException;
    }

    private Path saveTo(Path destinationDir, String filename, ByteSupplier supplier) {
        try {
            byte[] data = supplier.get();
            Files.createDirectories(destinationDir);
            Path target = destinationDir.resolve(filename);
            Files.write(target, data);
            return target;
        } catch (ApiException e) {
            throw new ServiceException(e.getMessage(), e.getStatus());
        } catch (IOException | InterruptedException e) {
            throw new ServiceException("Ошибка экспорта: " + e.getMessage());
        }
    }
}
