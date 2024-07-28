/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.internal;

import org.openrewrite.Incubating;

import java.util.regex.Pattern;

/**
 * Utilities for standard name case conventions.
 */
@Incubating(since = "7.17.0")
public enum NameCaseConvention {
    /**
     * Lowercase and hyphen-separated, e.g., "lower-hyphen".
     * This is also known as "kebab-case".
     */
    LOWER_HYPHEN,

    /**
     * Lowercase and underscore-separated, e.g., "lower_underscore".
     * This is also known as "snake_case".
     */
    LOWER_UNDERSCORE,

    /**
     * Camel case with the first letter lowercase, e.g., "lowerCamel".
     * This is the standard Java variable naming convention.
     */
    LOWER_CAMEL,

    /**
     * Camel case with the first letter uppercase, e.g., "UpperCamel".
     * This is the standard Java and C++ class naming convention.
     */
    UPPER_CAMEL,

    /**
     * Underscore separated with all letters uppercase, e.g., "UPPER_UNDERSCORE".
     * This is the standard Java and C++ constant variable naming convention.
     * This is also known as "SCREAMING_SNAKE_CASE".
     */
    UPPER_UNDERSCORE;

    private static final Pattern CAMEL_CASE_SPLIT = Pattern.compile("[\\s_-]");
    private static final int uppercaseAbbreviationMinLength = 3;

    /**
     * Formats the input to the style of this {@link NameCaseConvention}.
     *
     * @param str The input string to format.
     * @return The string formatted to the style of this convention.
     */
    public String format(String str) {
        return format(this, str);
    }

    public static String format(NameCaseConvention convention, String str) {
        switch (convention) {
            case LOWER_HYPHEN:
                return lowerHyphen(str);
            case LOWER_UNDERSCORE:
                return lowerUnderscore(str);
            case LOWER_CAMEL:
                return toCamelCase(str, true);
            case UPPER_CAMEL:
                return toCamelCase(str, false);
            case UPPER_UNDERSCORE:
                return upperUnderscore(str);
            default:
                return str;
        }
    }

    /**
     * Whether the input matches the formatting style of this {@link NameCaseConvention}.
     *
     * @param str The input string to check.
     * @return Whether the input matches the formatting style of this convention.
     */
    public boolean matches(String str) {
        return matches(this, str);
    }

    public static boolean matches(NameCaseConvention convention, String str) {
        switch (convention) {
            case LOWER_CAMEL:
                if (!Character.isLowerCase(str.charAt(0)) && str.charAt(0) != '$') {
                    return false;
                }
                for (int i = 1; i < str.length(); i++) {
                    char p = str.charAt(i - 1);
                    char c = str.charAt(i);
                    if (Character.isUpperCase(c) && Character.isUpperCase(p)) {
                        return false;
                    } else if (c == '_') {
                        return false;
                    }
                }
                return true;
            case UPPER_CAMEL:
                if (!Character.isUpperCase(str.charAt(0)) && str.charAt(0) != '$') {
                    return false;
                }
                for (int i = 1; i < str.length(); i++) {
                    char p = str.charAt(i - 1);
                    char c = str.charAt(i);
                    if (Character.isUpperCase(c) && Character.isUpperCase(p)) {
                        return false;
                    } else if (c == '_') {
                        return false;
                    }
                }
                return true;
            case LOWER_UNDERSCORE:
                for (int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if (Character.isAlphabetic(c) && Character.isUpperCase(c)) {
                        return false;
                    }
                }
                return true;
            case UPPER_UNDERSCORE:
                for (int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if (Character.isAlphabetic(c) && Character.isLowerCase(c)) {
                        return false;
                    }
                }
                return true;
            case LOWER_HYPHEN:
                for (int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if (Character.isAlphabetic(c) && Character.isUpperCase(c)) {
                        return false;
                    } else if (c == '_') {
                        return false;
                    }
                }
                return true;
            default:
                return str.equals(format(convention, str));
        }
    }

    /**
     * Check equality between two inputs using "relaxed binding" rules.
     * The inputs will be converted to {@link NameCaseConvention#LOWER_CAMEL} before being checked using {@link String#equals}.
     *
     * @param str0 The first input to compare.
     * @param str1 The second input to compare.
     * @return Whether the inputs are equal.
     * @see <a href="https://docs.micronaut.io/3.1.0/guide/index.html#_property_value_binding">Micronaut Property Value Binding Normalization</a>
     * @see <a href="https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding">Spring Boot Relaxed Binding</a>
     */
    public static boolean equalsRelaxedBinding(String str0, String str1) {
        return LOWER_CAMEL.format(str0).equals(LOWER_CAMEL.format(str1));
    }

    /**
     * Check equality between two inputs using "relaxed binding" rules.
     * The inputs will be converted to {@link NameCaseConvention#LOWER_CAMEL} before being checked using {@link String#equals}.
     *
     * @param test The property to test.
     * @param pattern A glob pattern.
     * @return Whether the inputs are equal.
     * @see <a href="https://docs.micronaut.io/3.1.0/guide/index.html#_property_value_binding">Micronaut Property Value Binding Normalization</a>
     * @see <a href="https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding">Spring Boot Relaxed Binding</a>
     */
    public static boolean matchesGlobRelaxedBinding(String test, String pattern) {
        return StringUtils.matchesGlob(LOWER_CAMEL.format(test), LOWER_CAMEL.format(pattern));
    }

    public static boolean matchesRegexRelaxedBinding(String test, String pattern) {
        return LOWER_CAMEL.format(test).matches(LOWER_CAMEL.format(pattern));
    }

    private static String lowerHyphen(String str) {
        return nameCaseJoiner(str.replace('_', '-').replace(' ', '-'), true, '-');
    }

    private static String lowerUnderscore(String str) {
        return nameCaseJoiner(str.replace('-', '_').replace(' ', '_'), true, '_')
                .toLowerCase();
    }

    /**
     * Returns input formatted using the Java "camelCase" (or "CamelCase") naming convention.
     *
     * @param str                  The input string to check.
     * @param lowerCaseFirstLetter Whether the first letter should be lowerCase or UpperCase.
     * @return The string formatted as {@link NameCaseConvention#LOWER_CAMEL}.
     */
    private static String toCamelCase(String str, boolean lowerCaseFirstLetter) {
        boolean allUpperCase = true;
        final int strLength = str.length();

        boolean uppercaseAbbreviationIncluded = false;
        int uppercaseAbbreviationEndIndex = -1;
        int i = 0;
        while (i < strLength) {
            final char c = str.charAt(i);
            if (Character.isLowerCase(c)) {
                allUpperCase = false;
                break;
            }
            i++;
        }
        if (i > uppercaseAbbreviationMinLength) {
            uppercaseAbbreviationIncluded = true;
            uppercaseAbbreviationEndIndex = i - 1;
        }

        StringBuilder sb = new StringBuilder(strLength);
        if (allUpperCase) {
            str = str.toLowerCase();
        } else if (uppercaseAbbreviationIncluded) {
            final String uppercaseAbbreviation = str.substring(0, uppercaseAbbreviationEndIndex);
            sb.append(StringUtils.capitalize(uppercaseAbbreviation.toLowerCase()));
            str = str.substring(uppercaseAbbreviationEndIndex);
        }

        for (String s : CAMEL_CASE_SPLIT.split(str)) {
            String capitalize = StringUtils.capitalize(s);
            sb.append(capitalize);
        }
        String result = sb.toString();
        if (lowerCaseFirstLetter) {
            return StringUtils.uncapitalize(result);
        }
        return result;
    }

    private static String upperUnderscore(String str) {
        return nameCaseJoiner(str.replace('-', '_').replace(' ', '_'), false, '_')
                .toUpperCase();
    }

    private static String nameCaseJoiner(String str, boolean lowerCaseFirstLetter, char separatorChar) {
        StringBuilder builder = new StringBuilder();
        if (lowerCaseFirstLetter) {
            char[] chars = str.toCharArray();
            boolean first = true;
            char last = '0';
            char secondLast = separatorChar;
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (Character.isLowerCase(c) || !Character.isLetter(c)) {
                    first = false;
                    if (c != separatorChar) {
                        if (last == separatorChar) {
                            builder.append(separatorChar);
                        }
                        builder.append(c);
                    }
                } else {
                    char lowerCaseChar = Character.toLowerCase(c);
                    if (first) {
                        first = false;
                        builder.append(lowerCaseChar);
                    } else if (Character.isUpperCase(last) || last == '.') {
                        builder.append(lowerCaseChar);
                    } else if (Character.isDigit(last) && (Character.isUpperCase(secondLast) || secondLast == separatorChar)) {
                        builder.append(lowerCaseChar);
                    } else {
                        builder.append(separatorChar).append(lowerCaseChar);
                    }
                }
                if (i > 1) {
                    secondLast = last;
                }
                last = c;
            }
        } else {
            boolean first = true;
            char last = '0';
            for (char c : str.toCharArray()) {
                if (first) {
                    builder.append(c);
                    first = false;
                } else {
                    if (Character.isUpperCase(c) && !Character.isUpperCase(last)) {
                        if (c != separatorChar) {
                            builder.append(separatorChar);
                        }
                        builder.append(c);
                    } else {
                        if (c == '.') {
                            first = true;
                        }
                        if (c != separatorChar) {
                            if (last == separatorChar) {
                                builder.append(separatorChar);
                            }
                            builder.append(c);
                        }
                    }
                }
                last = c;
            }
        }

        return builder.toString();
    }

}
