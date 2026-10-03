package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.AbstractValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;

public class LastNameValidator extends AbstractValidator {
  public static final String FIELD_NAME = "Фамилия";
  public static final int MIN_LENGTH = 2;

  public String isValid(String input) {
    if (ValidatorHelpers.isEmpty(input)) return ErrorMessages.Required;
    if (ValidatorHelpers.isShorterThan(input, MIN_LENGTH))
      return ErrorMessages.MinLength(FIELD_NAME, MIN_LENGTH);
    return null;
  }
}
