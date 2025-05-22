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
package org.openrewrite.java.internal.parser;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Deserializes annotation strings from the TypeTable format back into Java objects.
 * This class handles parsing of different types of annotation values:
 * - Primitive types (boolean, char, byte, short, int, long, float, double)
 * - String values
 * - Class constants
 * - Field constants
 * - Enum constants
 * - Arrays and nested arrays
 * - Nested annotations
 */
public class AnnotationDeserializer {

    /**
     * Parses a serialized annotation string.
     *
     * @param annotationStr The serialized annotation string
     * @return The annotation name and attributes (if any)
     */
    public static AnnotationInfo parseAnnotation(String annotationStr) {
        if (!annotationStr.startsWith("@")) {
            throw new IllegalArgumentException("Invalid annotation format: " + annotationStr);
        }

        // Extract annotation name and attributes
        int parenIndex = annotationStr.indexOf('(');
        String annotationName;
        if (parenIndex == -1) {
            // Simple annotation without attributes
            annotationName = annotationStr.substring(1).replace('/', '.');
            return new AnnotationInfo(annotationName, null);
        }

        annotationName = annotationStr.substring(1, parenIndex).replace('/', '.');
        String attributesStr = annotationStr.substring(parenIndex + 1, annotationStr.length() - 1);

        List<AttributeInfo> attributes = parseAttributes(attributesStr);
        return new AnnotationInfo(annotationName, attributes);
    }

    /**
     * Parses a string containing serialized attributes.
     *
     * @param attributesStr The serialized attributes string
     * @return A list of attribute name-value pairs
     */
    private static List<AttributeInfo> parseAttributes(String attributesStr) {
        List<AttributeInfo> attributes = new ArrayList<>();
        if (attributesStr.isEmpty()) {
            return attributes;
        }

        // Split by commas, but respect nested structures
        List<String> attributeStrings = splitRespectingNestedStructures(attributesStr, ',');

        for (String attributeStr : attributeStrings) {
            int equalsIndex = attributeStr.indexOf('=');
            if (equalsIndex == -1) {
                // Single unnamed attribute (e.g., "value")
                attributes.add(new AttributeInfo("value", attributeStr.trim()));
            } else {
                // Named attribute (e.g., "name=value")
                String name = attributeStr.substring(0, equalsIndex).trim();
                String value = attributeStr.substring(equalsIndex + 1).trim();
                attributes.add(new AttributeInfo(name, value));
            }
        }

        return attributes;
    }

    /**
     * Splits a string by a delimiter, respecting nested structures like parentheses,
     * braces, and quotes.
     *
     * @param str The string to split
     * @param delimiter The delimiter character
     * @return A list of substrings
     */
    private static List<String> splitRespectingNestedStructures(String str, char delimiter) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int parenDepth = 0;
        int braceDepth = 0;
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"' && !inQuotes) {
                inQuotes = true;
            } else if (c == '"' && inQuotes) {
                inQuotes = false;
            } else if (!inQuotes) {
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')') {
                    parenDepth--;
                } else if (c == '{') {
                    braceDepth++;
                } else if (c == '}') {
                    braceDepth--;
                } else if (c == delimiter && parenDepth == 0 && braceDepth == 0) {
                    result.add(str.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }

        // Add the last segment
        if (start < str.length()) {
            result.add(str.substring(start).trim());
        }

        return result;
    }

    /**
     * Unescapes special characters in a string.
     *
     * @param value The string to unescape
     * @return The unescaped string
     */
    public static String unescapeSpecialCharacters(String value) {
        return value.replace("\\|", "|").replace("\\t", "\t");
    }

    /**
     * Determines the type of a serialized value and returns it in the appropriate format.
     *
     * @param value The serialized value
     * @return The value in the appropriate format
     */
    public static Object parseValue(String value) {
        value = value.trim();

        // Boolean
        if ("true".equals(value) || "false".equals(value)) {
            return Boolean.parseBoolean(value);
        }

        // Character
        if (value.startsWith("'") && value.endsWith("'")) {
            return parseCharValue(value);
        }

        // String
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return parseStringValue(value);
        }

        // Array
        if (value.startsWith("{") && value.endsWith("}")) {
            return parseArrayValue(value);
        }

        // Nested annotation
        if (value.startsWith("@")) {
            return parseAnnotation(value);
        }

        // Class constant
        if (value.endsWith(".class")) {
            return new ClassConstant(value.substring(0, value.length() - 6).replace('/', '.'));
        }

        // Enum constant or field constant
        if (value.contains(".") && !value.contains("(")) {
            int lastDotIndex = value.lastIndexOf('.');
            String typeName = value.substring(0, lastDotIndex).replace('/', '.');
            String constantName = value.substring(lastDotIndex + 1);

            // Heuristic: if the constant name starts with an uppercase letter, it's likely an enum
            if (Character.isUpperCase(constantName.charAt(0))) {
                return new EnumConstant(typeName, constantName);
            } else {
                return new FieldConstant(typeName, constantName);
            }
        }

        // Numeric values
        try {
            // Long
            if (value.endsWith("L") || value.endsWith("l")) {
                return Long.parseLong(value.substring(0, value.length() - 1));
            }

            // Float
            if (value.endsWith("F") || value.endsWith("f")) {
                return Float.parseFloat(value.substring(0, value.length() - 1));
            }

            // Double
            if (value.endsWith("D") || value.endsWith("d") || value.contains(".")) {
                return Double.parseDouble(value);
            }

            // Integer (or other numeric types that fit in an int)
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // If it's not a recognized format, return it as a string
            return value;
        }
    }

    /**
     * Parses a serialized character value.
     *
     * @param value The serialized character value
     * @return The character value
     */
    private static char parseCharValue(String value) {
        // Remove the surrounding quotes
        String charContent = value.substring(1, value.length() - 1);

        if (charContent.length() == 1) {
            return charContent.charAt(0);
        } else if (charContent.startsWith("\\")) {
            // Handle escape sequences
            char escapeChar = charContent.charAt(1);
            switch (escapeChar) {
                case '\'': return '\'';
                case '\\': return '\\';
                case 'n': return '\n';
                case 'r': return '\r';
                case 't': return '\t';
                default: return escapeChar;
            }
        } else {
            throw new IllegalArgumentException("Invalid character format: " + value);
        }
    }

    /**
     * Parses a serialized string value.
     *
     * @param value The serialized string value
     * @return The string value
     */
    private static String parseStringValue(String value) {
        // Remove the surrounding quotes
        String stringContent = value.substring(1, value.length() - 1);

        StringBuilder sb = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < stringContent.length(); i++) {
            char c = stringContent.charAt(i);

            if (escaped) {
                switch (c) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case '|': sb.append('|'); break;
                    default: sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Parses a serialized array value.
     *
     * @param value The serialized array value
     * @return An array of values
     */
    private static Object[] parseArrayValue(String value) {
        // Remove the surrounding braces
        String arrayContent = value.substring(1, value.length() - 1).trim();

        if (arrayContent.isEmpty()) {
            return new Object[0];
        }

        List<String> elements = splitRespectingNestedStructures(arrayContent, ',');
        Object[] result = new Object[elements.size()];

        for (int i = 0; i < elements.size(); i++) {
            result[i] = parseValue(elements.get(i));
        }

        return result;
    }

    /**
     * Represents an annotation with its name and attributes.
     */
    public static class AnnotationInfo {
        private final String name;
        private final @Nullable List<AttributeInfo> attributes;

        public AnnotationInfo(String name, @Nullable List<AttributeInfo> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        public String getName() {
            return name;
        }

        public @Nullable List<AttributeInfo> getAttributes() {
            return attributes;
        }
    }

    /**
     * Represents an annotation attribute with its name and value.
     */
    public static class AttributeInfo {
        private final String name;
        private final String value;

        public AttributeInfo(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Represents a class constant.
     */
    public static class ClassConstant {
        private final String className;

        public ClassConstant(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }
    }

    /**
     * Represents an enum constant.
     */
    public static class EnumConstant {
        private final String enumType;
        private final String constantName;

        public EnumConstant(String enumType, String constantName) {
            this.enumType = enumType;
            this.constantName = constantName;
        }

        public String getEnumType() {
            return enumType;
        }

        public String getConstantName() {
            return constantName;
        }
    }

    /**
     * Represents a field constant.
     */
    public static class FieldConstant {
        private final String className;
        private final String fieldName;

        public FieldConstant(String className, String fieldName) {
            this.className = className;
            this.fieldName = fieldName;
        }

        public String getClassName() {
            return className;
        }

        public String getFieldName() {
            return fieldName;
        }
    }
}
