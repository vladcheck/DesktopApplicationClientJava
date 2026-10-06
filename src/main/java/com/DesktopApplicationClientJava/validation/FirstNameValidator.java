package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;

public final class FirstNameValidator {
  public static final String FIELD_NAME = "Имя";
  public static final int MIN_LENGTH = 2;

  private FirstNameValidator() {}

  public static List<Validator<String>> validators() {
    return List.of(
        StringLengthValidator.atLeast(MIN_LENGTH, ErrorMessages.MinLength(FIELD_NAME, MIN_LENGTH)));
  }

  /**
   * Thin contract over the stock FormsFX validators above: runs a {@link StringField} and returns
   * the first error message, or {@code null} when valid.
   */
  public static String validate(String input) {
    String value = input == null ? "" : input;
    if (value.isBlank()) {
      // Stock FormsFX required-check is empty-based; preserve blank-means-required semantics.
      return ErrorMessages.Required;
    }
    StringField field =
        Field.ofStringType(value).required(ErrorMessages.Required).validate(toArray(validators()));
    return field.isValid() ? null : field.getErrorMessages().get(0);
  }

  @SuppressWarnings("unchecked")
  private static Validator<String>[] toArray(List<Validator<String>> validators) {
    return validators.toArray(new Validator[0]);
  }
}
