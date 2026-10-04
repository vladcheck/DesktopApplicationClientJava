package com.DesktopApplicationClientJava.services;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.services.impl.*;
import com.DesktopApplicationClientJava.session.Session;

public class ServiceRegistry {
  private final AuthService authService;
  private final UsersService usersService;
  private final ResourceService resourceService;
  private final FileService fileService;
  private final ExportService exportService;

  public ServiceRegistry(ApiClient apiClient, Session session) {
    this.authService = new AuthServiceImpl(apiClient, session);
    this.usersService = new UsersServiceImpl(apiClient);
    this.resourceService = new ResourceServiceImpl(apiClient);
    this.fileService = new FileServiceImpl(apiClient);
    this.exportService = new ExportServiceImpl(apiClient);
  }

  public AuthService getAuthService() {
    return authService;
  }

  public ExportService getExportService() {
    return exportService;
  }

  public FileService getFileService() {
    return fileService;
  }

  public ResourceService getResourceService() {
    return resourceService;
  }

  public UsersService getUsersService() {
    return usersService;
  }
}
