package com.DesktopApplicationClientJava.ui.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.DesktopApplicationClientJava.services.ServiceException;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the login flow: real AuthService with a real ApiClient, no mocks. Requires a
 * running backend at {@code localhost:8080}; otherwise tests are aborted.
 *
 * <p>Excluded from the default build and CI (see surefire {@code excludedGroups} in pom.xml). Run
 * explicitly: {@code mvn test -Dgroups=integration -DexcludedGroups=} ({@code @Tag} inherited from
 * IntegrationTestCase).
 */
@Epic("Аутентификация")
@Feature("№1 Вход")
@Story("Страница входа")
@Tag("role:admin")
@DisplayName("Вход через живой API")
class LoginIntegrationTest extends IntegrationTestCase {

  @Test
  @DisplayName("Чужие учётные данные отклоняются живым API")
  void invalidCredentialsAreRejectedByRealApi() {
    assertThrows(
        ServiceException.class,
        () -> authService.login("no-such-user@example.com", "wrong-password-123"));
  }
}
