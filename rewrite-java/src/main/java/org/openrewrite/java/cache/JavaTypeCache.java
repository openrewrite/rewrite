/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cache;

import org.openrewrite.java.tree.JavaType;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public interface JavaTypeCache {
    void clear();

    JavaType.Class computeClass(Path classpathElement, String fullyQualifiedName, Supplier<JavaType.Class> fq);

    JavaType.GenericTypeVariable computeGeneric(Path classpathElement, String name, String fullyQualifiedName, Supplier<JavaType.GenericTypeVariable> g);

    JavaType.Method computeMethod(Path classpathElement, String fullyQualifiedName, String method, String resolvedReturnType, String resolvedArgumentTypeSignatures, Supplier<JavaType.Method> m);

    JavaType.Parameterized computeParameterized(Path classpathElement, String fullyQualifiedName, String typeVariableSignatures, Supplier<JavaType.Parameterized> m);

    JavaType.Variable computeVariable(Path classpathElement, String fullyQualifiedName, String variable, Supplier<JavaType.Variable> v);
}
