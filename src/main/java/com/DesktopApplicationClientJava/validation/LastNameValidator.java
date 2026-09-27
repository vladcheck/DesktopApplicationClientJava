package com.DesktopApplicationClientJava.validation;

import com.DesktopApplicationClientJava.validation.utils.AbstractValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.DesktopApplicationClientJava.validation.utils.ValidatorHelpers;

public class LastNameValidator extends AbstractValidator {
    public String isValid(String input) {
        if (ValidatorHelpers.isEmpty(input))
            return ErrorMessages.Required;
        return null;
    }
}
