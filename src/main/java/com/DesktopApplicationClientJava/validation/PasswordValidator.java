package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;
import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;

public final class PasswordValidator {
  public static final String FIELD_NAME = "Пароль";
  public static final int MIN_LENGTH = 8;
  public static final int MAX_LENGTH = 64;

  private PasswordValidator() {}

  public static String validate(String input) {
    if (ValidatorHelpers.isEmpty(input)) return ErrorMessages.Required;
    if (ValidatorHelpers.isShorterThan(input, MIN_LENGTH))
      return ErrorMessages.MinLength(FIELD_NAME, MIN_LENGTH);
    if (ValidatorHelpers.isLongerThan(input, MAX_LENGTH))
      return ErrorMessages.MaxLength(FIELD_NAME, MAX_LENGTH);
    return null;
  }

  public static List<Validator<String>> validators() {
    return List.of(
        CustomValidator.forPredicate(
            input -> !ValidatorHelpers.isEmpty(input), ErrorMessages.Required),
        StringLengthValidator.atLeast(MIN_LENGTH, ErrorMessages.MinLength(FIELD_NAME, MIN_LENGTH)),
        StringLengthValidator.upTo(MAX_LENGTH, ErrorMessages.MaxLength(FIELD_NAME, MAX_LENGTH)));
  }
}
