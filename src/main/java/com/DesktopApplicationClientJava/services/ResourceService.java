package com.DesktopApplicationClientJava.services;

import com.DesktopApplicationClientJava.entities.Resource;
import com.DesktopApplicationClientJava.services.filter.ResourceFilter;
import java.util.List;
import java.util.UUID;

public interface ResourceService {
  List<Resource> search(ResourceFilter filter, long offset, long count);

  Resource create(String title, String description);

  Resource getById(UUID resourceId);

  Resource update(UUID resourceId, String title, String description);

  void delete(UUID resourceId);
}
