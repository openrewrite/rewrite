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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for Kotlin metadata annotation parsing which contains binary-like data that needs proper escaping.
 */
class TypeTableKotlinMetadataTest {

    @Test
    void simpleKotlinMetadataAnnotation() {
        // Create a simplified Kotlin metadata annotation similar to what we might find
        String metadataAnnotation = "@Lkotlin/Metadata;(k=I1,d1=[s\"\\u0000\\u001e\\n\\u0002\\u0018\\u0002\\n\\u0000\"],d2=[s\"Lkotlin/Metadata\",s\"kotlin-stdlib\"])";

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(metadataAnnotation);
        assertThat(info.getDescriptor()).isEqualTo("Lkotlin/Metadata;");
        assertThat(info.getAttributes()).hasSize(3);
    }

    @Test
    void kotlinMetadataWithComplexBinaryData() {
        // Create a more complex case with longer binary-like strings
        String metadataAnnotation = "@Lkotlin/Metadata;(k=I1,d1=[s\"\\u0000\\u001e\\n\\u0002\\u0018\\u0002\\n\\u0000\\n\\u0002\\u0010\\u000e\\n\\u0002\\b\\u0002\\n\\u0002\\u0010\\u0002\"],d2=[s\"Collection\",s\"ExecutableStream\",s\"kotlin-stdlib\"])";

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(metadataAnnotation);
        assertThat(info.getDescriptor()).isEqualTo("Lkotlin/Metadata;");
        assertThat(info.getAttributes()).hasSize(3);

        // Verify attribute parsing
        assertThat(info.getAttributes().getFirst().getName()).isEqualTo("k");
        assertThat(info.getAttributes().getFirst().getValue()).isEqualTo(1);

        assertThat(info.getAttributes().get(1).getName()).isEqualTo("d1");
        Object[] d1Array = (Object[]) info.getAttributes().get(1).getValue();
        assertThat(d1Array).isNotEmpty();

        assertThat(info.getAttributes().get(2).getName()).isEqualTo("d2");
        Object[] d2Array = (Object[]) info.getAttributes().get(2).getValue();
        assertThat(d2Array).hasSize(3);
        assertThat(d2Array[0]).isEqualTo("Collection");
        assertThat(d2Array[1]).isEqualTo("ExecutableStream");
        assertThat(d2Array[2]).isEqualTo("kotlin-stdlib");
    }

    @Test
    void kotlinMetadataArrayParsingWithSpace() {
        // Test the specific case where we have space before closing paren (from the error message)
        String metadataAnnotation = "@Lkotlin/Metadata;(k=I1,d2=[s\"Collection\",s\"ExecutableStream\",s\"kotlin-stdlib\"])";

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(metadataAnnotation);
        assertThat(info.getDescriptor()).isEqualTo("Lkotlin/Metadata;");
        assertThat(info.getAttributes()).hasSize(2);
    }

    @Test
    void veryLongKotlinMetadataString() {
        // Create a test case with a very long string that might reproduce the position 3517 error
        StringBuilder longBinaryData = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longBinaryData.append("\\u").append("%04x".formatted(i % 65536));
        }

        String metadataAnnotation = "@Lkotlin/Metadata;(k=I1,d1=[s\"" + longBinaryData + "\"],d2=[s\"Collection\",s\"ExecutableStream\",s\"kotlin-stdlib\"])";

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(metadataAnnotation);
        assertThat(info.getDescriptor()).isEqualTo("Lkotlin/Metadata;");
        assertThat(info.getAttributes()).hasSize(3);
    }

    @Test
    void extremelyLongKotlinMetadataString() {
        // Create a test case with an extremely long string to reach ~3500 characters like the failing case
        StringBuilder longBinaryData = new StringBuilder();
        // Generate enough data to reach position ~3500
        for (int i = 0; i < 600; i++) {
            longBinaryData.append("\\u").append("%04x".formatted(i % 65536));
        }

        String metadataAnnotation = "@Lkotlin/Metadata;(k=I1,d1=[s\"" + longBinaryData + "\"],d2=[s\"Collection\",s\"ExecutableStream\",s\"kotlin-stdlib\"])";

        System.out.println("Annotation length: " + metadataAnnotation.length());

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(metadataAnnotation);
        assertThat(info.getDescriptor()).isEqualTo("Lkotlin/Metadata;");
        assertThat(info.getAttributes()).hasSize(3);
    }

    @Test
    void kotlinMetadataWithSpecialSequences() {
        // Test with some specific sequences that might cause parsing issues
        String problematicData = "\\u0000\\u001e\\n\\u0002\\u0018\\u0002\\n\\u0000\\n\\u0002\\u0010\\u000e\\n\\u0002\\b\\u0002\\n\\u0002\\u0010\\u0002";
        String metadataAnnotation = "@Lkotlin/Metadata;(k=I1,d1=[s\"" + problematicData + "\"],d2=[s\"Collection\",s\"ExecutableStream\",s\"junit-jupiter-api\"])";

        // Test deserialization
        AnnotationInfo info = AnnotationDeserializer.parseAnnotation(metadataAnnotation);
        assertThat(info.getDescriptor()).isEqualTo("Lkotlin/Metadata;");
        assertThat(info.getAttributes()).hasSize(3);
    }
}
