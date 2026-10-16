package com.DesktopApplicationClientJava.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Проверка ввода")
@Feature("Вход (email + пароль)")
@Tag("role:none")
@DisplayName("Проверка адреса почты")
public class EmailValidatorTest {
  private static final String FIELD_NAME = EmailValidator.FIELD_NAME;

  @Test
  void emptyEmailIsRejected() {
    assertEquals(ErrorMessages.Required, EmailValidator.validate(""));
  }

  @Test
  void blankEmailIsRejected() {
    assertEquals(ErrorMessages.Required, EmailValidator.validate("   "));
  }

  @Test
  void shortEmailIsRejected() {
    assertEquals(
        ErrorMessages.MinLength(FIELD_NAME, EmailValidator.MIN_EMAIL_LENGTH),
        EmailValidator.validate("a@b"));
  }

  @Test
  void longEmailIsRejected() {
    String e = "a".repeat(95) + "@x.com";
    assertEquals(
        ErrorMessages.MaxLength(FIELD_NAME, EmailValidator.MAX_EMAIL_LENGTH),
        EmailValidator.validate(e));
  }

  @Test
  void emailWithoutAtIsRejected() {
    assertEquals(ErrorMessages.IncorrectFormat, EmailValidator.validate("nope.com"));
  }

  @Test
  @DisplayName("Корректный адрес принимается")
  void validEmailIsAccepted() {
    assertNull(EmailValidator.validate("test@example.com"));
  }
}
