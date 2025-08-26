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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the serialization and deserialization of annotations in TypeTable TSV format.
 */
class TypeTableAnnotationSerializationTest {

    @Test
    void simpleAnnotation() {
        // Test serialization
        String serialized = AnnotationSerializer.serializeSimpleAnnotation("Lorg/junit/jupiter/api/Test;");
        assertThat(serialized).isEqualTo("@Lorg/junit/jupiter/api/Test;");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getDescriptor()).isEqualTo("Lorg/junit/jupiter/api/Test;");
        assertThat(info.getAttributes()).isNull();
    }

    @Test
    void annotationWithBooleanAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeBoolean(true);
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
          "Lorg/example/BooleanAnnotation;",
          new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@Lorg/example/BooleanAnnotation;(value=Ztrue)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getDescriptor()).isEqualTo("Lorg/example/BooleanAnnotation;");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().getFirst().getName()).isEqualTo("value");
        assertThat(info.getAttributes().getFirst().getValue()).isEqualTo(true);
    }

    @Test
    void annotationWithCharAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeChar('c');
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
          "Lorg/example/CharAnnotation;",
          new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@Lorg/example/CharAnnotation;(value=C99)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getDescriptor()).isEqualTo("Lorg/example/CharAnnotation;");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().getFirst().getName()).isEqualTo("value");
        assertThat(info.getAttributes().getFirst().getValue()).isEqualTo('c');
    }

    @Test
    void annotationWithCharConstant38() {
        // Test the specific case that was failing: C38 which is '&'
        String serialized = "@Lorg/example/CharAnnotation;(value=C38)";

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getDescriptor()).isEqualTo("Lorg/example/CharAnnotation;");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().getFirst().getName()).isEqualTo("value");
        assertThat(info.getAttributes().getFirst().getValue()).isEqualTo('&');

        // Verify serialization produces the same format
        char ampersand = '&';
        assertThat(AnnotationSerializer.serializeChar(ampersand)).isEqualTo("C38");
    }

    @Test
    void annotationWithStringAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeValue("Hello, World!");
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
          "Lorg/example/StringAnnotation;",
          new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@Lorg/example/StringAnnotation;(value=s\"Hello, World!\")");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getDescriptor()).isEqualTo("Lorg/example/StringAnnotation;");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().getFirst().getName()).isEqualTo("value");
        assertThat(info.getAttributes().getFirst().getValue()).isEqualTo("Hello, World!");
    }

    @Test
    void annotationWithClassAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeClassConstant(Type.getType("Ljava/lang/String;"));
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
          "Lorg/example/ClassAnnotation;",
          new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@Lorg/example/ClassAnnotation;(value=cLjava/lang/String;)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getDescriptor()).isEqualTo("Lorg/example/ClassAnnotation;");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().getFirst().getName()).isEqualTo("value");
        assertThat(((AnnotationDeserializer.ClassConstant) (info.getAttributes().getFirst().getValue())).getDescriptor()).isEqualTo("Ljava/lang/String;");

        // Parse the value
        Object value = info.getAttributes().getFirst().getValue();
        assertThat(value).isInstanceOf(ClassConstant.class);
        assertThat(((ClassConstant) value).getDescriptor()).isEqualTo("Ljava/lang/String;");
    }

    @Test
    void annotationWithEnumAttribute() {
        // Test serialization
        String attributeValue = AnnotationSerializer.serializeEnumConstant("Ljava/time/DayOfWeek;", "MONDAY");
        String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
          "Lorg/example/EnumAnnotation;",
          new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@Lorg/example/EnumAnnotation;(value=eLjava/time/DayOfWeek;.MONDAY)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getDescriptor()).isEqualTo("Lorg/example/EnumAnnotation;");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().getFirst().getName()).isEqualTo("value");

        // Parse the value
        Object value = info.getAttributes().getFirst().getValue();
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
          "Lorg/example/ArrayAnnotation;",
          new String[]{attribute}
        );
        assertThat(serialized).isEqualTo("@Lorg/example/ArrayAnnotation;(value=[I1,I2,I3])");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getDescriptor()).isEqualTo("Lorg/example/ArrayAnnotation;");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().getFirst().getName()).isEqualTo("value");
        assertThat(info.getAttributes().getFirst().getValue()).isEqualTo(new Object[]{1, 2, 3});

        // Parse the value
        Object value = info.getAttributes().getFirst().getValue();
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
        String nameAttribute = AnnotationSerializer.serializeAttribute("name", AnnotationSerializer.serializeValue("test"));
        String valueAttribute = AnnotationSerializer.serializeAttribute("value", AnnotationSerializer.serializeNumber(42));
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
          "Lorg/example/MultiAttributeAnnotation;",
          new String[]{nameAttribute, valueAttribute}
        );
        assertThat(serialized).isEqualTo("@Lorg/example/MultiAttributeAnnotation;(name=s\"test\",value=I42)");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getDescriptor()).isEqualTo("Lorg/example/MultiAttributeAnnotation;");
        assertThat(info.getAttributes()).hasSize(2);

        // Find attributes by name
        AttributeInfo nameAttr = findAttributeByName(info.getAttributes(), "name");
        AttributeInfo valueAttr = findAttributeByName(info.getAttributes(), "value");

        assertThat(nameAttr).isNotNull();
        assertThat(nameAttr.getValue()).isEqualTo("test");

        assertThat(valueAttr).isNotNull();
        assertThat(valueAttr.getValue()).isEqualTo(42);
    }

    @Test
    void nestedAnnotation() {
        // Test serialization
        String innerAttributeValue = AnnotationSerializer.serializeNumber(42);
        String innerAttribute = AnnotationSerializer.serializeAttribute("value", innerAttributeValue);
        String innerAnnotation = AnnotationSerializer.serializeAnnotationWithAttributes(
          "Lorg/example/InnerAnnotation;",
          new String[]{innerAttribute}
        );

        String outerAttribute = AnnotationSerializer.serializeAttribute("nested", innerAnnotation);
        String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
          "Lorg/example/OuterAnnotation;",
          new String[]{outerAttribute}
        );

        assertThat(serialized).isEqualTo("@Lorg/example/OuterAnnotation;(nested=@Lorg/example/InnerAnnotation;(value=I42))");

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
        assertThat(info.getDescriptor()).isEqualTo("Lorg/example/OuterAnnotation;");
        assertThat(info.getAttributes()).hasSize(1);
        assertThat(info.getAttributes().getFirst().getName()).isEqualTo("nested");

        // Parse the nested annotation
        Object value = info.getAttributes().getFirst().getValue();
        assertThat(value).isInstanceOf(AnnotationInfo.class);
        AnnotationInfo nestedInfo = (AnnotationInfo) value;
        assertThat(nestedInfo.getDescriptor()).isEqualTo("Lorg/example/InnerAnnotation;");
        assertThat(nestedInfo.getAttributes()).hasSize(1);
        assertThat(nestedInfo.getAttributes().getFirst().getName()).isEqualTo("value");
        assertThat(nestedInfo.getAttributes().getFirst().getValue()).isEqualTo(42);
    }

    @Nested
    class SpecialCharacters {

        @Test
        void pipe() {
            // Test serialization with pipe character
            String attributeValue = AnnotationSerializer.serializeValue("Hello|World");
            String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
            String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
              "Lorg/example/SpecialCharAnnotation;",
              new String[]{attribute}
            );

            // The pipe character is not escaped in elementAnnotations (only in pipe-delimited fields)
            assertThat(serialized).isEqualTo("@Lorg/example/SpecialCharAnnotation;(value=s\"Hello|World\")");

            // Test deserialization
            AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
            assertThat(info.getDescriptor()).isEqualTo("Lorg/example/SpecialCharAnnotation;");
            assertThat(info.getAttributes()).hasSize(1);
            assertThat(info.getAttributes().getFirst().getName()).isEqualTo("value");
            assertThat(info.getAttributes().getFirst().getValue()).isEqualTo("Hello|World");

            // Parse the value and verify the pipe character is preserved
            String value = (String) info.getAttributes().getFirst().getValue();
            assertThat(value).isEqualTo("Hello|World");
        }

        @Test
        void tab() {
            // Test serialization with pipe character
            String attributeValue = AnnotationSerializer.serializeValue("Hello\tWorld");
            String attribute = AnnotationSerializer.serializeAttribute("value", attributeValue);
            String serialized = AnnotationSerializer.serializeAnnotationWithAttributes(
              "Lorg/example/SpecialCharAnnotation;",
              new String[]{attribute}
            );

            // The pipe character should be escaped
            assertThat(serialized).isEqualTo("@Lorg/example/SpecialCharAnnotation;(value=s\"Hello\\tWorld\")");

            // Test deserialization
            AnnotationInfo info = AnnotationDeserializer.parseAnnotation(serialized);
            assertThat(info.getDescriptor()).isEqualTo("Lorg/example/SpecialCharAnnotation;");
            assertThat(info.getAttributes()).hasSize(1);
            assertThat(info.getAttributes().getFirst().getName()).isEqualTo("value");
            assertThat(info.getAttributes().getFirst().getValue()).isEqualTo("Hello\tWorld");

            // Parse the value and verify the pipe character is preserved
            String value = (String) info.getAttributes().getFirst().getValue();
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
        assertThat(collectedAnnotations.getFirst()).isEqualTo("@org/junit/jupiter/api/Test");
    }

    @Test
    void testAnnotationValueCollectorWithAttributes() {
        // Create a list to collect the annotations
        List<String> collectedAnnotations = new ArrayList<>();

        // Create an AnnotationValueCollector
        AnnotationCollectorHelper.AnnotationValueCollector collector =
          new AnnotationCollectorHelper.AnnotationValueCollector(
            result -> {
                if (result.isEmpty()) {
                    collectedAnnotations.add("@Lorg/junit/jupiter/api/Test;");
                } else {
                    String annotationWithAttributes = AnnotationSerializer.serializeAnnotationWithAttributes(
                      "Lorg/junit/jupiter/api/Test;",
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
        assertThat(collectedAnnotations.getFirst()).isEqualTo("@Lorg/junit/jupiter/api/Test;(timeout=J1000)");

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
            result -> {
                if (result.isEmpty()) {
                    collectedAnnotations.add("@Lorg/example/OuterAnnotation;");
                } else {
                    String annotationWithAttributes = AnnotationSerializer.serializeAnnotationWithAttributes(
                      "Lorg/example/OuterAnnotation;",
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
        assertThat(collectedAnnotations.getFirst()).isEqualTo("@Lorg/example/OuterAnnotation;(nested=@Lorg/example/InnerAnnotation;(value=I42))");

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
            result -> {
                if (result.isEmpty()) {
                    collectedAnnotations.add("@Lorg/example/ArrayAnnotation;");
                } else {
                    String annotationWithAttributes = AnnotationSerializer.serializeAnnotationWithAttributes(
                      "Lorg/example/ArrayAnnotation;",
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
        assertThat(collectedAnnotations.getFirst()).isEqualTo("@Lorg/example/ArrayAnnotation;(value=[I1,I2,I3])");
    }

    @Test
    void testParserErrorPositioning() {
        // Test malformed annotation - missing closing parenthesis
        assertThatThrownBy(() -> AnnotationDeserializer.parseAnnotation("@Lorg/springframework/retry/annotation/Backoff;("))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("at position 48, but reached end of input")
          .hasMessageContaining("Input string: @Lorg/springframework/retry/annotation/Backoff;(")
          .hasMessageContaining("Position indicator:                                           ^");

        // Test malformed annotation - invalid character at specific position  
        assertThatThrownBy(() -> AnnotationDeserializer.parseAnnotation("@Lorg/example/Test;(value=I123$)"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("at position 30")
          .hasMessageContaining("but found '$'")
          .hasMessageContaining("Input string: @Lorg/example/Test;(value=I123$)")
          .hasMessageContaining("Position indicator:                         ^");

        // Test missing attribute value
        assertThatThrownBy(() -> AnnotationDeserializer.parseAnnotation("@Lorg/example/Test;(name=)"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown value format at position 25")
          .hasMessageContaining("Input string: @Lorg/example/Test;(name=)")
          .hasMessageContaining("Position indicator:                    ^");

        // Test missing attribute value
        assertThatThrownBy(() -> AnnotationDeserializer.parseAnnotation("@Lorg/example/Test;(0name=)"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Expected identifier at position 20 but found '0'")
          .hasMessageContaining("Input string: @Lorg/example/Test;(0name=)")
          .hasMessageContaining("Position indicator:               ^");
    }
}
