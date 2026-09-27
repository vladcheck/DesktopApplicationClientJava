package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.AbstractValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;

public class EmailValidator extends AbstractValidator {
    public static final int MIN_EMAIL_LENGTH = 5;
    public static final int MAX_EMAIL_LENGTH = 100;

    public String isValid(String input) {
        if (ValidatorHelpers.isEmpty(input))
            return ErrorMessages.Required;
        if (ValidatorHelpers.isShorterThan(input, MIN_EMAIL_LENGTH))
            return ErrorMessages.MinLength("Почта", MIN_EMAIL_LENGTH);
        if (ValidatorHelpers.isLongerThan(input, MAX_EMAIL_LENGTH))
            return ErrorMessages.MaxLength("Почта", MAX_EMAIL_LENGTH);
        if (!ValidatorHelpers.includes(input, "@"))
            return "Неверный формат почты";
        return null;
    }
}
