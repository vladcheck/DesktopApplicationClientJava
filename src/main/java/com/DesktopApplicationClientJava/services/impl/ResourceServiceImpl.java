package com.DesktopApplicationClientJava.services.impl;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.Api.ApiException;
import com.DesktopApplicationClientJava.Api.Dto.request.Resource.CreateResourceRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Resource.ResourceFilterRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Resource.UpdateResourceRequest;
import com.DesktopApplicationClientJava.Api.Dto.response.ResourceDto;
import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.filter.ResourceFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ResourceServiceImpl implements ResourceService {

  private final ApiClient apiClient;

  public ResourceServiceImpl(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public List<Resource> search(ResourceFilter filter, long offset, long count) {
    ResourceFilterRequest req =
        new ResourceFilterRequest(filter.getTitle(), filter.getDescription(), null, null);
    try {
      return apiClient.findResourcesByFilters(req, offset, count, "createdAt", "desc").stream()
          .map(this::toEntity)
          .toList();
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  @Override
  public Resource create(String title, String description) {
    try {
      return toEntity(apiClient.createResource(new CreateResourceRequest(title, description)));
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  @Override
  public Resource getById(UUID resourceId) {
    try {
      return toEntity(apiClient.findResourceById(resourceId));
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  @Override
  public Resource update(UUID resourceId, String title, String description) {
    UpdateResourceRequest req = new UpdateResourceRequest(title, description);
    try {
      return toEntity(apiClient.updateResource(resourceId, req));
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  @Override
  public void delete(UUID resourceId) {
    try {
      apiClient.deleteResource(resourceId);
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  private Resource toEntity(ResourceDto dto) {
    return new Resource(
        dto.uuid(), dto.title(), dto.description(), dto.createdAt(), dto.updatedAt());
  }
}
