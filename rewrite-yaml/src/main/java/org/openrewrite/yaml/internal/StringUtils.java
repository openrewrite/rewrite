/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.yaml.internal;

public class StringUtils {

    private StringUtils() {
    }

    private static final String INDICATOR_CHARS = "-?:,[]{}#&*!|>'\"%@`";

    /**
     * Quotes a YAML scalar value if needed, per the YAML 1.2.2 spec.
     * <p>
     * If the input is already quoted (surrounded by matching single or double quotes),
     * it is returned as-is. Otherwise, the method determines whether the value requires
     * quoting and applies the minimal quoting style: single quotes by default, double
     * quotes only when escape sequences are required.
     *
     * @param value the raw string value
     * @return the value, potentially quoted for safe use as a YAML scalar
     */
    public static String quoteIfNeeded(String value) {
        if (isAlreadyQuoted(value)) {
            return value;
        }
        if (!needsQuoting(value)) {
            return value;
        }
        if (needsDoubleQuotes(value)) {
            return "\"" + escapeForDoubleQuoting(value) + "\"";
        }
        return "'" + value + "'";
    }

    private static boolean isAlreadyQuoted(String value) {
        if (value.length() < 2) {
            return false;
        }
        return (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'') ||
               (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"');
    }

    private static boolean needsQuoting(String value) {
        // Empty string
        if (value.isEmpty()) {
            return true;
        }

        // Leading or trailing whitespace
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if (first == ' ' || first == '\t' || last == ' ' || last == '\t') {
            return true;
        }

        // Contains line breaks
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return true;
        }

        // Starts with indicator character
        if (INDICATOR_CHARS.indexOf(first) >= 0) {
            return true;
        }

        // Contains colon-space or space-hash mid-string
        if (value.contains(": ") || value.contains(" #")) {
            return true;
        }

        // Document markers
        if (value.equals("---") || value.equals("...")) {
            return true;
        }

        // Contains control characters
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }

        return false;
    }

    private static boolean needsDoubleQuotes(String value) {
        // Need double quotes if value contains single quote
        if (value.indexOf('\'') >= 0) {
            return true;
        }
        // Need double quotes if value contains control characters
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    private static String escapeForDoubleQuoting(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\0':
                    sb.append("\\0");
                    break;
                case '\u0007':
                    sb.append("\\a");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\u000B':
                    sb.append("\\v");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\u001B':
                    sb.append("\\e");
                    break;
                default:
                    if (c < 0x20 || c == 0x7F) {
                        sb.append("\\x");
                        sb.append(String.format("%02x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
