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

public class AnnotationDeserializer {

    private static final int MAX_NESTING_DEPTH = 32;

    // JVM type descriptors: V=void, Z=boolean, C=char, B=byte, S=short, I=int, F=float, J=long, D=double
    private static final String JVM_PRIMITIVE_DESCRIPTORS = "VZCBSIFJD";

    /**
     * Cursor-based parser for efficient annotation string parsing.
     */
    private static class Parser {
        private final String input;
        private int pos = 0;

        Parser(String input) {
            this.input = input;
        }

        private char peek() {
            return pos < input.length() ? input.charAt(pos) : '\0';
        }

        private char consume() {
            return pos < input.length() ? input.charAt(pos++) : '\0';
        }

        private void expect(char expected) {
            int errorPos = pos; // Save position before consuming
            char actual = consume();
            if (actual != expected) {
                // Create a helpful error message with context
                StringBuilder errorMsg = new StringBuilder();
                errorMsg.append("Expected '").append(expected).append("' at position ").append(errorPos);
                if (actual == '\0') {
                    errorMsg.append(", but reached end of input");
                } else {
                    errorMsg.append(", but found '").append(actual).append("'");
                }
                errorMsg.append(inputWithErrorIndicator(errorPos));

                throw new IllegalArgumentException(errorMsg.toString());
            }
        }

        private String inputWithErrorIndicator(int errorPos) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("\nInput string:       ").append(input);

            // Add a visual indicator of the error position
            errorMsg.append("\nPosition indicator: ");
            for (int i = 0; i < errorPos; i++) {
                errorMsg.append(' ');
            }
            errorMsg.append('^');

            // Add some context around the error position
            int contextStart = Math.max(0, errorPos - 20);
            int contextEnd = Math.min(input.length(), errorPos + 20);
            if (contextStart > 0 || contextEnd < input.length()) {
                errorMsg.append("\nContext: ");
                if (contextStart > 0) errorMsg.append("...");
                errorMsg.append(input, contextStart, contextEnd);
                if (contextEnd < input.length()) errorMsg.append("...");
            }
            return errorMsg.toString();
        }

        private List<AttributeInfo> parseAttributes() {
            List<AttributeInfo> attributes = new ArrayList<>();

            while (pos < input.length()) {

                // Parse attribute name (identifier)
                String attributeName = parseIdentifier();
                if (attributeName.isEmpty()) break;

                // Check for '=' - if not found, this is a value-only attribute
                expect('=');
                Object attributeValue = parseValue(0);

                attributes.add(new AttributeInfo(attributeName, attributeValue));

                // Check for comma separator
                if (peek() == ',') {
                    consume(); // consume ','
                } else {
                    // No comma means we're done with attributes
                    break;
                }
            }

            return attributes;
        }

        private String parseIdentifier() {
            int start = pos;

            // Parse a simple identifier (letters, digits, underscores)
            if (!Character.isJavaIdentifierStart(peek())) {
                throw new IllegalArgumentException("Expected identifier at position " + pos + " but found '" + peek() + "'" + inputWithErrorIndicator(pos));
            }
            while (pos < input.length()) {
                char c = peek();
                if (Character.isJavaIdentifierPart(c)) {
                    consume();
                } else {
                    break;
                }
            }

            return input.substring(start, pos);
        }

        private Object[] parseArrayValue(int depth) {
            // Parse array elements directly without creating new parser instances
            List<Object> elements = new ArrayList<>();

            expect('[');

            while (pos < input.length() && peek() != ']') {
                // Parse the next array element value directly
                Object element = parseValue(depth);
                elements.add(element);

                // Check if we have more elements (comma) or end of array
                if (peek() == ',') {
                    consume(); // consume comma
                } else {
                    // No comma means end of array
                    break;
                }
            }
            expect(']');
            return elements.toArray();
        }

        private AnnotationInfo parseNestedAnnotationValue(int depth) {
            if (depth > MAX_NESTING_DEPTH) {
                throw new IllegalArgumentException("Maximum nesting depth of " + MAX_NESTING_DEPTH + " exceeded while parsing nested annotation: " + input);
            }
            return parseSingleAnnotation();
        }

        private AnnotationInfo parseSingleAnnotation() {
            expect('@');
            ClassConstant annotationName = parseClassConstantValue();
            if (peek() != '(') {
                // No attributes
                return new AnnotationInfo(annotationName.getDescriptor(), null);
            }

            expect('(');
            // Parse attributes directly without extracting substring first
            List<AttributeInfo> attributes = parseAttributes();
            expect(')');

            return new AnnotationInfo(annotationName.getDescriptor(), attributes);
        }

        private Object parseValue(int depth) {
            if (depth > MAX_NESTING_DEPTH) {
                throw new IllegalArgumentException("Maximum nesting depth of " + MAX_NESTING_DEPTH + " exceeded while parsing: " + input);
            }

            // Check for type-prefixed values (javap style)
            char typePrefix = peek();

            // Handle javap-style type prefixes
            switch (typePrefix) {
                case 'Z': // boolean
                    consume();
                    return parseBooleanValue();
                case 'B': // byte
                    consume();
                    return Byte.parseByte(parseNumericValue());
                case 'C': // char
                    consume();
                    return parseCharValue();
                case 'S': // short
                    consume();
                    return Short.parseShort(parseNumericValue());
                case 'I': // int
                    consume();
                    return Integer.parseInt(parseNumericValue());
                case 'J': // long
                    consume();
                    return Long.parseLong(parseNumericValue());
                case 'F': // float
                    consume();
                    return Float.parseFloat(parseNumericValue());
                case 'D': // double
                    consume();
                    return Double.parseDouble(parseNumericValue());
                case 's': // string
                    consume();
                    return parseStringValue();
                case 'c': // class
                    consume();
                    return parseClassConstantValue();
                case 'e': // enum
                    consume();
                    return parseEnumConstantValue();
                case '[': // array
                    return parseArrayValue(depth);
                case '@': // annotation
                    return parseNestedAnnotationValue(depth + 1);
            }

            // If no type prefix matched, this is an error
            throw new IllegalArgumentException("Unknown value format at position " + pos + ": " + peek() + inputWithErrorIndicator(pos));
        }

        private Object parseBooleanValue() {
            if (matchesAtPosition("true")) {
                pos += 4; // consume "true"
                return Boolean.TRUE;
            } else if (matchesAtPosition("false")) {
                pos += 5; // consume "false"
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Expected boolean value at position " + pos);
        }

        private Object parseCharValue() {
            // Parse numeric value and cast to char
            String numStr = parseNumericValue();
            return (char) Integer.parseInt(numStr);
        }

        private Object parseStringValue() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;

            while (pos < input.length()) {
                char c = peek();
                if (escaped) {
                    // Process escape sequences directly
                    switch (c) {
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case '|':
                            sb.append('|');
                            break; // TypeTable-specific escape
                        default:
                            sb.append(c); // Pass through unknown escapes
                    }
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    consume(); // consume closing quote
                    return sb.toString();
                } else {
                    sb.append(c);
                }
                consume();
            }

            throw new IllegalArgumentException("Unterminated string at position " + (pos - sb.length()));
        }

        private ClassConstant parseClassConstantValue() {
            int start = pos;
            // Parse class descriptor: L...;, [L...;, [I, etc.
            if (peek() == '[') {
                // Array type descriptor
                while (pos < input.length() && peek() == '[') {
                    consume();
                }
            }
            if (peek() == 'L') {
                consume(); // consume 'L'
                while (pos < input.length() && peek() != ';') {
                    consume();
                }
                if (pos < input.length()) {
                    consume(); // consume ';'
                }
            } else if (isPrimitiveTypeDescriptor()) {
                consume(); // consume primitive type character
            }

            String descriptor = input.substring(start, pos);
            return new ClassConstant(descriptor);
        }

        private Object parseEnumConstantValue() {
            // Parse enum constant with dot notation: L...;.CONSTANT_NAME
            int start = pos;
            expect('L');
            while (pos < input.length() && peek() != ';') {
                consume();
            }
            expect(';');
            int semicolonPos = pos - 1;
            expect('.');

            return new EnumConstant(input.substring(start, semicolonPos + 1), parseIdentifier());
        }

        private String parseNumericValue() {
            int start = pos;
            boolean hasSign = false;

            // Check for sign
            if (peek() == '-' || peek() == '+') {
                consume();
                hasSign = true;
            }

            // Check for special floating-point values (Infinity, NaN)
            if (matchesAtPosition("Infinity")) {
                pos += 8;
                return input.substring(start, pos);
            }
            if (matchesAtPosition("NaN")) {
                pos += 3;
                return input.substring(start, pos);
            }

            // Parse digits and decimal point
            while (pos < input.length()) {
                char c = peek();
                if (!Character.isDigit(c) && c != '.') {
                    break;
                }
                consume();
            }

            if (pos == start || (hasSign && pos == start + 1)) {
                throw new IllegalArgumentException("Expected numeric value at position " + start + inputWithErrorIndicator(start));
            }

            return input.substring(start, pos);
        }

        private boolean isPrimitiveTypeDescriptor() {
            char c = peek();
            return JVM_PRIMITIVE_DESCRIPTORS.indexOf(c) != -1;
        }

        private boolean matchesAtPosition(String text) {
            if (pos + text.length() > input.length()) {
                return false;
            }
            return input.startsWith(text, pos);
        }
    }

    /**
     * Parses a serialized annotation string.
     */
    public static AnnotationInfo parseAnnotation(String annotationStr) {
        if (!annotationStr.startsWith("@")) {
            throw new IllegalArgumentException("Invalid annotation format: " + annotationStr);
        }

        Parser parser = new Parser(annotationStr);
        return parser.parseSingleAnnotation();
    }

    /**
     * Parses multiple annotations from a string that may contain:
     * - Multiple annotations separated by pipes (backward compatibility)
     * - Multiple annotations concatenated without delimiters (new format)
     * - Single annotation
     *
     * @param annotationsStr The serialized annotations string
     * @return List of parsed annotation info objects
     */
    public static List<AnnotationInfo> parseAnnotations(String annotationsStr) {
        if (annotationsStr.isEmpty()) {
            return new ArrayList<>();
        }

        List<AnnotationInfo> annotations = new ArrayList<>();

        Parser parser = new Parser(annotationsStr);
        while (parser.pos < parser.input.length()) {
            if (parser.peek() != '@') {
                break; // No more annotations
            }
            annotations.add(parser.parseSingleAnnotation());
        }

        return annotations;
    }


    /**
     * Determines the type of a serialized value and returns it in the appropriate format.
     * This is a public API method that creates a new parser for the complete value string.
     */
    public static Object parseValue(String value) {
        Parser parser = new Parser(value);
        return parser.parseValue(0);
    }


    // Value classes for different types of constants
    public static class AnnotationInfo {
        private final String descriptor;
        private final @Nullable List<AttributeInfo> attributes;

        public AnnotationInfo(String descriptor, @Nullable List<AttributeInfo> attributes) {
            this.descriptor = descriptor;
            this.attributes = attributes;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public @Nullable List<AttributeInfo> getAttributes() {
            return attributes;
        }
    }

    public static class AttributeInfo {
        private final String name;
        private final Object value;

        public AttributeInfo(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }
    }

    public static class ClassConstant {
        private final String descriptor;

        public ClassConstant(String descriptor) {
            this.descriptor = descriptor;
        }

        public String getDescriptor() {
            return descriptor;
        }
    }

    public static class EnumConstant {
        private final String enumDescriptor;
        private final String constantName;

        public EnumConstant(String enumDescriptor, String constantName) {
            this.enumDescriptor = enumDescriptor;
            this.constantName = constantName;
        }

        public String getEnumDescriptor() {
            return enumDescriptor;
        }

        public String getConstantName() {
            return constantName;
        }
    }

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

/**
 * Utility methods for handling TSV escaping in annotation contexts.
 */
