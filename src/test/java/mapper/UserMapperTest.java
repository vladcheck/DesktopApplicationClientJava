package mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.DesktopApplicationClientJava.Api.Dto.response.UserDto;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.mapper.UserMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Отображение данных")
@Feature("UserMapper")
@Story("Преобразование пользователя в сущность")
@Tag("role:none")
@DisplayName("Преобразование пользователя")
class UserMapperTest {

  @Test
  @DisplayName("Поля бэкенда преобразуются в сущность пользователя")
  void mapsBackendLowercaseNamesToCamelCaseFields() {
    UUID uuid = UUID.randomUUID();
    UserDto dto =
        new UserDto(
            uuid,
            "user@example.com",
            Role.USER,
            "Иван",
            "Петров",
            true,
            LocalDateTime.parse("2026-01-02T03:04:05"),
            LocalDateTime.parse("2026-01-03T04:05:06"));

    User user = UserMapper.INSTANCE.toEntity(dto);

    assertEquals(uuid, user.getUuid());
    assertEquals("user@example.com", user.getEmail());
    assertEquals(Role.USER, user.getRole());
    assertEquals("Иван", user.getFirstName());
    assertEquals("Петров", user.getLastName());
    assertEquals(true, user.getEnabled());
  }
}
