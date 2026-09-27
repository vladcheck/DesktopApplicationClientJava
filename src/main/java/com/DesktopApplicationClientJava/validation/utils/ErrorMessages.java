package com.DesktopApplicationClientJava.validation.utils;

public class ErrorMessages {
    public final static String Required = "Это поле обязательно для заполнения";

    public static final String MinLength(String fieldName, int minLength) {
        return fieldName + " не может быть короче, чем " + minLength + " символов";
    }

    public static final String MaxLength(String fieldName, int maxLength) {
        return fieldName + " не может быть длиннее, чем " + maxLength + " символов";
    }
}
