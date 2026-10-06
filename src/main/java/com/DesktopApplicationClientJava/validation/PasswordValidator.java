package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.PasswordField;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;

public final class PasswordValidator {
  public static final String FIELD_NAME = "Пароль";
  public static final int MIN_LENGTH = 8;
  public static final int MAX_LENGTH = 64;

  private PasswordValidator() {}

  public static List<Validator<String>> validators() {
    return List.of(
        StringLengthValidator.atLeast(MIN_LENGTH, ErrorMessages.MinLength(FIELD_NAME, MIN_LENGTH)),
        StringLengthValidator.upTo(MAX_LENGTH, ErrorMessages.MaxLength(FIELD_NAME, MAX_LENGTH)));
  }

  /**
   * Thin contract over the stock FormsFX validators above: runs a {@link PasswordField} and returns
   * the first error message, or {@code null} when valid.
   */
  public static String validate(String input) {
    String value = input == null ? "" : input;
    if (value.isBlank()) {
      // Stock FormsFX required-check is empty-based; preserve blank-means-required semantics.
      return ErrorMessages.Required;
    }
    PasswordField field =
        Field.ofPasswordType(value)
            .required(ErrorMessages.Required)
            .validate(toArray(validators()));
    return field.isValid() ? null : field.getErrorMessages().get(0);
  }

  @SuppressWarnings("unchecked")
  private static Validator<String>[] toArray(List<Validator<String>> validators) {
    return validators.toArray(new Validator[0]);
  }
}
