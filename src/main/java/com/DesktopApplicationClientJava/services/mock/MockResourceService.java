package com.DesktopApplicationClientJava.services.mock;

import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.filter.ResourceFilter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** In-memory {@link ResourceService} over {@link MockStore}. */
public class MockResourceService implements ResourceService, MockService {

  private final MockStore store;

  public MockResourceService(MockStore store) {
    this.store = store;
  }

  @Override
  public List<Resource> search(ResourceFilter filter, long offset, long count) {
    return store.getResources().values().stream()
        .filter(resource -> matches(resource, filter))
        .skip(Math.max(0, offset))
        .limit(count <= 0 ? Long.MAX_VALUE : count)
        .toList();
  }

  @Override
  public Resource create(String title, String description) {
    LocalDateTime now = LocalDateTime.now();
    Resource resource =
        new Resource(UUID.randomUUID(), title, description, now, now, new ArrayList<>());
    store.getResources().put(resource.getUuid(), resource);
    return resource;
  }

  @Override
  public Resource getById(UUID resourceId) {
    Resource resource = store.getResources().get(resourceId);
    if (resource == null) {
      throw new ServiceException("Ресурс не найден", 404);
    }
    return resource;
  }

  @Override
  public Resource update(UUID resourceId, String title, String description) {
    Resource resource = getById(resourceId);
    resource.setTitle(title);
    resource.setDescription(description);
    resource.setUpdatedAt(LocalDateTime.now());
    return resource;
  }

  @Override
  public void delete(UUID resourceId) {
    if (store.getResources().remove(resourceId) == null) {
      throw new ServiceException("Ресурс не найден", 404);
    }
  }

  private static boolean matches(Resource resource, ResourceFilter filter) {
    if (filter == null) {
      return true;
    }
    return contains(resource.getTitle(), filter.getTitle())
        && contains(resource.getDescription(), filter.getDescription());
  }

  private static boolean contains(String value, String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    return value != null && value.toLowerCase().contains(query.toLowerCase());
  }
}
