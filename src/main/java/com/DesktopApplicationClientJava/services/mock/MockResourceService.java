package com.DesktopApplicationClientJava.services.mock;

import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.filter.ResourceFilter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
  public List<Resource> search(ResourceFilter filter, long offset, long count,
                               String sortBy, String sortDir) {
    // 1. Сначала фильтрация (как в текущей 3-аргументной версии)
    List<Resource> filtered = store.getResources().values().stream()
            .filter(r -> matches(r, filter))
            .collect(Collectors.toList());

    // 2. Сортировка по sortBy/sortDir
    Comparator<Resource> comparator = switch (sortBy == null ? "createdAt" : sortBy) {
      case "title" -> Comparator.comparing(
              r -> r.getTitle() == null ? "" : r.getTitle(),
              String.CASE_INSENSITIVE_ORDER);
      default -> Comparator.comparing(
              Resource::getCreatedAt,
              Comparator.nullsLast(Comparator.naturalOrder()));
    };
    if ("desc".equalsIgnoreCase(sortDir)) {
      comparator = comparator.reversed();
    }
    filtered.sort(comparator);

    // 3. Пагинация
    int from = (int) Math.max(0, offset);
    int to = (int) Math.min(filtered.size(), from + count);
    if (from >= filtered.size()) return List.of();
    return List.copyOf(filtered.subList(from, to));
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
