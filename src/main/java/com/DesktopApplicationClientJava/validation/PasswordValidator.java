package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.AbstractValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;

public class PasswordValidator extends AbstractValidator {
  public static final String FIELD_NAME = "Пароль";
  public static final int MIN_LENGTH = 8;
  public static final int MAX_LENGTH = 64;

  public String isValid(String input) {
    if (ValidatorHelpers.isEmpty(input)) return ErrorMessages.Required;
    if (ValidatorHelpers.isShorterThan(input, MIN_LENGTH))
      return ErrorMessages.MinLength(FIELD_NAME, MIN_LENGTH);
    if (ValidatorHelpers.isLongerThan(input, MAX_LENGTH))
      return ErrorMessages.MaxLength(FIELD_NAME, MAX_LENGTH);
    return null;
  }
}
