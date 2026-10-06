package com.DesktopApplicationClientJava.services;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.services.impl.*;
import com.DesktopApplicationClientJava.services.mock.MockAuthService;
import com.DesktopApplicationClientJava.services.mock.MockExportService;
import com.DesktopApplicationClientJava.services.mock.MockFileService;
import com.DesktopApplicationClientJava.services.mock.MockResourceService;
import com.DesktopApplicationClientJava.services.mock.MockStore;
import com.DesktopApplicationClientJava.services.mock.MockUsersService;
import com.DesktopApplicationClientJava.session.Session;
import lombok.Getter;

@Getter
public class ServiceRegistry {
  private final AuthService authService;
  private final UsersService usersService;
  private final ResourceService resourceService;
  private final FileService fileService;
  private final ExportService exportService;

  public ServiceRegistry(ApiClient apiClient, Session session) {
    this(
        new AuthServiceImpl(apiClient, session),
        new UsersServiceImpl(apiClient),
        new ResourceServiceImpl(apiClient),
        new FileServiceImpl(apiClient),
        new ExportServiceImpl(apiClient));
  }

  public ServiceRegistry(
      AuthService authService,
      UsersService usersService,
      ResourceService resourceService,
      FileService fileService,
      ExportService exportService) {
    this.authService = authService;
    this.usersService = usersService;
    this.resourceService = resourceService;
    this.fileService = fileService;
    this.exportService = exportService;
  }

  /** In-memory services for UI testing without a backend (see MOCK_DATA in Program). */
  public static ServiceRegistry mock(Session session) {
    MockStore store = new MockStore();
    return new ServiceRegistry(
        new MockAuthService(store, session),
        new MockUsersService(store),
        new MockResourceService(store),
        new MockFileService(store),
        new MockExportService());
  }
}
