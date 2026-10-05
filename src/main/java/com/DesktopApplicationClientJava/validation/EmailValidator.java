package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;
import javafx.beans.value.ObservableValue;
import net.synedra.validatorfx.Check;
import net.synedra.validatorfx.Validator;

public final class EmailValidator {
  public static final String FIELD_NAME = "Почта";
  public static final int MIN_EMAIL_LENGTH = 5;
  public static final int MAX_EMAIL_LENGTH = 100;
  private static final String KEY = "value";

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

  public static void check(Check.Context context) {
    String error = validate(context.get(KEY));
    if (error != null) {
      context.error(error);
    }
  }

  public static Check createCheck(Validator validator, ObservableValue<String> property) {
    return validator.createCheck().dependsOn(KEY, property).withMethod(EmailValidator::check);
  }
}
