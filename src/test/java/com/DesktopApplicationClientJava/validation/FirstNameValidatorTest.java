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
@Feature("FirstNameValidator")
@Story("Валидация имени")
@Tag("role:none")
@DisplayName("Проверка имени")
public class FirstNameValidatorTest {
  private static final String FIELD_NAME = FirstNameValidator.FIELD_NAME;

  @Test
  @DisplayName("Пустое имя отклоняется")
  void emptyFirstNameIsRejected() {
    assertEquals(ErrorMessages.Required, FirstNameValidator.validate(""));
  }

  @Test
  @DisplayName("Короткое имя отклоняется")
  void shortFirstNameIsRejected() {
    assertEquals(
        ErrorMessages.MinLength(FIELD_NAME, FirstNameValidator.MIN_LENGTH),
        FirstNameValidator.validate("А"));
  }

  @Test
  @DisplayName("Корректное имя принимается")
  void validFirstNameIsAccepted() {
    assertNull(FirstNameValidator.validate("Антон"));
  }
}
