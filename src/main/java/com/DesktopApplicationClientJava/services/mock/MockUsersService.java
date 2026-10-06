package com.DesktopApplicationClientJava.services.mock;

import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.services.UsersService;
import com.DesktopApplicationClientJava.services.data.RegisterData;
import com.DesktopApplicationClientJava.services.data.UpdateUserData;
import com.DesktopApplicationClientJava.services.filter.UserFilter;
import java.util.List;
import java.util.UUID;

/** In-memory {@link UsersService} over {@link MockStore}. */
public class MockUsersService implements UsersService, MockService {

  private final MockStore store;

  public MockUsersService(MockStore store) {
    this.store = store;
  }

  @Override
  public List<User> search(UserFilter filter, long offset, long count) {
    return store.getUsers().values().stream()
        .filter(user -> matches(user, filter))
        .skip(Math.max(0, offset))
        .limit(count <= 0 ? Long.MAX_VALUE : count)
        .toList();
  }

  @Override
  public User getById(UUID uuid) {
    User user = store.getUsers().get(uuid);
    if (user == null) {
      throw new ServiceException("Пользователь не найден", 404);
    }
    return user;
  }

  @Override
  public User create(RegisterData data) {
    boolean duplicate =
        store.getUsers().values().stream()
            .anyMatch(user -> user.getEmail().equalsIgnoreCase(data.getEmail()));
    if (duplicate) {
      throw new ServiceException("Пользователь уже существует", 409);
    }
    User user =
        new User(
            UUID.randomUUID(),
            data.getEmail(),
            data.getRole(),
            data.getFirstName(),
            data.getLastName(),
            true);
    store.getUsers().put(user.getUuid(), user);
    return user;
  }

  @Override
  public User update(UUID uuid, UpdateUserData data) {
    User user = getById(uuid);
    user.setEmail(data.getEmail());
    user.setRole(data.getRole());
    user.setFirstName(data.getFirstName());
    user.setLastName(data.getLastName());
    user.setEnabled(data.getEnabled());
    return user;
  }

  @Override
  public void delete(UUID uuid) {
    if (store.getUsers().remove(uuid) == null) {
      throw new ServiceException("Пользователь не найден", 404);
    }
  }

  private static boolean matches(User user, UserFilter filter) {
    if (filter == null) {
      return true;
    }
    return contains(user.getEmail(), filter.getEmail())
        && (filter.getRole() == null || filter.getRole() == user.getRole())
        && contains(user.getFirstName(), filter.getFirstName())
        && contains(user.getLastName(), filter.getLastName())
        && (filter.getEnabled() == null || filter.getEnabled().equals(user.getEnabled()));
  }

  private static boolean contains(String value, String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    return value != null && value.toLowerCase().contains(query.toLowerCase());
  }
}
