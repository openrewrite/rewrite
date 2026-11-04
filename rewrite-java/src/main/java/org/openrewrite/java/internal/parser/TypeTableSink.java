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

import java.util.List;

/**
 * Receives type metadata extracted from class files by {@link TypeTable.Writer}.
 * <p>
 * Data is passed as directly as possible from ASM:
 * <ul>
 *   <li>Signatures: raw JVMS strings (use {@link TypeSignature#parseClassSignature} if structured data needed)</li>
 *   <li>Annotations: structured {@link AnnotationDeserializer.AnnotationInfo} objects (no string serialization)</li>
 *   <li>Descriptors: raw JVM descriptor strings</li>
 * </ul>
 */
public interface TypeTableSink {

    void visitClass(String groupId, String artifactId, String version,
                    int classAccess, String className,
                    @Nullable String classSignature,
                    @Nullable String superName,
                    String @Nullable [] interfaces,
                    List<AnnotationDeserializer.AnnotationInfo> annotations);

    void visitMethod(int access, String name, String descriptor,
                     @Nullable String signature,
                     List<String> parameterNames,
                     String @Nullable [] exceptions,
                     List<AnnotationDeserializer.AnnotationInfo> annotations,
                     @Nullable String constantValue);

    void visitField(int access, String name, String descriptor,
                    @Nullable String signature,
                    List<AnnotationDeserializer.AnnotationInfo> annotations,
                    @Nullable String constantValue);

    void visitEndClass();
}
