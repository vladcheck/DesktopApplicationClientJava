package com.DesktopApplicationClientJava.Api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ApiConfig {

  private static final Properties PROPS = new Properties();

  static {
    try (InputStream in = ApiConfig.class.getResourceAsStream("/api.properties")) {
      if (in == null) {
        throw new IllegalStateException("api.properties not found on classpath");
      }
      PROPS.load(in);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private ApiConfig() {}

  public static String baseUrl() {
    return required("api.baseUrl");
  }

  public static long connectTimeoutMs() {
    return Long.parseLong(required("api.connectTimeoutMs"));
  }

  public static long readTimeoutMs() {
    return Long.parseLong(required("api.readTimeoutMs"));
  }

  public static String pathLogin() {
    return required("api.path.login");
  }

  public static String pathRefresh() {
    return required("api.path.refresh");
  }

  public static String pathLogout() {
    return required("api.path.logout");
  }

  public static String pathMe() {
    return required("api.path.me");
  }

  public static String pathResource() {
    return required("api.path.resource");
  }

  public static String pathAdmin() {
    return required("api.path.admin");
  }

  public static String pathExportUsers() {
    return required("api.path.export.users");
  }

  public static String pathExportResources() {
    return required("api.path.export.resources");
  }

  /** /api/v1/admin/resource/{uuid}/files/export */
  public static String pathExportResourceFiles(String resourceId) {
    return pathAdmin() + "/resource/" + resourceId + "/files/export";
  }

  /** /api/v1/resource/{uuid}/file */
  public static String pathResourceFiles(String resourceId) {
    return pathResource() + "/" + resourceId + "/file";
  }

  /** /api/v1/resource/{uuid}/file/{fileId} */
  public static String pathResourceFile(String resourceId, String fileId) {
    return pathResource() + "/" + resourceId + "/file/" + fileId;
  }

  private static String required(String key) {
    String value = PROPS.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing config property: " + key);
    }
    return value;
  }
}
