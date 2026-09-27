package com.DesktopApplicationClientJava.validation.utils;

public class ValidatorHelpers {
    public static boolean isEmpty(String input) {
        return input == null || input.isBlank();
    }

    public static boolean isShorterThan(String input, int min) {
        return input.length() < min;
    }

    public static boolean isLongerThan(String input, int max) {
        return input.length() > max;
    }

    /**
     * @param input input string
     * @param sub   substring
     * @return <code>true</code> if <code>input</code> includes a substring
     *         <code>sub</code>, <code>false</code> otherwise
     */
    public static boolean includes(String input, String sub) {
        return input.contains(sub);
    }
}
