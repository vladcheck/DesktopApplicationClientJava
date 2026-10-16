package com.DesktopApplicationClientJava.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Проверка ввода")
@Feature("PasswordValidator")
@Story("Валидация пароля")
@Tag("role:none")
@DisplayName("Проверка пароля")
public class PasswordValidatorTest {
  private static final String FIELD_NAME = PasswordValidator.FIELD_NAME;

  @Test
  @DisplayName("Пустой пароль отклоняется")
  void emptyPasswordIsRejected() {
    assertEquals(ErrorMessages.Required, PasswordValidator.validate(""));
  }

  @Test
  @DisplayName("Короткий пароль отклоняется")
  void shortPasswordIsRejected() {
    assertEquals(
        ErrorMessages.MinLength(FIELD_NAME, PasswordValidator.MIN_LENGTH),
        PasswordValidator.validate("short"));
  }

  @Test
  @DisplayName("Пароль из восьми символов принимается")
  void exactlyEightCharPasswordIsAccepted() {
    assertNull(PasswordValidator.validate("12345678"));
  }
}
