package com.DesktopApplicationClientJava.Api.handlers;

import com.DesktopApplicationClientJava.Api.HttpController;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public abstract class Handler {
  protected final HttpController httpController;

  protected Handler(HttpController httpController) {
    this.httpController = httpController;
  }

  public String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
