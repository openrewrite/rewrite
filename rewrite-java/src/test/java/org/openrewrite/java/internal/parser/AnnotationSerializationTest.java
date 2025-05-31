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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.openrewrite.java.internal.parser.AnnotationDeserializer.AnnotationInfo;
import org.openrewrite.java.internal.parser.AnnotationDeserializer.AttributeInfo;
import org.openrewrite.java.internal.parser.AnnotationDeserializer.ClassConstant;
import org.openrewrite.java.internal.parser.AnnotationDeserializer.EnumConstant;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the serialization and deserialization of annotations.
 */
class AnnotationSerializationTest {

    @Test
    void simpleAnnotation() {
        // Test serialization
        String serialized = AnnotationSerializer.serializeSimpleAnnotation("org.junit.jupiter.api.Test");
        assertThat(serialized).isEqualTo("@org/junit/jupiter/api/Test");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.junit.jupiter.api.Test");
        assertThat(info.getAttributes()).isNull();
    }

    @Test
    void annotationWithBooleanAttribute() {
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
    void annotationWithCharAttribute() {
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
    void annotationWithStringAttribute() {
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
    void annotationWithClassAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeClassConstant(Type.getType("Ljava/lang/String;"));
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
          "org.example.ClassAnnotation",
          new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@org/example/ClassAnnotation(value=Ljava/lang/String;)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.ClassAnnotation");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().get(0).getName()).isEqualTo("value");
        assertThat(info.getAttributes().get(0).getValue()).isEqualTo("Ljava/lang/String;");

        // Parse the value
        Object value = AnnotationDeserializer.parseValue(info.getAttributes().get(0).getValue());
        assertThat(value).isInstanceOf(ClassConstant.class);
        assertThat(((ClassConstant) value).getDescriptor()).isEqualTo("Ljava/lang/String;");
    }

    @Test
    void annotationWithEnumAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeEnumConstant("Ljava/time/DayOfWeek;", "MONDAY");
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
          "org.example.EnumAnnotation",
          new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@org/example/EnumAnnotation(value=Ljava/time/DayOfWeek;MONDAY)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getName()).isEqualTo("org.example.EnumAnnotation");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().get(0).getName()).isEqualTo("value");
        assertThat(info.getAttributes().get(0).getValue()).isEqualTo("Ljava/time/DayOfWeek;MONDAY");

        // Parse the value
        Object value = AnnotationDeserializer.parseValue(info.getAttributes().get(0).getValue());
        assertThat(value).isInstanceOf(EnumConstant.class);
        assertThat(((EnumConstant) value).getEnumDescriptor()).isEqualTo("Ljava/time/DayOfWeek;");
        assertThat(((EnumConstant) value).getConstantName()).isEqualTo("MONDAY");
    }

    @Test
    void annotationWithArrayAttribute() {
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
    void annotationWithMultipleAttributes() {
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
    void nestedAnnotation() {
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

    @Nested
    class SpecialCharacters {

        @Test
        void pipe() {
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

        @Test
        void tab() {
            // Test serialization with pipe character
            String attributeValue = AnnotationSerializer.serializeString("Hello\tWorld");
            String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
            String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
              "org.example.SpecialCharAnnotation",
              new String[]{attribute}
            );

            // The pipe character should be escaped
            assertThat(serialized).isEqualTo("@org/example/SpecialCharAnnotation(value=\"Hello\\tWorld\")");

            // Test deserialization
            AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
            assertThat(info.getName()).isEqualTo("org.example.SpecialCharAnnotation");
            assertThat(info.getAttributes()).hasSize(1);
            assertThat(info.getAttributes().get(0).getName()).isEqualTo("value");
            assertThat(info.getAttributes().get(0).getValue()).isEqualTo("\"Hello\\tWorld\"");

            // Parse the value and verify the pipe character is preserved
            String value = (String) AnnotationDeserializer.parseValue(info.getAttributes().get(0).getValue());
            assertThat(value).isEqualTo("Hello\tWorld");
        }
    }

    private @Nullable AttributeInfo findAttributeByName(List<AttributeInfo> attributes, String name) {
        return attributes.stream()
                .filter(attr -> attr.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Test
    void testAnnotationValueCollector() {
        // Create a list to collect the annotations
        List<String> collectedAnnotations = new ArrayList<>();

        // Create an AnnotationValueCollector
        AnnotationCollectorHelper.AnnotationValueCollector collector = 
            new AnnotationCollectorHelper.AnnotationValueCollector(
                "org.junit.jupiter.api.Test", 
                null, 
                result -> {
                    if (result.isEmpty()) {
                        collectedAnnotations.add("@org/junit/jupiter/api/Test");
                    } else {
                        String annotationWithAttributes = AnnotationSerializer.serializeAnnotationWithAttributes(
                                "org.junit.jupiter.api.Test",
                                result.toArray(new String[0])
                        );
                        collectedAnnotations.add(annotationWithAttributes);
                    }
                }
            );

        // Call visitEnd to trigger the callback
        collector.visitEnd();

        // Verify that the annotation was collected correctly
        assertThat(collectedAnnotations).hasSize(1);
        assertThat(collectedAnnotations.get(0)).isEqualTo("@org/junit/jupiter/api/Test");
    }

    @Test
    void testAnnotationValueCollectorWithAttributes() {
        // Create a list to collect the annotations
        List<String> collectedAnnotations = new ArrayList<>();

        // Create an AnnotationValueCollector
        AnnotationCollectorHelper.AnnotationValueCollector collector = 
            new AnnotationCollectorHelper.AnnotationValueCollector(
                "org.junit.jupiter.api.Test", 
                null, 
                result -> {
                    if (result.isEmpty()) {
                        collectedAnnotations.add("@org/junit/jupiter/api/Test");
                    } else {
                        String annotationWithAttributes = AnnotationSerializer.serializeAnnotationWithAttributes(
                                "org.junit.jupiter.api.Test",
                                result.toArray(new String[0])
                        );
                        collectedAnnotations.add(annotationWithAttributes);
                    }
                }
            );

        // Add an attribute
        collector.visit("timeout", 1000L);

        // Call visitEnd to trigger the callback
        collector.visitEnd();

        // Verify that the annotation was collected correctly
        assertThat(collectedAnnotations).hasSize(1);
        // With our test setup, the actual output is:
        assertThat(collectedAnnotations.get(0)).isEqualTo("@org/junit/jupiter/api/Test(timeout=1000L)");

        // Note: The actual output is "@org/junit/jupiter/api/Test(1000L)" because the name parameter
        // is only used if attributeName is not null, and we're creating the collector with attributeName=null.
        // In a real-world scenario, the AnnotationCollectorHelper.createCollector() method would set
        // attributeName to null, and the name parameter would be used in the visit() method.
    }

    @Test
    void testAnnotationValueCollectorWithNestedAnnotation() {
        // Create a list to collect the annotations
        List<String> collectedAnnotations = new ArrayList<>();

        // Create an AnnotationValueCollector
        AnnotationCollectorHelper.AnnotationValueCollector collector = 
            new AnnotationCollectorHelper.AnnotationValueCollector(
                "org.example.OuterAnnotation", 
                null, 
                result -> {
                    if (result.isEmpty()) {
                        collectedAnnotations.add("@org/example/OuterAnnotation");
                    } else {
                        String annotationWithAttributes = AnnotationSerializer.serializeAnnotationWithAttributes(
                                "org.example.OuterAnnotation",
                                result.toArray(new String[0])
                        );
                        collectedAnnotations.add(annotationWithAttributes);
                    }
                }
            );

        // Create a nested annotation visitor
        AnnotationVisitor nestedVisitor = collector.visitAnnotation("nested", "Lorg/example/InnerAnnotation;");

        // Add an attribute to the nested annotation
        nestedVisitor.visit("value", 42);

        // End the nested annotation
        nestedVisitor.visitEnd();

        // Call visitEnd to trigger the callback
        collector.visitEnd();

        // Verify that the annotation was collected correctly
        assertThat(collectedAnnotations).hasSize(1);
        // With our test setup, the actual output is:
        assertThat(collectedAnnotations.get(0)).isEqualTo("@org/example/OuterAnnotation(nested=@org/example/InnerAnnotation(value=42))");

        // Note: The actual output is missing the attribute name "nested=" because the name parameter
        // is only used if attributeName is not null, and we're creating the collector with attributeName=null.
    }

    @Test
    void testAnnotationValueCollectorWithArray() {
        // Create a list to collect the annotations
        List<String> collectedAnnotations = new ArrayList<>();

        // Create an AnnotationValueCollector
        AnnotationCollectorHelper.AnnotationValueCollector collector = 
            new AnnotationCollectorHelper.AnnotationValueCollector(
                "org.example.ArrayAnnotation", 
                null, 
                result -> {
                    if (result.isEmpty()) {
                        collectedAnnotations.add("@org/example/ArrayAnnotation");
                    } else {
                        String annotationWithAttributes = AnnotationSerializer.serializeAnnotationWithAttributes(
                                "org.example.ArrayAnnotation",
                                result.toArray(new String[0])
                        );
                        collectedAnnotations.add(annotationWithAttributes);
                    }
                }
            );

        // Create an array visitor
        AnnotationVisitor arrayVisitor = collector.visitArray("value");

        // Add elements to the array
        arrayVisitor.visit(null, 1);
        arrayVisitor.visit(null, 2);
        arrayVisitor.visit(null, 3);

        // End the array
        arrayVisitor.visitEnd();

        // Call visitEnd to trigger the callback
        collector.visitEnd();

        // Verify that the annotation was collected correctly
        assertThat(collectedAnnotations).hasSize(1);
        assertThat(collectedAnnotations.get(0)).isEqualTo("@org/example/ArrayAnnotation(value={1, 2, 3})");
    }
}
