package com.DesktopApplicationClientJava.services.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResourceFilter {
  private final String title;
  private final String description;

  public static ResourceFilter empty() {
    return new ResourceFilter(null, null);
  }
}
