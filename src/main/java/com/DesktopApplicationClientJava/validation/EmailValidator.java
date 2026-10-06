package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;
import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;

public final class EmailValidator {
  public static final String FIELD_NAME = "Почта";
  public static final int MIN_EMAIL_LENGTH = 5;
  public static final int MAX_EMAIL_LENGTH = 100;

  private EmailValidator() {}

  public static String validate(String input) {
    if (ValidatorHelpers.isEmpty(input)) return ErrorMessages.Required;
    if (ValidatorHelpers.isShorterThan(input, MIN_EMAIL_LENGTH))
      return ErrorMessages.MinLength(FIELD_NAME, MIN_EMAIL_LENGTH);
    if (ValidatorHelpers.isLongerThan(input, MAX_EMAIL_LENGTH))
      return ErrorMessages.MaxLength(FIELD_NAME, MAX_EMAIL_LENGTH);
    if (!ValidatorHelpers.includes(input, "@")) return ErrorMessages.IncorrectFormat;
    return null;
  }

  public static List<Validator<String>> validators() {
    return List.of(
        CustomValidator.forPredicate(
            input -> !ValidatorHelpers.isEmpty(input), ErrorMessages.Required),
        StringLengthValidator.atLeast(
            MIN_EMAIL_LENGTH, ErrorMessages.MinLength(FIELD_NAME, MIN_EMAIL_LENGTH)),
        StringLengthValidator.upTo(
            MAX_EMAIL_LENGTH, ErrorMessages.MaxLength(FIELD_NAME, MAX_EMAIL_LENGTH)),
        CustomValidator.forPredicate(
            EmailValidator::hasValidFormat, ErrorMessages.IncorrectFormat));
  }

  private static boolean hasValidFormat(String input) {
    return ValidatorHelpers.isEmpty(input) || ValidatorHelpers.includes(input, "@");
  }
}
