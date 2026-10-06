package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;
import javafx.beans.value.ObservableValue;
import net.synedra.validatorfx.Check;
import net.synedra.validatorfx.Validator;

public final class PasswordValidator {
  public static final String FIELD_NAME = "Пароль";
  public static final int MIN_LENGTH = 8;
  public static final int MAX_LENGTH = 64;
  private static final String KEY = "value";

  private PasswordValidator() {}

  public static String validate(String input) {
    if (ValidatorHelpers.isEmpty(input)) return ErrorMessages.Required;
    if (ValidatorHelpers.isShorterThan(input, MIN_LENGTH))
      return ErrorMessages.MinLength(FIELD_NAME, MIN_LENGTH);
    if (ValidatorHelpers.isLongerThan(input, MAX_LENGTH))
      return ErrorMessages.MaxLength(FIELD_NAME, MAX_LENGTH);
    return null;
  }

  public static void check(Check.Context context) {
    String error = validate(context.get(KEY));
    if (error != null) {
      context.error(error);
    }
  }

  public static Check createCheck(Validator validator, ObservableValue<String> property) {
    return validator.createCheck().dependsOn(KEY, property).withMethod(PasswordValidator::check);
  }
}
