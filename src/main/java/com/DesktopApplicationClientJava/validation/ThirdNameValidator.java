package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;

public final class ThirdNameValidator {
  public static final String FIELD_NAME = "Отчество";
  public static final int MIN_LENGTH = 2;

  private ThirdNameValidator() {}

  public static List<Validator<String>> validators() {
    return List.of(
        StringLengthValidator.atLeast(MIN_LENGTH, ErrorMessages.MinLength(FIELD_NAME, MIN_LENGTH)));
  }

  /**
   * Thin contract over the stock FormsFX setup above: runs a {@link StringField} and returns the
   * first error message, or {@code null} when valid. A missing third name is valid — the field is
   * optional — but a whitespace-only value is malformed.
   */
  public static String validate(String input) {
    if (input == null || input.isEmpty()) {
      return null;
    }
    if (input.isBlank()) {
      return ErrorMessages.IncorrectFormat;
    }
    StringField field = Field.ofStringType(input).validate(toArray(validators()));
    return field.isValid() ? null : field.getErrorMessages().get(0);
  }

  @SuppressWarnings("unchecked")
  private static Validator<String>[] toArray(List<Validator<String>> validators) {
    return validators.toArray(new Validator[0]);
  }
}
