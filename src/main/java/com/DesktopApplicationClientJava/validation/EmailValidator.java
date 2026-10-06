package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.model.validators.RegexValidator;
import com.dlsc.formsfx.model.validators.StringLengthValidator;
import com.dlsc.formsfx.model.validators.Validator;
import java.util.List;

public final class EmailValidator {
  public static final String FIELD_NAME = "Почта";
  public static final int MIN_EMAIL_LENGTH = 5;
  public static final int MAX_EMAIL_LENGTH = 100;

  private EmailValidator() {}

  public static List<Validator<String>> validators() {
    return List.of(
        StringLengthValidator.atLeast(
            MIN_EMAIL_LENGTH, ErrorMessages.MinLength(FIELD_NAME, MIN_EMAIL_LENGTH)),
        StringLengthValidator.upTo(
            MAX_EMAIL_LENGTH, ErrorMessages.MaxLength(FIELD_NAME, MAX_EMAIL_LENGTH)),
        RegexValidator.forEmail(ErrorMessages.IncorrectFormat));
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
