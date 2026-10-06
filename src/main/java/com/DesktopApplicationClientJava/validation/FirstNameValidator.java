package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;
import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;

public final class FirstNameValidator {
  public static final String FIELD_NAME = "Имя";
  public static final int MIN_LENGTH = 2;

  private FirstNameValidator() {}

  public static String validate(String input) {
    if (ValidatorHelpers.isEmpty(input)) return ErrorMessages.Required;
    if (ValidatorHelpers.isShorterThan(input, MIN_LENGTH))
      return ErrorMessages.MinLength(FIELD_NAME, MIN_LENGTH);
    return null;
  }

  public static List<Validator<String>> validators() {
    return List.of(
        CustomValidator.forPredicate(
            input -> !ValidatorHelpers.isEmpty(input), ErrorMessages.Required),
        StringLengthValidator.atLeast(MIN_LENGTH, ErrorMessages.MinLength(FIELD_NAME, MIN_LENGTH)));
  }
}
