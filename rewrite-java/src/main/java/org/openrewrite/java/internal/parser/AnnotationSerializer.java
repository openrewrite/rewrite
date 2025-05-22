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

import org.objectweb.asm.Type;

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
     * @param className The fully qualified name of the class
     * @return The serialized class constant
     */
    public static String serializeClassConstant(String className) {
        return className.replace('.', '/') + ".class";
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
     * @param enumType The fully qualified name of the enum type
     * @param enumConstant The name of the enum constant
     * @return The serialized enum constant
     */
    public static String serializeEnumConstant(String enumType, String enumConstant) {
        return enumType.replace('.', '/') + "." + enumConstant;
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
                sb.append(", ");
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

    /**
     * Escapes special characters in a string for use in a TSV file.
     * This includes escaping pipe characters (|) and tab characters.
     *
     * @param value The string to escape
     * @return The escaped string
     */
    public static String escapeSpecialCharacters(String value) {
        return value.replace("|", "\\|").replace("\t", "\\t");
    }

    static String serializeValue(Object value) {
        if (value instanceof String) {
            return serializeString((String) value);
        } else if (value instanceof Type) {
            return serializeClassConstant(((Type) value).getClassName());
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
}
