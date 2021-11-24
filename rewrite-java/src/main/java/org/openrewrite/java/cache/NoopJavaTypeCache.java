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

import java.util.function.Supplier;

public class NoopJavaTypeCache implements JavaTypeCache {
    @Override
    public void clear() {
    }

    @Override
    public JavaType.Class computeClass(String fullyQualifiedName, Supplier<JavaType.Class> fq) {
        return fq.get();
    }

    @Override
    public JavaType.GenericTypeVariable computeGeneric(String name, String fullyQualifiedName, Supplier<JavaType.GenericTypeVariable> g) {
        return g.get();
    }

    @Override
    public JavaType.Method computeMethod(String fullyQualifiedName, String methodName, String resolvedReturnType, String resolvedArgumentTypeSignatures, Supplier<JavaType.Method> m) {
        return m.get();
    }

    @Override
    public JavaType.Parameterized computeParameterized(String fullyQualifiedName, String typeVariableSignatures, Supplier<JavaType.Parameterized> p) {
        return p.get();
    }

    @Override
    public JavaType.Variable computeVariable(String fullyQualifiedName, String variableName, Supplier<JavaType.Variable> v) {
        return v.get();
    }
}
