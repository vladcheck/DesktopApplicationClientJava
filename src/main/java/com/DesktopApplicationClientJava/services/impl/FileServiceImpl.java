package com.DesktopApplicationClientJava.services.impl;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.Api.ApiException;
import com.DesktopApplicationClientJava.Api.Dto.response.FileDto;
import com.DesktopApplicationClientJava.entities.FileInfo;
import com.DesktopApplicationClientJava.services.FileService;
import com.DesktopApplicationClientJava.services.ServiceException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class FileServiceImpl implements FileService {

    private final ApiClient apiClient;

    public FileServiceImpl(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public FileInfo uploadFile(UUID resourceId, Path file) {
        try {
            FileDto dto = apiClient.uploadFile(resourceId, file);
            return new FileInfo(dto.uuid(), dto.name(), dto.contentType(), dto.size());
        } catch (ApiException e) {
            throw new ServiceException(e.getMessage(), e.getStatus());
        } catch (IOException | InterruptedException e) {
            throw new ServiceException("Ошибка соединения с сервером");
        }
    }

    @Override
    public Path downloadFile(UUID resourceId, UUID fileId, Path destinationDir) {
        try {
            return apiClient.downloadFile(resourceId, fileId, destinationDir);
        } catch (ApiException e) {
            throw new ServiceException(e.getMessage(), e.getStatus());
        } catch (IOException | InterruptedException e) {
            throw new ServiceException("Ошибка соединения с сервером");
        }
    }

    @Override
    public void deleteFile(UUID resourceId, UUID fileId) {
        try {
             apiClient.deleteFile(resourceId, fileId);
        } catch (ApiException e) {
            throw new ServiceException(e.getMessage(), e.getStatus());
        } catch (IOException | InterruptedException e) {
            throw new ServiceException("Ошибка соединения с сервером");
        }
    }
}
