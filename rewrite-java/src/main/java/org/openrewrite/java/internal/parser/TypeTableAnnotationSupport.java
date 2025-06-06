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
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.internal.parser.AnnotationSerializer.*;

/**
 * Helper class for applying annotations to classes, methods, and fields using ASM.
 */
class AnnotationApplier {

    /**
     * Applies an annotation to a class, method, or field.
     *
     * @param annotationStr The serialized annotation string
     * @param visitAnnotation A function that creates an AnnotationVisitor
     * @return true if the annotation was applied successfully, false otherwise
     */
    public static boolean applyAnnotation(String annotationStr, AnnotationVisitorCreator visitAnnotation) {
        if (!annotationStr.startsWith("@")) {
            return false;
        }

        // Parse the annotation using AnnotationDeserializer
        AnnotationDeserializer.AnnotationInfo annotationInfo = AnnotationDeserializer.parseAnnotation(annotationStr);
        String annotationName = annotationInfo.getName();

        String annotationDescriptor = "L" + annotationName.replace('.', '/') + ";";
        AnnotationVisitor av = visitAnnotation.create(annotationDescriptor, true);
        if (av != null) {
            // Apply annotation attributes if present
            if (annotationInfo.getAttributes() != null && !annotationInfo.getAttributes().isEmpty()) {
                for (AnnotationDeserializer.AttributeInfo attribute : annotationInfo.getAttributes()) {
                    String attributeName = attribute.getName();
                    String attributeValue = attribute.getValue();
                    Object parsedValue = AnnotationDeserializer.parseValue(attributeValue);

                    if (parsedValue instanceof Boolean) {
                        av.visit(attributeName, parsedValue);
                    } else if (parsedValue instanceof Character) {
                        av.visit(attributeName, parsedValue);
                    } else if (parsedValue instanceof Number) {
                        av.visit(attributeName, parsedValue);
                    } else if (parsedValue instanceof String) {
                        // Remove quotes from string values
                        String stringValue = (String) parsedValue;
                        if (stringValue.startsWith("\"") && stringValue.endsWith("\"")) {
                            stringValue = stringValue.substring(1, stringValue.length() - 1);
                        }
                        av.visit(attributeName, stringValue);
                    } else if (parsedValue instanceof AnnotationDeserializer.ClassConstant) {
                        String className = ((AnnotationDeserializer.ClassConstant) parsedValue).getDescriptor();
                        av.visit(attributeName, org.objectweb.asm.Type.getType(className.replace('.', '/')));
                    } else if (parsedValue instanceof AnnotationDeserializer.EnumConstant) {
                        AnnotationDeserializer.EnumConstant enumConstant = (AnnotationDeserializer.EnumConstant) parsedValue;
                        String enumDescriptor = enumConstant.getEnumDescriptor();
                        String constantName = enumConstant.getConstantName();
                        av.visitEnum(attributeName, enumDescriptor, constantName);
                    } else if (parsedValue instanceof AnnotationDeserializer.AnnotationInfo) {
                        // Handle nested annotations
                        AnnotationDeserializer.AnnotationInfo nestedAnnotationInfo = (AnnotationDeserializer.AnnotationInfo) parsedValue;
                        String nestedAnnotationName = nestedAnnotationInfo.getName();
                        String nestedAnnotationDescriptor = "L" + nestedAnnotationName.replace('.', '/') + ";";
                        AnnotationVisitor nestedAv = av.visitAnnotation(attributeName, nestedAnnotationDescriptor);

                        if (nestedAv != null && nestedAnnotationInfo.getAttributes() != null && !nestedAnnotationInfo.getAttributes().isEmpty()) {
                            for (AnnotationDeserializer.AttributeInfo nestedAttribute : nestedAnnotationInfo.getAttributes()) {
                                String nestedAttributeName = nestedAttribute.getName();
                                String nestedAttributeValue = nestedAttribute.getValue();
                                Object nestedParsedValue = AnnotationDeserializer.parseValue(nestedAttributeValue);

                                if (nestedParsedValue instanceof Boolean) {
                                    nestedAv.visit(nestedAttributeName, nestedParsedValue);
                                } else if (nestedParsedValue instanceof Character) {
                                    nestedAv.visit(nestedAttributeName, nestedParsedValue);
                                } else if (nestedParsedValue instanceof Number) {
                                    nestedAv.visit(nestedAttributeName, nestedParsedValue);
                                } else if (nestedParsedValue instanceof String) {
                                    // Remove quotes from string values
                                    String stringValue = (String) nestedParsedValue;
                                    if (stringValue.startsWith("\"") && stringValue.endsWith("\"")) {
                                        stringValue = stringValue.substring(1, stringValue.length() - 1);
                                    }
                                    nestedAv.visit(nestedAttributeName, stringValue);
                                } else if (nestedParsedValue instanceof AnnotationDeserializer.ClassConstant) {
                                    String className = ((AnnotationDeserializer.ClassConstant) nestedParsedValue).getDescriptor();
                                    nestedAv.visit(nestedAttributeName, org.objectweb.asm.Type.getObjectType(className.replace('.', '/')));
                                } else if (nestedParsedValue instanceof AnnotationDeserializer.EnumConstant) {
                                    AnnotationDeserializer.EnumConstant enumConstant = (AnnotationDeserializer.EnumConstant) nestedParsedValue;
                                    String enumType = enumConstant.getEnumDescriptor();
                                    String constantName = enumConstant.getConstantName();
                                    nestedAv.visitEnum(nestedAttributeName, "L" + enumType.replace('.', '/') + ";", constantName);
                                }
                                // We don't handle nested annotations within nested annotations or arrays within nested annotations
                            }
                            nestedAv.visitEnd();
                        }
                    } else if (parsedValue instanceof Object[]) {
                        // Handle array attributes
                        Object[] arrayValues = (Object[]) parsedValue;
                        AnnotationVisitor arrayVisitor = av.visitArray(attributeName);
                        if (arrayVisitor != null) {
                            for (Object arrayValue : arrayValues) {
                                if (arrayValue instanceof Boolean) {
                                    arrayVisitor.visit(null, arrayValue);
                                } else if (arrayValue instanceof Character) {
                                    arrayVisitor.visit(null, arrayValue);
                                } else if (arrayValue instanceof Number) {
                                    arrayVisitor.visit(null, arrayValue);
                                } else if (arrayValue instanceof String) {
                                    // Remove quotes from string values
                                    String stringValue = (String) arrayValue;
                                    if (stringValue.startsWith("\"") && stringValue.endsWith("\"")) {
                                        stringValue = stringValue.substring(1, stringValue.length() - 1);
                                    }
                                    arrayVisitor.visit(null, stringValue);
                                } else if (arrayValue instanceof AnnotationDeserializer.ClassConstant) {
                                    String className = ((AnnotationDeserializer.ClassConstant) arrayValue).getDescriptor();
                                    arrayVisitor.visit(null, org.objectweb.asm.Type.getObjectType(className.replace('.', '/')));
                                } else if (arrayValue instanceof AnnotationDeserializer.EnumConstant) {
                                    AnnotationDeserializer.EnumConstant enumConstant = (AnnotationDeserializer.EnumConstant) arrayValue;
                                    String enumDescriptor = enumConstant.getEnumDescriptor();
                                    String constantName = enumConstant.getConstantName();
                                    arrayVisitor.visitEnum(null, enumDescriptor, constantName);
                                }
                            }
                            arrayVisitor.visitEnd();
                        }
                    }
                }
            }

            av.visitEnd();
            return true;
        }

        return false;
    }

    /**
     * Functional interface for creating an AnnotationVisitor.
     */
    @FunctionalInterface
    public interface AnnotationVisitorCreator {
        @Nullable
        AnnotationVisitor create(String descriptor, boolean visible);
    }

}

/**
 * Helper class to create reusable annotation visitors, eliminating code duplication.
 */
class AnnotationCollectorHelper {

    /**
     * Creates an annotation visitor that collects annotation values into a serialized string format.
     *
     * @param descriptor The descriptor of the annotation
     * @param collectedAnnotations The list to which the serialized annotation will be added
     * @return An annotation visitor that collects annotation values
     */
    static AnnotationVisitor createCollector(String descriptor, List<String> collectedAnnotations) {
        String annotationName = Type.getType(descriptor).getClassName();
        String baseAnnotation = serializeSimpleAnnotation(annotationName);

        return new AnnotationValueCollector(annotationName, null, result -> {
            if (result.isEmpty()) {
                collectedAnnotations.add(baseAnnotation);
            } else {
                String annotationWithAttributes = serializeAnnotationWithAttributes(
                        annotationName,
                        result.toArray(new String[0])
                );
                collectedAnnotations.add(annotationWithAttributes);
            }
        });
    }

    /**
     * A reusable annotation visitor that collects annotation values and supports nesting.
     * This class handles nested annotations and arrays properly by creating new instances
     * of itself for nested structures.
     */
    static class AnnotationValueCollector extends AnnotationVisitor {
        private final String annotationName;
        private final @Nullable String attributeName;
        private final ResultCallback callback;
        private final List<String> collectedValues = new ArrayList<>();

        /**
         * Creates a new annotation value collector.
         *
         * @param annotationName The name of the annotation being collected
         * @param attributeName The name of the attribute being collected (null for array elements)
         * @param callback The callback to invoke with the collected result
         */
        AnnotationValueCollector(String annotationName, @Nullable String attributeName, ResultCallback callback) {
            super(Opcodes.ASM9);
            this.annotationName = annotationName;
            this.attributeName = attributeName;
            this.callback = callback;
        }

        @Override
        public void visit(@Nullable String name, Object value) {
            String attributeValue = serializeValue(value);
            if (attributeName == null && name ==  null) {
                // This is an array element
                collectedValues.add(attributeValue);
            } else {
                // This is a named attribute
                collectedValues.add(serializeAttribute(
                        name != null ? name : attributeName,
                        attributeValue
                ));
            }
        }

        @Override
        public void visitEnum(@Nullable String name, String descriptor, String value) {
            String attributeValue = serializeEnumConstant(descriptor, value);
            if (name == null && attributeName == null) {
                // This is an array element
                collectedValues.add(attributeValue);
            } else {
                // This is a named attribute
                collectedValues.add(serializeAttribute(
                        name != null ? name : attributeName,
                        attributeValue
                ));
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(@Nullable String name, String descriptor) {
            String nestedAnnotationName = Type.getType(descriptor).getClassName();

            // Create a new collector for the nested annotation
            return new AnnotationValueCollector(nestedAnnotationName, name, result -> {
                String nestedAnnotation;
                if (result.isEmpty()) {
                    nestedAnnotation = serializeSimpleAnnotation(nestedAnnotationName);
                } else {
                    nestedAnnotation = serializeAnnotationWithAttributes(
                            nestedAnnotationName,
                            result.toArray(new String[0])
                    );
                }

                if (attributeName == null && name == null) {
                    // This is an array element
                    collectedValues.add(nestedAnnotation);
                } else {
                    // This is a named attribute
                    collectedValues.add(serializeAttribute(
                            name != null ? name : attributeName,
                            nestedAnnotation
                    ));
                }
            });
        }

        @Override
        public AnnotationVisitor visitArray(@Nullable String name) {
            // Create a new collector for the array elements
            return new AnnotationValueCollector(annotationName, null, result -> {
                String arrayValue = serializeArray(result.toArray(new String[0]));
                collectedValues.add(serializeAttribute(
                        name != null ? name : requireNonNull(attributeName),
                        arrayValue
                ));
            });
        }

        @Override
        public void visitEnd() {
            callback.onResult(collectedValues);
        }
    }

    /**
     * Callback interface for receiving the result of annotation value collection.
     */
    @FunctionalInterface
    interface ResultCallback {
        void onResult(List<String> result);
    }
}

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
class AnnotationDeserializer {

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

        // Class constant
        if (value.startsWith("L") && value.endsWith(";")) {
            return new ClassConstant(value);
        }

        // Constant
        if (value.startsWith("L")) {
            int semicolonIndex = value.indexOf(';');
            return new EnumConstant(value.substring(0, semicolonIndex + 1), value.substring(semicolonIndex + 1));
        }

        // primitive types
        if (value.length() == 1 && "VZCBSIFJD".contains(value)) {
            return new ClassConstant(value);
        }

        // array types
        if (value.startsWith("[")) {
            return new ClassConstant(value);
        }

        // Nested annotation
        if (value.startsWith("@")) {
            return parseAnnotation(value);
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
        private final String descriptor;

        public ClassConstant(String descriptor) {
            this.descriptor = descriptor;
        }

        public String getDescriptor() {
            return descriptor;
        }
    }

    /**
     * Represents an enum constant.
     */
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

/**
 * Serializes Java annotations to a string format for storage in the TypeTable.
 * This class handles serialization of different types of annotation values:
 * - Primitive types (boolean, char, byte, short, int, long, float, double)
 * - String values
 * - Class constants
 * - Field constants
 * - Enum constants
 * - Arrays and nested arrays
 * - Nested annotations
 */
class AnnotationSerializer {

    /**
     * Serializes a simple annotation without attributes.
     *
     * @param annotationName The fully qualified name of the annotation
     * @return The serialized annotation string
     */
    public static String serializeSimpleAnnotation(String annotationName) {
        return "@" + annotationName.replace('.', '/');
    }

    /**
     * Serializes a boolean value.
     *
     * @param value The boolean value
     * @return The serialized boolean value
     */
    public static String serializeBoolean(boolean value) {
        return Boolean.toString(value);
    }

    /**
     * Serializes a character value.
     *
     * @param value The character value
     * @return The serialized character value
     */
    public static String serializeChar(char value) {
        StringBuilder sb = new StringBuilder();
        sb.append('\'');
        switch (value) {
            case '\'':
                sb.append("\\'");
                break;
            case '\\':
                sb.append("\\\\");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            default:
                sb.append(value);
        }
        sb.append('\'');
        return sb.toString();
    }

    /**
     * Serializes a numeric value (byte, short, int).
     *
     * @param value The numeric value
     * @return The serialized numeric value
     */
    public static String serializeNumber(Number value) {
        return value.toString();
    }

    /**
     * Serializes a long value.
     *
     * @param value The long value
     * @return The serialized long value
     */
    public static String serializeLong(long value) {
        return value + "L";
    }

    /**
     * Serializes a float value.
     *
     * @param value The float value
     * @return The serialized float value
     */
    public static String serializeFloat(float value) {
        return value + "F";
    }

    /**
     * Serializes a double value.
     *
     * @param value The double value
     * @return The serialized double value
     */
    public static String serializeDouble(double value) {
        return Double.toString(value);
    }

    /**
     * Serializes a string value.
     *
     * @param value The string value
     * @return The serialized string value
     */
    public static String serializeString(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '|':
                    sb.append("\\|");
                    break;
                default:
                    sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Serializes a class constant.
     *
     * @param type The type
     * @return The serialized class constant
     */
    public static String serializeClassConstant(Type type) {
        return type.getDescriptor();
    }

    /**
     * Serializes a field constant.
     *
     * @param className The fully qualified name of the class containing the field
     * @param fieldName The name of the field
     * @return The serialized field constant
     */
    public static String serializeFieldConstant(String className, String fieldName) {
        return className.replace('.', '/') + "." + fieldName;
    }

    /**
     * Serializes an enum constant.
     *
     * @param enumDescriptor The enum type descriptor
     * @param enumConstant The name of the enum constant
     * @return The serialized enum constant
     */
    public static String serializeEnumConstant(String enumDescriptor, String enumConstant) {
        return enumDescriptor + enumConstant;
    }

    /**
     * Serializes an array of values.
     *
     * @param values The array of serialized values
     * @return The serialized array
     */
    public static String serializeArray(String[] values) {
        if (values.length == 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(values[i]);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes an annotation with attributes.
     *
     * @param annotationName The fully qualified name of the annotation
     * @param attributes The array of serialized attribute name-value pairs
     * @return The serialized annotation
     */
    public static String serializeAnnotationWithAttributes(String annotationName, String[] attributes) {
        if (attributes.length == 0) {
            return serializeSimpleAnnotation(annotationName);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(annotationName.replace('.', '/')).append("(");
        for (int i = 0; i < attributes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(attributes[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Serializes an annotation attribute.
     *
     * @param name The name of the attribute
     * @param value The serialized value of the attribute
     * @return The serialized attribute
     */
    public static String serializeAttribute(String name, String value) {
        return name + "=" + value;
    }

    static String serializeValue(Object value) {
        return serializeValueInternal(value, false);
    }

    /**
     * Converts annotation values to their string representation for TypeTable serialization.
     * This method handles the specific format requirements for TypeTable, including
     * delimiter escaping and compact array formatting.
     *
     * @param value The annotation value to convert
     * @return The serialized string representation
     */
    public static String convertAnnotationValueToString(Object value) {
        return serializeValueInternal(value, true);
    }

    /**
     * Internal method for serializing values with options for different formatting styles.
     *
     * @param value The value to serialize
     * @param typeTableFormat If true, uses TypeTable format (delimiter escaping, compact arrays)
     * @return The serialized string representation
     */
    private static String serializeValueInternal(Object value, boolean typeTableFormat) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            if (typeTableFormat) {
                return "\"" + escapeDelimiters(value.toString()) + "\"";
            } else {
                return serializeString((String) value);
            }
        } else if (value instanceof Type) {
            if (typeTableFormat) {
                return ((Type) value).getDescriptor();
            } else {
                return serializeClassConstant((Type) value);
            }
        } else if (value.getClass().isArray()) {
            // Handle primitive arrays and object arrays
            List<String> elements = new ArrayList<>();
            if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                for (Object element : array) {
                    elements.add(serializeValueInternal(element, typeTableFormat));
                }
            } else {
                // Handle primitive arrays
                int length = java.lang.reflect.Array.getLength(value);
                for (int i = 0; i < length; i++) {
                    Object element = java.lang.reflect.Array.get(value, i);
                    elements.add(serializeValueInternal(element, typeTableFormat));
                }
            }
            if (typeTableFormat) {
                return "{" + String.join(",", elements) + "}";
            } else {
                return serializeArray(elements.toArray(new String[0]));
            }
        } else if (value instanceof Boolean) {
            return serializeBoolean((Boolean) value);
        } else if (value instanceof Character) {
            return serializeChar((Character) value);
        } else if (value instanceof Number) {
            if (value instanceof Long) {
                return serializeLong((Long) value);
            } else if (value instanceof Float) {
                return serializeFloat((Float) value);
            } else if (value instanceof Double) {
                return serializeDouble((Double) value);
            } else {
                return serializeNumber((Number) value);
            }
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Escapes delimiter characters for TypeTable serialization.
     * This is specifically used for string values in annotation default values.
     *
     * @param value The string value to escape
     * @return The escaped string value, or null if input is null
     */
    public static String escapeDelimiters(String value) {
        if (value == null) return null;
        return value.replace("\\", "\\\\")  // Escape backslashes first
                .replace("|", "\\|")    // Escape pipes
                .replace("\"", "\\\""); // Escape quotes
    }

    /**
     * Processes an annotation default value and applies it to the provided annotation visitor.
     * This method handles different types of annotation values including arrays, class constants,
     * enum constants, and field constants.
     *
     * @param annotationDefaultVisitor The annotation visitor to apply the default value to
     * @param defaultValueStr The string representation of the default value
     */
    public static void processAnnotationDefaultValue(AnnotationVisitor annotationDefaultVisitor, Object value) {
        if (annotationDefaultVisitor == null || value == null) {
            return;
        }

        if (value.getClass().isArray()) {
            AnnotationVisitor annotationVisitor = annotationDefaultVisitor.visitArray(null);
            for (Object v : ((Object[]) value)) {
                processAnnotationDefaultValue(annotationVisitor, v);
            }
            annotationVisitor.visitEnd();
        } else if (value instanceof AnnotationDeserializer.ClassConstant) {
            annotationDefaultVisitor.visit(null, Type.getType(((AnnotationDeserializer.ClassConstant) value).getDescriptor()));
        } else if (value instanceof AnnotationDeserializer.EnumConstant) {
            AnnotationDeserializer.EnumConstant enumConstant = (AnnotationDeserializer.EnumConstant) value;
            annotationDefaultVisitor.visitEnum(null, enumConstant.getEnumDescriptor(), enumConstant.getConstantName());
        } else if (value instanceof AnnotationDeserializer.FieldConstant) {
            AnnotationDeserializer.FieldConstant fieldConstant = (AnnotationDeserializer.FieldConstant) value;
            annotationDefaultVisitor.visitEnum(null, fieldConstant.getClassName(), fieldConstant.getFieldName());
        } else if (value instanceof AnnotationDeserializer.AnnotationInfo) {
            AnnotationDeserializer.AnnotationInfo annotationInfo = (AnnotationDeserializer.AnnotationInfo) value;
            AnnotationVisitor annotationVisitor = annotationDefaultVisitor.visitAnnotation(null, 'L' + annotationInfo.getName().replace('.', '/') + ';');
            if (annotationInfo.getAttributes() != null) {
                annotationInfo.getAttributes().forEach(attribute -> {
                    // FIXME call correct visit method
                    annotationVisitor.visit(attribute.getName(), attribute.getValue());
                });
            }
            annotationVisitor.visitEnd();
        } else {
            annotationDefaultVisitor.visit(null, value);
        }
    }
}
