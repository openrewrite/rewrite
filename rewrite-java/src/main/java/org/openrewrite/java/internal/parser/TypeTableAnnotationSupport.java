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

import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import java.lang.reflect.Array;
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
     * Applies a sequence of annotations to a class, method, or field.
     * Annotations can be separated by pipes or concatenated without delimiters.
     *
     * @param annotationsStr  The serialized annotations string (may contain multiple annotations)
     * @param visitAnnotation A function that creates an AnnotationVisitor
     */
    public static void applyAnnotations(String annotationsStr, AnnotationVisitorCreator visitAnnotation) {
        if (annotationsStr.isEmpty()) {
            return;
        }

        List<AnnotationDeserializer.AnnotationInfo> annotations = AnnotationDeserializer.parseAnnotations(annotationsStr);
        for (AnnotationDeserializer.AnnotationInfo annotationInfo : annotations) {
            applyAnnotation(annotationInfo, visitAnnotation);
        }
    }

    /**
     * Applies a single annotation string to a class, method, or field.
     *
     * @param annotationStr   The serialized annotation string
     * @param visitAnnotation A function that creates an AnnotationVisitor
     */
    public static void applyAnnotation(String annotationStr, AnnotationVisitorCreator visitAnnotation) {
        if (!annotationStr.startsWith("@")) {
            return;
        }

        AnnotationDeserializer.AnnotationInfo annotationInfo = AnnotationDeserializer.parseAnnotation(annotationStr);
        applyAnnotation(annotationInfo, visitAnnotation);
    }

    /**
     * Applies a parsed annotation info to a class, method, or field.
     */
    private static void applyAnnotation(AnnotationDeserializer.AnnotationInfo annotationInfo, AnnotationVisitorCreator visitAnnotation) {
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
            applyParsedValue(av, attribute.getName(), attribute.getValue());
        }
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
     * Creates an annotation visitor that collects into structured {@link AnnotationDeserializer.AnnotationInfo} objects.
     * No string serialization — values are stored as-is from ASM.
     */
    static AnnotationVisitor createStructuredCollector(String annotationDescriptor,
                                                        List<AnnotationDeserializer.AnnotationInfo> target) {
        return new StructuredAnnotationCollector(annotationDescriptor, target::add);
    }

    /**
     * Creates an annotation visitor that collects annotation values into a serialized string format.
     */
    static AnnotationVisitor createCollector(String annotationDescriptor, List<String> collectedAnnotations) {
        return new AnnotationValueCollector(result -> {
            String serializedAnnotation = result.isEmpty() ?
                    AnnotationSerializer.serializeSimpleAnnotation(annotationDescriptor) :
                    AnnotationSerializer.serializeAnnotationWithAttributes(annotationDescriptor, result.toArray(new String[0]));
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
                String nestedAnnotation = result.isEmpty() ?
                        AnnotationSerializer.serializeSimpleAnnotation(nestedAnnotationDescriptor) :
                        AnnotationSerializer.serializeAnnotationWithAttributes(nestedAnnotationDescriptor, result.toArray(new String[0]));
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
                String annotation = result.isEmpty() ?
                        AnnotationSerializer.serializeSimpleAnnotation(descriptor) :
                        AnnotationSerializer.serializeAnnotationWithAttributes(descriptor, result.toArray(new String[0]));
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

    /**
     * ASM AnnotationVisitor that collects into structured AnnotationInfo objects.
     * No string serialization — values stored as-is from ASM callbacks.
     * Allocation-minimal: reuses ArrayList, produces final AnnotationInfo only at visitEnd().
     */
    static class StructuredAnnotationCollector extends AnnotationVisitor {
        private final String descriptor;
        private final java.util.function.Consumer<AnnotationDeserializer.AnnotationInfo> callback;
        private @Nullable List<AnnotationDeserializer.AttributeInfo> attributes;

        StructuredAnnotationCollector(String descriptor,
                                       java.util.function.Consumer<AnnotationDeserializer.AnnotationInfo> callback) {
            super(Opcodes.ASM9);
            this.descriptor = descriptor;
            this.callback = callback;
        }

        @Override
        public void visit(@Nullable String name, Object value) {
            // ASM delivers: String, Byte, Boolean, Character, Short, Integer, Long, Float, Double, Type
            if (name != null) {
                addAttribute(name, value instanceof org.objectweb.asm.Type
                        ? new AnnotationDeserializer.ClassConstant(((org.objectweb.asm.Type) value).getDescriptor())
                        : value);
            }
        }

        @Override
        public void visitEnum(@Nullable String name, String enumDescriptor, String value) {
            if (name != null) {
                addAttribute(name, new AnnotationDeserializer.EnumConstant(enumDescriptor, value));
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(@Nullable String name, String nestedDescriptor) {
            return new StructuredAnnotationCollector(nestedDescriptor, nestedInfo -> {
                if (name != null) {
                    addAttribute(name, nestedInfo);
                }
            });
        }

        @Override
        public AnnotationVisitor visitArray(@Nullable String name) {
            return new StructuredArrayCollector(arrayValues -> {
                if (name != null) {
                    addAttribute(name, arrayValues);
                }
            });
        }

        @Override
        public void visitEnd() {
            callback.accept(new AnnotationDeserializer.AnnotationInfo(descriptor, attributes));
        }

        private void addAttribute(String name, Object value) {
            if (attributes == null) {
                attributes = new ArrayList<>(4);
            }
            attributes.add(new AnnotationDeserializer.AttributeInfo(name, value));
        }
    }

    /**
     * Collects array element values for structured annotation collection.
     */
    static class StructuredArrayCollector extends AnnotationVisitor {
        private final java.util.function.Consumer<Object[]> callback;
        private final List<Object> values = new ArrayList<>(4);

        StructuredArrayCollector(java.util.function.Consumer<Object[]> callback) {
            super(Opcodes.ASM9);
            this.callback = callback;
        }

        @Override
        public void visit(@Nullable String name, Object value) {
            values.add(value instanceof org.objectweb.asm.Type
                    ? new AnnotationDeserializer.ClassConstant(((org.objectweb.asm.Type) value).getDescriptor())
                    : value);
        }

        @Override
        public void visitEnum(@Nullable String name, String enumDescriptor, String value) {
            values.add(new AnnotationDeserializer.EnumConstant(enumDescriptor, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(@Nullable String name, String descriptor) {
            return new StructuredAnnotationCollector(descriptor, values::add);
        }

        @Override
        public void visitEnd() {
            callback.accept(values.toArray());
        }
    }
}

/**
 * Deserializes annotation strings from the TypeTable format back into Java objects.
 */
class TsvEscapeUtils {

    /**
     * Splits a string by the given delimiter, respecting escape sequences and only unescaping \| sequences.
     * Other escape sequences are preserved as-is since they're part of the content format.
     *
     * @param input the input string to split
     * @param delimiter the delimiter character to split on
     * @return array of string segments with \| unescaped to |
     */
    @VisibleForTesting
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
                    current.append(input, segmentStart, i - 1); // Exclude the backslash from previous content
                }

                if (c == '|') {
                    // Unescape \| to just |
                    current.append('|');
                } else {
                    // For all other escapes, preserve the backslash and character
                    current.append('\\').append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                // This is an escape character - set flag but don't append yet
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
        return "Z" + value;
    }

    public static String serializeChar(char value) {
        // Use numeric format for consistency with convertConstantValueWithType
        return "C" + (int) value;
    }

    public static String serializeNumber(Number value) {
        if (value instanceof Byte) {
            return "B" + value;
        } else if (value instanceof Short) {
            return "S" + value;
        } else if (value instanceof Integer) {
            return "I" + value;
        } else if (value instanceof Long) {
            return "J" + value;
        } else if (value instanceof Float) {
            return "F" + value;
        } else if (value instanceof Double) {
            return "D" + value;
        }
        return "I" + value; // Default to integer
    }

    public static String serializeLong(long value) {
        return "J" + value;
    }

    public static String serializeFloat(float value) {
        return "F" + value;
    }

    public static String serializeDouble(double value) {
        return "D" + value;
    }

    public static String serializeClassConstant(Type type) {
        return "c" + type.getDescriptor();
    }

    public static String serializeEnumConstant(String enumDescriptor, String enumConstant) {
        return "e" + enumDescriptor + "." + enumConstant;
    }

    public static String serializeArray(String[] values) {
        if (values.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(values[i]);
        }
        sb.append("]");
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
                sb.append(",");
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
        return serializeValueInternal(value);
    }

    public static String convertAnnotationValueToString(Object value) {
        return serializeValueInternal(value);
    }

    /**
     * Converts a constant value to its string representation with type prefix,
     * specifically handling primitive types that are stored as integers in the JVM.
     * This is used for field constants in TypeTable serialization.
     *
     * @param value the constant value from the bytecode
     * @param descriptor the JVM type descriptor for the field (e.g., "Z" for boolean, "C" for char)
     * @return the serialized string with appropriate type prefix
     */
    public static String convertConstantValueWithType(Object value, String descriptor) {
        // For primitive types that are stored as integers in the constant pool,
        // use the descriptor to determine the correct type prefix
        if (value instanceof Integer) {
            int intValue = (Integer) value;
            switch (descriptor) {
                case "Z": // boolean
                    return "Z" + (intValue != 0 ? "true" : "false");
                case "C": // char
                    return "C" + intValue; // Could format as C'A' but numeric is simpler
                case "B": // byte
                    return "B" + intValue;
                case "S": // short
                    return "S" + intValue;
                case "I": // int
                    return "I" + intValue;
                default:
                    // Shouldn't happen, but fall back to regular formatting
                    return convertAnnotationValueToString(value);
            }
        }
        // For other types, use the regular conversion
        return convertAnnotationValueToString(value);
    }

    private static String serializeValueInternal(@Nullable Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "s\"" + escapeStringContentForTsv((String) value) + "\"";
        } else if (value instanceof Type) {
            return serializeClassConstant((Type) value);
        } else if (value.getClass().isArray()) {
            return serializeArrayValue(value);
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

    /**
     * Escapes string content for TSV storage.
     * It escapes backslashes, control characters, and quotes using Java source code escape sequences.
     * Note: Pipe characters are NOT escaped here. Escaping pipes is only needed when the string
     * will be part of a pipe-delimited list, and that escaping is done at the point of joining.
     */
    private static String escapeStringContentForTsv(String content) {
        StringBuilder sb = null; // Lazy allocation
        int start = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            String replacement = null;

            switch (c) {
                case '\\':
                    replacement = "\\\\";
                    break;
                // Pipes are NOT escaped here - only when joining with pipes as delimiters
                case '\n':
                    replacement = "\\n";
                    break;
                case '\r':
                    replacement = "\\r";
                    break;
                case '\t':
                    replacement = "\\t";
                    break;
                case '\b':
                    replacement = "\\b";
                    break;
                case '\f':
                    replacement = "\\f";
                    break;
                case '"':
                    replacement = "\\\"";
                    break;
            }

            if (replacement != null) {
                if (sb == null) {
                    // First escape found - allocate StringBuilder and copy previous content
                    sb = new StringBuilder();
                    sb.append(content, start, i);
                }
                sb.append(replacement);
            } else if (sb != null) {
                // We're building escaped content and this is a regular character
                sb.append(c);
            }
        }

        // If no escaping was needed, return the original string
        return sb == null ? content : sb.toString();
    }

    private static String serializeArrayValue(Object value) {
        StringBuilder elements = new StringBuilder();
        elements.append('[');

        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    elements.append(',');
                }
                elements.append(serializeValueInternal(array[i]));
            }
        } else {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    elements.append(',');
                }
                elements.append(serializeValueInternal(Array.get(value, i)));
            }
        }

        elements.append(']');
        return elements.toString();
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

/**
 * Support for type annotations and parameter annotations in TypeTable TSV format.
 * Uses abbreviated codes for compact representation while maintaining javap compatibility.
 */
class TypeAnnotationSupport {

    /**
     * Format a complete type annotation for TSV.
     * Format: typeRefHex:pathString:annotation
     * Where:
     * - typeRefHex is the full typeRef value in hex (8 hex digits)
     * - pathString is the TypePath.toString() representation (empty if no path)
     * - annotation is the full JVM descriptor with values
     */
    public static String formatTypeAnnotation(int typeRef, @Nullable TypePath typePath, String annotation) {
        // Format typeRef as 8 hex digits
        String typeRefHex = String.format("%08x", typeRef);

        // Use TypePath.toString() if present, empty string otherwise
        String pathString = (typePath != null) ? typePath.toString() : "";

        return typeRefHex + ":" + pathString + ":" + annotation;
    }

    /**
     * Parse and reconstruct a type annotation from TSV format.
     */
    public static class TypeAnnotationInfo {
        public final int typeRef;
        public final @Nullable TypePath typePath;
        public final String annotation;

        private TypeAnnotationInfo(int typeRef, @Nullable TypePath typePath, String annotation) {
            this.typeRef = typeRef;
            this.typePath = typePath;
            this.annotation = annotation;
        }

        public static TypeAnnotationInfo parse(String serialized) {
            // Type annotation format: "typeRefHex:pathString:annotation"
            final int EXPECTED_PARTS = 3;
            String[] parts = serialized.split(":", EXPECTED_PARTS);
            if (parts.length != EXPECTED_PARTS) {
                throw new IllegalArgumentException("Invalid type annotation format: " + serialized);
            }

            String typeRefHex = parts[0];
            String pathString = parts[1];
            String annotation = parts[2];

            // Parse typeRef from hex (8 hex digits)
            int typeRef = (int) Long.parseLong(typeRefHex, 16);

            // Reconstruct TypePath if present
            TypePath typePath = null;
            if (!pathString.isEmpty()) {
                // Use TypePath.fromString() to parse the string representation
                typePath = TypePath.fromString(pathString);
            }

            return new TypeAnnotationInfo(typeRef, typePath, annotation);
        }
    }

}
