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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to create reusable annotation visitors, eliminating code duplication.
 */
class AnnotationCollectorHelper {

    static AnnotationVisitor createCollector(String descriptor, List<String> collectedAnnotations) {
        String annotationName = Type.getType(descriptor).getClassName();
        String baseAnnotation = AnnotationSerializer.serializeSimpleAnnotation(annotationName);

        return new AnnotationVisitor(Opcodes.ASM9) {
            private final List<String> attributes = new ArrayList<>();

            @Override
            public void visit(String name, Object value) {
                String attributeName = name == null ? "value" : name;
                String attributeValue = AnnotationSerializer.serializeValue(value);
                attributes.add(AnnotationSerializer.serializeAttribute(attributeName, attributeValue));
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                String attributeName = name == null ? "value" : name;
                String enumType = Type.getType(descriptor).getClassName();
                String attributeValue = AnnotationSerializer.serializeEnumConstant(enumType, value);
                attributes.add(AnnotationSerializer.serializeAttribute(attributeName, attributeValue));
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                // For nested annotations, we'll just collect the name for now
                // In a more complete implementation, we would recursively collect details
                String attributeName = name == null ? "value" : name;
                String nestedAnnotationName = Type.getType(descriptor).getClassName();
                String attributeValue = AnnotationSerializer.serializeSimpleAnnotation(nestedAnnotationName);
                attributes.add(AnnotationSerializer.serializeAttribute(attributeName, attributeValue));
                return null;
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                String attributeName = name == null ? "value" : name;
                List<String> arrayElements = new ArrayList<>();

                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String name, Object value) {
                        String elementValue = AnnotationSerializer.serializeValue(value);
                        arrayElements.add(elementValue);
                    }

                    @Override
                    public void visitEnum(String name, String descriptor, String value) {
                        String enumType = Type.getType(descriptor).getClassName();
                        String elementValue = AnnotationSerializer.serializeEnumConstant(enumType, value);
                        arrayElements.add(elementValue);
                    }

                    @Override
                    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                        // For nested annotations in arrays, we'll just collect the name for now
                        String nestedAnnotationName = Type.getType(descriptor).getClassName();
                        String elementValue = AnnotationSerializer.serializeSimpleAnnotation(nestedAnnotationName);
                        arrayElements.add(elementValue);
                        return null;
                    }

                    @Override
                    public AnnotationVisitor visitArray(String name) {
                        // For nested arrays, we'll just add an empty array for now
                        arrayElements.add("{}");
                        return null;
                    }

                    @Override
                    public void visitEnd() {
                        String arrayValue = AnnotationSerializer.serializeArray(arrayElements.toArray(new String[0]));
                        attributes.add(AnnotationSerializer.serializeAttribute(attributeName, arrayValue));
                    }
                };
            }

            @Override
            public void visitEnd() {
                if (attributes.isEmpty()) {
                    collectedAnnotations.add(baseAnnotation);
                } else {
                    String annotationWithAttributes = AnnotationSerializer.serializeAnnotationWithAttributes(
                            annotationName,
                            attributes.toArray(new String[0])
                    );
                    collectedAnnotations.add(annotationWithAttributes);
                }
            }
        };
    }
}
