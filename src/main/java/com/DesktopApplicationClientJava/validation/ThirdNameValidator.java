package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;
import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;

public final class ThirdNameValidator {
  public static final String FIELD_NAME = "Отчество";

  private ThirdNameValidator() {}

  public static String validate(String input) {
    if (ValidatorHelpers.isEmpty(input)) return ErrorMessages.Required;
    return null;
  }

  public static List<Validator<String>> validators() {
    return List.of(
        CustomValidator.forPredicate(
            input -> !ValidatorHelpers.isEmpty(input), ErrorMessages.Required));
  }
}
