package com.DesktopApplicationClientJava.services;

import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.data.RegisterData;
import com.DesktopApplicationClientJava.services.data.UpdateUserData;
import com.DesktopApplicationClientJava.services.filter.UserFilter;
import java.util.List;
import java.util.UUID;

public interface UsersService {

  /** Поиск с фильтрами. Пустые параметры = без фильтра. */
  List<User> search(UserFilter filter, long offset, long count);

  User getById(UUID uuid);

  User create(RegisterData data);

  User update(UUID uuid, UpdateUserData data);

  void delete(UUID uuid);
}
