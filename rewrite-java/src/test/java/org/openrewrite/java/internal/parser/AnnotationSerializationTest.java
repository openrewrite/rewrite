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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.internal.parser.AnnotationDeserializer.AnnotationInfo;
import org.openrewrite.java.internal.parser.AnnotationDeserializer.AttributeInfo;
import org.openrewrite.java.internal.parser.AnnotationDeserializer.ClassConstant;
import org.openrewrite.java.internal.parser.AnnotationDeserializer.EnumConstant;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the serialization and deserialization of annotations.
 */
class AnnotationSerializationTest {

    @Test
    void testSimpleAnnotation() {
        // Test serialization
        String serialized = AnnotationSerializer.serializeSimpleAnnotation("org.junit.jupiter.api.Test");
        assertThat(serialized).isEqualTo("@org/junit/jupiter/api/Test");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.junit.jupiter.api.Test");
        assertThat(info.getAttributes()).isNull();
    }

    @Test
    void testAnnotationWithBooleanAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeBoolean(true);
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
                "org.example.BooleanAnnotation", 
                new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@org/example/BooleanAnnotation(value=true)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.BooleanAnnotation");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().get(0).getName()).isEqualTo("value");
        assertThat(info.getAttributes().get(0).getValue()).isEqualTo("true");
    }

    @Test
    void testAnnotationWithCharAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeChar('c');
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
                "org.example.CharAnnotation", 
                new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@org/example/CharAnnotation(value='c')");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.CharAnnotation");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().get(0).getName()).isEqualTo("value");
        assertThat(info.getAttributes().get(0).getValue()).isEqualTo("'c'");
    }

    @Test
    void testAnnotationWithStringAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeString("Hello, World!");
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
                "org.example.StringAnnotation", 
                new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@org/example/StringAnnotation(value=\"Hello, World!\")");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.StringAnnotation");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().get(0).getName()).isEqualTo("value");
        assertThat(info.getAttributes().get(0).getValue()).isEqualTo("\"Hello, World!\"");
    }

    @Test
    void testAnnotationWithClassAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeClassConstant("java.lang.String");
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
                "org.example.ClassAnnotation", 
                new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@org/example/ClassAnnotation(value=java/lang/String.class)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.ClassAnnotation");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().get(0).getName()).isEqualTo("value");
        assertThat(info.getAttributes().get(0).getValue()).isEqualTo("java/lang/String.class");

        // Parse the value
        Object value = AnnotationDeserializer.parseValue(info.getAttributes().get(0).getValue());
        assertThat(value).isInstanceOf(ClassConstant.class);
        assertThat(((ClassConstant) value).getClassName()).isEqualTo("java.lang.String");
    }

    @Test
    void testAnnotationWithEnumAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeEnumConstant("java.time.DayOfWeek", "MONDAY");
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
                "org.example.EnumAnnotation", 
                new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@org/example/EnumAnnotation(value=java/time/DayOfWeek.MONDAY)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.EnumAnnotation");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().get(0).getName()).isEqualTo("value");
        assertThat(info.getAttributes().get(0).getValue()).isEqualTo("java/time/DayOfWeek.MONDAY");

        // Parse the value
        Object value = AnnotationDeserializer.parseValue(info.getAttributes().get(0).getValue());
        assertThat(value).isInstanceOf(EnumConstant.class);
        assertThat(((EnumConstant) value).getEnumType()).isEqualTo("java.time.DayOfWeek");
        assertThat(((EnumConstant) value).getConstantName()).isEqualTo("MONDAY");
    }

    @Test
    void testAnnotationWithArrayAttribute() {
        // Test serialization
        String[] arrayValues = new String[]{
                AnnotationSerializer.serializeNumber(1),
                AnnotationSerializer.serializeNumber(2),
                AnnotationSerializer.serializeNumber(3)
        };
        String attributeValue = AnnotationSerializer.serializeArray(arrayValues);
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
                "org.example.ArrayAnnotation", 
                new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@org/example/ArrayAnnotation(value={1, 2, 3})");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.ArrayAnnotation");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().get(0).getName()).isEqualTo("value");
        assertThat(info.getAttributes().get(0).getValue()).isEqualTo("{1, 2, 3}");

        // Parse the value
        Object value = AnnotationDeserializer.parseValue(info.getAttributes().get(0).getValue());
        assertThat(value).isInstanceOf(Object[].class);
        Object[] array = (Object[]) value;
        assertThat(array).hasSize(3);
        assertThat(array[0]).isEqualTo(1);
        assertThat(array[1]).isEqualTo(2);
        assertThat(array[2]).isEqualTo(3);
    }

    @Test
    void testAnnotationWithMultipleAttributes() {
        // Test serialization
        String nameAttribute = AnnotationSerializer.serializeAttribute("name", AnnotationSerializer.serializeString("test"));
        String valueAttribute = AnnotationSerializer.serializeAttribute("value", AnnotationSerializer.serializeNumber(42));
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
                "org.example.MultiAttributeAnnotation", 
                new String[]{nameAttribute, valueAttribute}
        );
        assertThat(serialized).isEqualTo("@org/example/MultiAttributeAnnotation(name=\"test\", value=42)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.MultiAttributeAnnotation");
        assertThat(info.getAttributes()).hasSize(2);

        // Find attributes by name
        AttributeInfo nameAttr = findAttributeByName(info.getAttributes(), "name");
        AttributeInfo valueAttr = findAttributeByName(info.getAttributes(), "value");

        assertThat(nameAttr).isNotNull();
        assertThat(nameAttr.getValue()).isEqualTo("\"test\"");

        assertThat(valueAttr).isNotNull();
        assertThat(valueAttr.getValue()).isEqualTo("42");
    }

    @Test
    void testNestedAnnotation() {
        // Test serialization
        String innerAttributeValue = AnnotationSerializer.serializeNumber(42);
        String innerAttribute = AnnotationSerializer.serializeAttribute("value", innerAttributeValue);
        String innerAnnotation = AnnotationSerializer.serializeAnnotationWithAttributes(
                "org.example.InnerAnnotation", 
                new String[]{innerAttribute}
        );

        String outerAttribute = AnnotationSerializer.serializeAttribute("nested", innerAnnotation);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
                "org.example.OuterAnnotation", 
                new String[]{outerAttribute}
        );

        assertThat(serialized).isEqualTo("@org/example/OuterAnnotation(nested=@org/example/InnerAnnotation(value=42))");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.OuterAnnotation");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().get(0).getName()).isEqualTo("nested");
        assertThat(info.getAttributes().get(0).getValue()).isEqualTo("@org/example/InnerAnnotation(value=42)");

        // Parse the nested annotation
        Object value = AnnotationDeserializer.parseValue(info.getAttributes().get(0).getValue());
        assertThat(value).isInstanceOf(AnnotationInfo.class);
        AnnotationInfo nestedInfo = (AnnotationInfo) value;
        assertThat(nestedInfo.getName()).isEqualTo("org.example.InnerAnnotation");
        assertThat(nestedInfo.getAttributes()).hasSize(1);
        assertThat(nestedInfo.getAttributes().get(0).getName()).isEqualTo("value");
        assertThat(nestedInfo.getAttributes().get(0).getValue()).isEqualTo("42");
    }

    @Test
    void testSpecialCharacters() {
        // Test serialization with pipe character
        String attributeValue = AnnotationSerializer.serializeString("Hello|World");
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
                "org.example.SpecialCharAnnotation", 
                new String[]{attribute}
        );

        // The pipe character should be escaped
        assertThat(serialized).isEqualTo("@org/example/SpecialCharAnnotation(value=\"Hello\\|World\")");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.SpecialCharAnnotation");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().get(0).getName()).isEqualTo("value");
        assertThat(info.getAttributes().get(0).getValue()).isEqualTo("\"Hello\\|World\"");

        // Parse the value and verify the pipe character is preserved
        String value = (String) AnnotationDeserializer.parseValue(info.getAttributes().get(0).getValue());
        assertThat(value).isEqualTo("Hello|World");
    }

    private AttributeInfo findAttributeByName(List<AttributeInfo> attributes, String name) {
        return attributes.stream()
                .filter(attr -> attr.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
