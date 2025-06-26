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

/**
 * Functional interface for creating an AnnotationVisitor.
 */
@FunctionalInterface
interface AnnotationVisitorCreator {
    @Nullable
    AnnotationVisitor create(String descriptor, boolean visible);
}

/**
 * Helper class for applying annotations to classes, methods, and fields using ASM.
 */
class AnnotationApplier {

    /**
     * Applies an annotation to a class, method, or field.
     *
     * @param annotationStr   The serialized annotation string
     * @param visitAnnotation A function that creates an AnnotationVisitor
     */
    public static void applyAnnotation(String annotationStr, AnnotationVisitorCreator visitAnnotation) {
        if (!annotationStr.startsWith("@")) {
            return;
        }

        AnnotationDeserializer.AnnotationInfo annotationInfo = AnnotationDeserializer.parseAnnotation(annotationStr);
        AnnotationVisitor av = visitAnnotation.create(annotationInfo.getDescriptor(), true);
        
        if (av != null) {
            AnnotationAttributeApplier.applyAttributes(av, annotationInfo.getAttributes());
            av.visitEnd();
        }
    }
}

/**
 * Handles the application of annotation attributes to ASM AnnotationVisitors.
 * Centralizes the value handling logic that was previously duplicated.
 */
class AnnotationAttributeApplier {

    /**
     * Applies a list of attributes to an annotation visitor.
     */
    public static void applyAttributes(AnnotationVisitor av, @Nullable List<AnnotationDeserializer.AttributeInfo> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        for (AnnotationDeserializer.AttributeInfo attribute : attributes) {
            applyAttribute(av, attribute.getName(), attribute.getValue());
        }
    }

    /**
     * Applies a single attribute to an annotation visitor.
     */
    private static void applyAttribute(AnnotationVisitor av, String attributeName, String attributeValue) {
        Object parsedValue = AnnotationDeserializer.parseValue(attributeValue);
        applyParsedValue(av, attributeName, parsedValue);
    }

    /**
     * Applies a parsed value to an annotation visitor.
     * This method centralizes all the value handling logic that was previously duplicated.
     */
    public static void applyParsedValue(AnnotationVisitor av, @Nullable String attributeName, Object parsedValue) {
        if (parsedValue instanceof Boolean || parsedValue instanceof Character || parsedValue instanceof Number) {
            av.visit(attributeName, parsedValue);
        } else if (parsedValue instanceof String) {
            String stringValue = (String) parsedValue;
            if (stringValue.startsWith("\"") && stringValue.endsWith("\"")) {
                stringValue = stringValue.substring(1, stringValue.length() - 1);
            }
            av.visit(attributeName, stringValue);
        } else if (parsedValue instanceof AnnotationDeserializer.ClassConstant) {
            String classDescriptor = ((AnnotationDeserializer.ClassConstant) parsedValue).getDescriptor();
            av.visit(attributeName, Type.getType(classDescriptor));
        } else if (parsedValue instanceof AnnotationDeserializer.EnumConstant) {
            AnnotationDeserializer.EnumConstant enumConstant = (AnnotationDeserializer.EnumConstant) parsedValue;
            av.visitEnum(attributeName, enumConstant.getEnumDescriptor(), enumConstant.getConstantName());
        } else if (parsedValue instanceof AnnotationDeserializer.AnnotationInfo) {
            applyNestedAnnotation(av, attributeName, (AnnotationDeserializer.AnnotationInfo) parsedValue);
        } else if (parsedValue instanceof Object[]) {
            applyArrayAttribute(av, attributeName, (Object[]) parsedValue);
        }
    }

    /**
     * Applies a nested annotation to an annotation visitor.
     */
    private static void applyNestedAnnotation(AnnotationVisitor av, @Nullable String attributeName, 
                                            AnnotationDeserializer.AnnotationInfo nestedAnnotationInfo) {
        AnnotationVisitor nestedAv = av.visitAnnotation(attributeName, nestedAnnotationInfo.getDescriptor());
        
        if (nestedAv != null) {
            applyAttributes(nestedAv, nestedAnnotationInfo.getAttributes());
            nestedAv.visitEnd();
        }
    }

    /**
     * Applies an array attribute to an annotation visitor.
     */
    private static void applyArrayAttribute(AnnotationVisitor av, @Nullable String attributeName, Object[] arrayValues) {
        AnnotationVisitor arrayVisitor = av.visitArray(attributeName);
        
        if (arrayVisitor != null) {
            for (Object arrayValue : arrayValues) {
                applyParsedValue(arrayVisitor, null, arrayValue);
            }
            arrayVisitor.visitEnd();
        }
    }
}

/**
 * Helper class to create reusable annotation visitors for collecting annotation data.
 */
class AnnotationCollectorHelper {

    /**
     * Creates an annotation visitor that collects annotation values into a serialized string format.
     */
    static AnnotationVisitor createCollector(String annotationDescriptor, List<String> collectedAnnotations) {
        return new AnnotationValueCollector(result -> {
            String serializedAnnotation = result.isEmpty() 
                ? AnnotationSerializer.serializeSimpleAnnotation(annotationDescriptor)
                : AnnotationSerializer.serializeAnnotationWithAttributes(annotationDescriptor, result.toArray(new String[0]));
            collectedAnnotations.add(serializedAnnotation);
        });
    }

    /**
     * A reusable annotation visitor that collects annotation values.
     */
    static class AnnotationValueCollector extends AnnotationVisitor {
        private final ResultCallback callback;
        private final List<String> collectedValues = new ArrayList<>();

        AnnotationValueCollector(ResultCallback callback) {
            super(Opcodes.ASM9);
            this.callback = callback;
        }

        @Override
        public void visit(@Nullable String name, Object value) {
            String serializedValue = AnnotationSerializer.serializeValue(value);
            addCollectedValue(name, serializedValue);
        }

        @Override
        public void visitEnum(@Nullable String name, String descriptor, String value) {
            String serializedValue = AnnotationSerializer.serializeEnumConstant(descriptor, value);
            addCollectedValue(name, serializedValue);
        }

        @Override
        public AnnotationVisitor visitAnnotation(@Nullable String name, String nestedAnnotationDescriptor) {
            return new AnnotationValueCollector(result -> {
                String nestedAnnotation = result.isEmpty()
                    ? AnnotationSerializer.serializeSimpleAnnotation(nestedAnnotationDescriptor)
                    : AnnotationSerializer.serializeAnnotationWithAttributes(nestedAnnotationDescriptor, result.toArray(new String[0]));
                addCollectedValue(name, nestedAnnotation);
            });
        }

        @Override
        public AnnotationVisitor visitArray(@Nullable String name) {
            return new ArrayValueCollector(arrayValues -> {
                String arrayValue = AnnotationSerializer.serializeArray(arrayValues.toArray(new String[0]));
                addCollectedValue(name, arrayValue);
            });
        }

        @Override
        public void visitEnd() {
            callback.onResult(collectedValues);
        }

        private void addCollectedValue(@Nullable String name, String value) {
            if (name == null) {
                collectedValues.add(value);
            } else {
                collectedValues.add(AnnotationSerializer.serializeAttribute(name, value));
            }
        }
    }

    /**
     * Specialized collector for array values.
     */
    static class ArrayValueCollector extends AnnotationVisitor {
        private final List<String> arrayValues = new ArrayList<>();
        private final ResultCallback callback;

        ArrayValueCollector(ResultCallback callback) {
            super(Opcodes.ASM9);
            this.callback = callback;
        }

        @Override
        public void visit(@Nullable String name, Object value) {
            arrayValues.add(AnnotationSerializer.serializeValue(value));
        }

        @Override
        public void visitEnum(@Nullable String name, String descriptor, String value) {
            arrayValues.add(AnnotationSerializer.serializeEnumConstant(descriptor, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(@Nullable String name, String descriptor) {
            return new AnnotationValueCollector(result -> {
                String annotation = result.isEmpty()
                    ? AnnotationSerializer.serializeSimpleAnnotation(descriptor)
                    : AnnotationSerializer.serializeAnnotationWithAttributes(descriptor, result.toArray(new String[0]));
                arrayValues.add(annotation);
            });
        }

        @Override
        public void visitEnd() {
            callback.onResult(arrayValues);
        }
    }

    /**
     * Callback interface for receiving collection results.
     */
    @FunctionalInterface
    interface ResultCallback {
        void onResult(List<String> result);
    }
}

/**
 * Deserializes annotation strings from the TypeTable format back into Java objects.
 */
class AnnotationDeserializer {

    private static final int MAX_NESTING_DEPTH = 32;
    
    // JVM type descriptors: V=void, Z=boolean, C=char, B=byte, S=short, I=int, F=float, J=long, D=double
    private static final String JVM_PRIMITIVE_DESCRIPTORS = "VZCBSIFJD";

    /**
     * Parses a serialized annotation string.
     */
    public static AnnotationInfo parseAnnotation(String annotationStr) {
        if (!annotationStr.startsWith("@")) {
            throw new IllegalArgumentException("Invalid annotation format: " + annotationStr);
        }

        int parenIndex = annotationStr.indexOf('(');
        if (parenIndex == -1) {
            String annotationName = annotationStr.substring(1);
            return new AnnotationInfo(annotationName, null);
        }

        String annotationName = annotationStr.substring(1, parenIndex);
        String attributesStr = annotationStr.substring(parenIndex + 1, annotationStr.length() - 1);
        List<AttributeInfo> attributes = parseAttributes(attributesStr);
        
        return new AnnotationInfo(annotationName, attributes);
    }

    /**
     * Parses a string containing serialized attributes.
     */
    private static List<AttributeInfo> parseAttributes(String attributesStr) {
        List<AttributeInfo> attributes = new ArrayList<>();
        if (attributesStr.isEmpty()) {
            return attributes;
        }

        List<String> attributeStrings = splitRespectingNestedStructures(attributesStr, ',');
        for (String attributeStr : attributeStrings) {
            int equalsIndex = attributeStr.indexOf('=');
            if (equalsIndex == -1) {
                attributes.add(new AttributeInfo("value", attributeStr.trim()));
            } else {
                String name = attributeStr.substring(0, equalsIndex).trim();
                String value = attributeStr.substring(equalsIndex + 1).trim();
                attributes.add(new AttributeInfo(name, value));
            }
        }

        return attributes;
    }

    /**
     * Splits a string by a delimiter, respecting nested structures.
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
                switch (c) {
                    case '(':
                        parenDepth++;
                        break;
                    case ')':
                        parenDepth--;
                        break;
                    case '{':
                        braceDepth++;
                        break;
                    case '}':
                        braceDepth--;
                        break;
                    default:
                        if (c == delimiter && parenDepth == 0 && braceDepth == 0) {
                            result.add(str.substring(start, i).trim());
                            start = i + 1;
                        }
                }
            }
        }

        if (start < str.length()) {
            result.add(str.substring(start).trim());
        }

        return result;
    }

    /**
     * Determines the type of a serialized value and returns it in the appropriate format.
     */
    public static Object parseValue(String value) {
        return parseValue(value, 0);
    }

    private static Object parseValue(String value, int depth) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new IllegalArgumentException("Maximum nesting depth of " + MAX_NESTING_DEPTH + " exceeded while parsing: " + value);
        }

        value = value.trim();

        // Handle different value types in order of specificity
        if (isBoolean(value)) return Boolean.parseBoolean(value);
        if (isCharLiteral(value)) return parseCharValue(value);
        if (isStringLiteral(value)) return parseStringValue(value);
        if (isArrayLiteral(value)) return parseArrayValue(value, depth + 1);
        if (isClassConstant(value)) return parseClassConstant(value);
        if (isEnumConstant(value)) return parseEnumConstant(value);
        if (isPrimitiveTypeDescriptor(value)) return new ClassConstant(value);
        if (isArrayTypeDescriptor(value)) return new ClassConstant(value);
        if (isAnnotation(value)) return parseAnnotation(value);
        
        // Try parsing as numeric, fallback to string
        return parseNumericValue(value);
    }

    // Type checking helper methods
    private static boolean isBoolean(String value) {
        return "true".equals(value) || "false".equals(value);
    }

    private static boolean isCharLiteral(String value) {
        return value.startsWith("'") && value.endsWith("'");
    }

    private static boolean isStringLiteral(String value) {
        return value.startsWith("\"") && value.endsWith("\"");
    }

    private static boolean isArrayLiteral(String value) {
        return value.startsWith("{") && value.endsWith("}");
    }

    private static boolean isClassConstant(String value) {
        return value.startsWith("L") && value.endsWith(";");
    }

    private static boolean isEnumConstant(String value) {
        return value.startsWith("L") && value.contains(";") && !value.endsWith(";");
    }

    private static boolean isPrimitiveTypeDescriptor(String value) {
        return value.length() == 1 && JVM_PRIMITIVE_DESCRIPTORS.contains(value);
    }

    private static boolean isArrayTypeDescriptor(String value) {
        return value.startsWith("[");
    }

    private static boolean isAnnotation(String value) {
        return value.startsWith("@");
    }

    // Value parsing helper methods
    private static Object parseClassConstant(String value) {
        return new ClassConstant(value);
    }

    private static Object parseEnumConstant(String value) {
        int semicolonIndex = value.indexOf(';');
        return new EnumConstant(
            value.substring(0, semicolonIndex + 1), 
            value.substring(semicolonIndex + 1)
        );
    }

    private static Object parseNumericValue(String value) {
        try {
            if (value.endsWith("L") || value.endsWith("l")) {
                return Long.parseLong(value.substring(0, value.length() - 1));
            }
            if (value.endsWith("F") || value.endsWith("f")) {
                return Float.parseFloat(value.substring(0, value.length() - 1));
            }
            if (value.endsWith("D") || value.endsWith("d") || value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static char parseCharValue(String value) {
        String charContent = value.substring(1, value.length() - 1);
        
        if (charContent.length() == 1) {
            return charContent.charAt(0);
        } else if (charContent.startsWith("\\") && charContent.length() == 2) {
            return parseCharEscapeSequence(charContent.charAt(1));
        } else {
            throw new IllegalArgumentException(
                "Invalid character literal '" + value + "'. Expected format: 'c' or escape sequence like '\\n'"
            );
        }
    }

    private static char parseCharEscapeSequence(char escapeChar) {
        switch (escapeChar) {
            case '\'': return '\'';
            case '\\': return '\\';
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
            default: 
                // Allow other escape sequences to pass through (like unicode escapes)
                return escapeChar;
        }
    }

    private static String parseStringValue(String value) {
        String stringContent = value.substring(1, value.length() - 1);
        return processStringEscapes(stringContent);
    }

    /**
     * Processes escape sequences in string content.
     * Handles standard escapes (\", \\, \n, \r, \t) and TypeTable-specific escapes (\|).
     */
    private static String processStringEscapes(String content) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escaped) {
                switch (c) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case '|': sb.append('|'); break; // TypeTable-specific escape
                    default: sb.append(c); // Pass through unknown escapes
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

    private static Object[] parseArrayValue(String value, int depth) {
        String arrayContent = value.substring(1, value.length() - 1).trim();
        
        if (arrayContent.isEmpty()) {
            return new Object[0];
        }

        List<String> elements = splitRespectingNestedStructures(arrayContent, ',');
        Object[] result = new Object[elements.size()];

        for (int i = 0; i < elements.size(); i++) {
            result[i] = parseValue(elements.get(i), depth);
        }

        return result;
    }

    // Value classes for different types of constants
    public static class AnnotationInfo {
        private final String descriptor;
        private final @Nullable List<AttributeInfo> attributes;

        public AnnotationInfo(String descriptor, @Nullable List<AttributeInfo> attributes) {
            this.descriptor = descriptor;
            this.attributes = attributes;
        }

        public String getDescriptor() { return descriptor; }
        public @Nullable List<AttributeInfo> getAttributes() { return attributes; }
    }

    public static class AttributeInfo {
        private final String name;
        private final String value;

        public AttributeInfo(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public String getValue() { return value; }
    }

    public static class ClassConstant {
        private final String descriptor;

        public ClassConstant(String descriptor) { this.descriptor = descriptor; }
        public String getDescriptor() { return descriptor; }
    }

    public static class EnumConstant {
        private final String enumDescriptor;
        private final String constantName;

        public EnumConstant(String enumDescriptor, String constantName) {
            this.enumDescriptor = enumDescriptor;
            this.constantName = constantName;
        }

        public String getEnumDescriptor() { return enumDescriptor; }
        public String getConstantName() { return constantName; }
    }

    public static class FieldConstant {
        private final String className;
        private final String fieldName;

        public FieldConstant(String className, String fieldName) {
            this.className = className;
            this.fieldName = fieldName;
        }

        public String getClassName() { return className; }
        public String getFieldName() { return fieldName; }
    }
}

/**
 * Utility methods for handling TSV escaping in annotation contexts.
 */
class TsvEscapeUtils {
    
    /**
     * Splits a string by the given delimiter, respecting escape sequences and returning unescaped values.
     * TSV escaping is only for transport - the results are unescaped to get back original values.
     * 
     * @param input the input string to split
     * @param delimiter the delimiter character to split on
     * @return array of unescaped string segments
     */
    @org.jetbrains.annotations.VisibleForTesting
    static String[] splitAnnotationList(String input, char delimiter) {
        if (input.isEmpty()) return new String[0];
        
        List<String> result = new ArrayList<>();
        StringBuilder current = null; // Lazy allocation
        boolean escaped = false;
        int segmentStart = 0;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (escaped) {
                // Previous character was backslash, so this character is escaped
                if (current == null) {
                    // First escape found - allocate StringBuilder and copy previous content
                    current = new StringBuilder();
                    current.append(input, segmentStart, i - 1); // Exclude the backslash
                }
                // Append the unescaped character (remove the escape)
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                // This is an escape character - don't append, just set flag
                escaped = true;
            } else if (c == delimiter) {
                // Unescaped delimiter - split here
                if (current == null) {
                    // No escapes in this segment - use substring for efficiency
                    result.add(input.substring(segmentStart, i));
                } else {
                    result.add(current.toString());
                    current = null;
                }
                segmentStart = i + 1;
            } else if (current != null) {
                // Regular character and we're building escaped content
                current.append(c);
            }
            // If current == null and it's a regular character, do nothing (will use substring later)
        }
        
        // Handle trailing backslash (malformed escape)
        if (escaped) {
            if (current == null) {
                current = new StringBuilder();
                current.append(input, segmentStart, input.length() - 1);
            }
            current.append('\\');
        }
        
        // Add the last segment
        if (current == null) {
            // No escapes in final segment - use substring
            result.add(input.substring(segmentStart));
        } else {
            result.add(current.toString());
        }
        
        return result.toArray(new String[0]);
    }
}

/**
 * Serializes Java annotations to a string format for storage in the TypeTable.
 */
class AnnotationSerializer {

    public static String serializeSimpleAnnotation(String annotationDescriptor) {
        return "@" + annotationDescriptor;
    }

    public static String serializeBoolean(boolean value) {
        return Boolean.toString(value);
    }

    public static String serializeChar(char value) {
        StringBuilder sb = new StringBuilder();
        sb.append('\'');
        switch (value) {
            case '\'': sb.append("\\'"); break;
            case '\\': sb.append("\\\\"); break;
            case '\n': sb.append("\\n"); break;
            case '\r': sb.append("\\r"); break;
            case '\t': sb.append("\\t"); break;
            default: sb.append(value);
        }
        sb.append('\'');
        return sb.toString();
    }

    public static String serializeNumber(Number value) {
        return value.toString();
    }

    public static String serializeLong(long value) {
        return value + "L";
    }

    public static String serializeFloat(float value) {
        return value + "F";
    }

    public static String serializeDouble(double value) {
        return Double.toString(value);
    }

    public static String serializeString(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '|': sb.append("\\|"); break;
                default: sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static String serializeClassConstant(Type type) {
        return type.getDescriptor();
    }

    public static String serializeEnumConstant(String enumDescriptor, String enumConstant) {
        return enumDescriptor + enumConstant;
    }

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

    public static String serializeAnnotationWithAttributes(String annotationDescriptor, String[] attributes) {
        if (attributes.length == 0) {
            return serializeSimpleAnnotation(annotationDescriptor);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(annotationDescriptor).append("(");
        for (int i = 0; i < attributes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(attributes[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    public static String serializeAttribute(String name, String value) {
        return name + "=" + value;
    }

    static String serializeValue(Object value) {
        return serializeValueInternal(value, false);
    }

    public static String convertAnnotationValueToString(Object value) {
        return serializeValueInternal(value, true);
    }

    private static String serializeValueInternal(@Nullable Object value, boolean typeTableFormat) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return typeTableFormat ? 
                "\"" + escapeDelimiters(value.toString()) + "\"" : 
                serializeString((String) value);
        } else if (value instanceof Type) {
            return typeTableFormat ? 
                ((Type) value).getDescriptor() : 
                serializeClassConstant((Type) value);
        } else if (value.getClass().isArray()) {
            return serializeArrayValue(value, typeTableFormat);
        } else if (value instanceof Boolean) {
            return serializeBoolean((Boolean) value);
        } else if (value instanceof Character) {
            return serializeChar((Character) value);
        } else if (value instanceof Number) {
            return serializeNumericValue((Number) value);
        } else {
            return String.valueOf(value);
        }
    }

    private static String serializeArrayValue(Object value, boolean typeTableFormat) {
        List<String> elements = new ArrayList<>();
        
        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            for (Object element : array) {
                elements.add(serializeValueInternal(element, typeTableFormat));
            }
        } else {
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object element = java.lang.reflect.Array.get(value, i);
                elements.add(serializeValueInternal(element, typeTableFormat));
            }
        }
        
        return typeTableFormat ? 
            "{" + String.join(",", elements) + "}" : 
            serializeArray(elements.toArray(new String[0]));
    }

    private static String serializeNumericValue(Number value) {
        if (value instanceof Long) {
            return serializeLong((Long) value);
        } else if (value instanceof Float) {
            return serializeFloat((Float) value);
        } else if (value instanceof Double) {
            return serializeDouble((Double) value);
        } else {
            return serializeNumber(value);
        }
    }

    static String escapeDelimiters(String value) {
        return value.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\"", "\\\"");
    }

    static String unescapeDelimiters(String value) {
        return value.replace("\\|", "|")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    public static void processAnnotationDefaultValue(AnnotationVisitor annotationDefaultVisitor, Object value) {
        if (value.getClass().isArray()) {
            AnnotationVisitor arrayVisitor = annotationDefaultVisitor.visitArray(null);
            for (Object v : ((Object[]) value)) {
                processAnnotationDefaultValue(arrayVisitor, v);
            }
            arrayVisitor.visitEnd();
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
            AnnotationVisitor nestedVisitor = annotationDefaultVisitor.visitAnnotation(null, annotationInfo.getDescriptor());
            AnnotationAttributeApplier.applyAttributes(nestedVisitor, annotationInfo.getAttributes());
            nestedVisitor.visitEnd();
        } else {
            annotationDefaultVisitor.visit(null, value);
        }
    }
}
