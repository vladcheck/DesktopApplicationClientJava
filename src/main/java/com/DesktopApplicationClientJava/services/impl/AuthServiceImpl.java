package com.DesktopApplicationClientJava.services.impl;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.Api.ApiException;
import com.DesktopApplicationClientJava.Api.Dto.request.Auth.LoginRequest;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.mapper.UserMapper;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.session.Session;
import java.io.IOException;

public class AuthServiceImpl implements AuthService {

  private final ApiClient apiClient;
  private final Session session;

  public AuthServiceImpl(ApiClient apiClient, Session session) {
    this.apiClient = apiClient;
    this.session = session;
  }

  @Override
  public User login(String email, String password) {
    try {
      apiClient.login(new LoginRequest(email, password));
      User user = UserMapper.INSTANCE.toEntity(apiClient.getMe());
      session.setCurrentUser(user);
      return user;
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  @Override
  public void logout() {
    try {
      apiClient.logout();
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    } finally {
      session.clear();
    }
  }

  @Override
  public User getCurrentUser() {
    return session.getCurrentUser();
  }

  @Override
  public boolean hasRole(Role required) {
    User current = session.getCurrentUser();
    return current != null
        && current.getRole() != null
        && level(current.getRole()) >= level(required);
  }

  @Override
  public boolean isAdmin() {
    return hasRole(Role.ADMIN);
  }

  @Override
  public boolean isModerator() {
    return hasRole(Role.MODER);
  }

  private static int level(Role role) {
    return switch (role) {
      case USER -> 0;
      case MODER -> 1;
      case ADMIN -> 2;
    };
  }
}
