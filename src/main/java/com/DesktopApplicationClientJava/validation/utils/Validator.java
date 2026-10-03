package com.DesktopApplicationClientJava.validation.utils;

@FunctionalInterface
public interface Validator {
  String isValid(String input);
}
