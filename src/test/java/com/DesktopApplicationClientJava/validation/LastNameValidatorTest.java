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
@Feature("LastNameValidator")
@Story("Валидация фамилии")
@Tag("role:none")
@DisplayName("Проверка фамилии")
public class LastNameValidatorTest {
  private static final String FIELD_NAME = LastNameValidator.FIELD_NAME;

  @Test
  @DisplayName("Пустая фамилия отклоняется")
  void emptyLastNameIsRejected() {
    assertEquals(ErrorMessages.Required, LastNameValidator.validate(""));
  }

  @Test
  @DisplayName("Короткая фамилия отклоняется")
  void shortLastNameIsRejected() {
    assertEquals(
        ErrorMessages.MinLength(FIELD_NAME, LastNameValidator.MIN_LENGTH),
        LastNameValidator.validate("А"));
  }

  @Test
  @DisplayName("Корректная фамилия принимается")
  void validLastNameIsAccepted() {
    assertNull(LastNameValidator.validate("Антонов"));
  }
}
