package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;
import javafx.beans.value.ObservableValue;
import net.synedra.validatorfx.Check;
import net.synedra.validatorfx.Validator;

public final class ThirdNameValidator {
  public static final String FIELD_NAME = "Отчество";
  private static final String KEY = "value";

  private ThirdNameValidator() {}

  public static String validate(String input) {
    if (ValidatorHelpers.isEmpty(input)) return ErrorMessages.Required;
    return null;
  }

  public static void check(Check.Context context) {
    String error = validate(context.get(KEY));
    if (error != null) {
      context.error(error);
    }
  }

  public static Check createCheck(Validator validator, ObservableValue<String> property) {
    return validator.createCheck().dependsOn(KEY, property).withMethod(ThirdNameValidator::check);
  }
}
