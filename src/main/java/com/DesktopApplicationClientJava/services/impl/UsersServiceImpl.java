package com.DesktopApplicationClientJava.services.impl;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.Api.ApiException;
import com.DesktopApplicationClientJava.Api.Dto.request.Admin.RegisterRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Admin.UpdateUserRequest;
import com.DesktopApplicationClientJava.Api.Dto.request.Admin.UserFilterRequest;
import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.UsersService;
import com.DesktopApplicationClientJava.services.data.RegisterData;
import com.DesktopApplicationClientJava.services.data.UpdateUserData;
import com.DesktopApplicationClientJava.services.filter.UserFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class UsersServiceImpl implements UsersService {
  private final ApiClient apiClient;

  public UsersServiceImpl(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public List<User> search(UserFilter filter, long offset, long count) {
    UserFilterRequest req =
        new UserFilterRequest(
            filter.getEmail(),
            filter.getRole(),
            filter.getFirstName(),
            filter.getLastName(),
            filter.getEnabled(),
            null,
            null);

    try {
      return apiClient.getUsersByFilters(req, offset, count, "createdAt", "desc").stream()
          .map(this::toEntity)
          .toList();
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  @Override
  public User getById(UUID uuid) {
    try {
      return toEntity(apiClient.getUserById(uuid));
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  @Override
  public User create(RegisterData data) {
    RegisterRequest req =
        new RegisterRequest(
            data.getEmail(),
            data.getPassword(),
            data.getRole(),
            data.getFirstName(),
            data.getLastName());
    try {
      return toEntity(apiClient.createUser(req));
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  @Override
  public User update(UUID uuid, UpdateUserData data) {
    UpdateUserRequest req =
        new UpdateUserRequest(
            data.getEmail(),
            data.getEnabled(),
            data.getRole(),
            data.getFirstName(),
            data.getLastName());

    try {
      return toEntity(apiClient.updateUser(uuid, req));
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  @Override
  public void delete(UUID uuid) {
    try {
      apiClient.deleteUser(uuid);
    } catch (ApiException e) {
      throw new ServiceException(e.getMessage(), e.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new ServiceException("Ошибка соединения с сервером");
    }
  }

  private User toEntity(UserDto dto) {
    return new User(
        dto.uuid(), dto.email(), dto.role(), dto.firstName(), dto.lastName(), dto.enabled());
  }
}
