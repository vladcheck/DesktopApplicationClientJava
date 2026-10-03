package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.AbstractValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;

public class EmailValidator extends AbstractValidator {
  public static final String FIELD_NAME = "Почта";
  public static final int MIN_EMAIL_LENGTH = 5;
  public static final int MAX_EMAIL_LENGTH = 100;

  public String isValid(String input) {
    if (ValidatorHelpers.isEmpty(input)) return ErrorMessages.Required;
    if (ValidatorHelpers.isShorterThan(input, MIN_EMAIL_LENGTH))
      return ErrorMessages.MinLength(FIELD_NAME, MIN_EMAIL_LENGTH);
    if (ValidatorHelpers.isLongerThan(input, MAX_EMAIL_LENGTH))
      return ErrorMessages.MaxLength(FIELD_NAME, MAX_EMAIL_LENGTH);
    if (!ValidatorHelpers.includes(input, "@")) return ErrorMessages.IncorrectFormat;
    return null;
  }
}
