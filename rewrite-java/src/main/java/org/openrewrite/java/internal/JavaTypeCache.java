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
package org.openrewrite.java.internal;

import org.openrewrite.java.tree.JavaType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Bypasses delegate for types when we know we are in the same source set as previous cache checks
 * because there is presumed to only be one type definition per fully-qualified class name.
 */
public class JavaTypeCache {
    private final Map<String, JavaType.Array> arrayCache = new HashMap<>();
    private final Map<String, JavaType.Class> classCache = new HashMap<>();
    private final Map<String, Map<String, JavaType.GenericTypeVariable>> genericCache = new HashMap<>();
    private final Map<String, Map<String, Map<String, Map<String, JavaType.Method>>>> methodCache = new HashMap<>();
    private final Map<String, Map<String, JavaType.Variable>> variableCache = new HashMap<>();
    private final Map<String, Map<String, JavaType.Parameterized>> parameterizedCache = new HashMap<>();

    public void clear() {
        arrayCache.clear();
        classCache.clear();
        methodCache.clear();
        variableCache.clear();
        genericCache.clear();
        parameterizedCache.clear();
    }

    public JavaType.Array computeArray(String fullyQualifiedName, Supplier<JavaType.Array> fq) {
        return arrayCache.computeIfAbsent(fullyQualifiedName, n -> fq.get());
    }

    public JavaType.Class computeClass(String fullyQualifiedName, Supplier<JavaType.Class> fq) {
        return classCache.computeIfAbsent(fullyQualifiedName, n -> fq.get());
    }

    public JavaType.GenericTypeVariable computeGeneric(String name, String fullyQualifiedName, Supplier<JavaType.GenericTypeVariable> g) {
        Map<String, JavaType.GenericTypeVariable> byFqn = genericCache.computeIfAbsent(name, dt -> new HashMap<>());

        JavaType.GenericTypeVariable genericTypeVariable = byFqn.get(fullyQualifiedName);
        if(genericTypeVariable != null) {
            return genericTypeVariable;
        }
        genericTypeVariable = g.get();
        byFqn.put(fullyQualifiedName, genericTypeVariable);
        return genericTypeVariable;
    }

    public JavaType.Method computeMethod(String fullyQualifiedName, String methodName,  String resolvedReturnType, String resolvedArgumentTypeSignatures, Supplier<JavaType.Method> m) {
        Map<String, JavaType.Method> byArgSignatures = methodCache
                .computeIfAbsent(fullyQualifiedName, dt -> new HashMap<>())
                .computeIfAbsent(resolvedReturnType, n -> new HashMap<>())
                .computeIfAbsent(methodName, n -> new HashMap<>());

        JavaType.Method method = byArgSignatures.get(resolvedArgumentTypeSignatures);
        if(method != null) {
            return method;
        }
        method = m.get();
        byArgSignatures.put(resolvedArgumentTypeSignatures, m.get());
        return method;
    }

    public JavaType.Parameterized computeParameterized(String fullyQualifiedName, String typeVariableSignatures, Supplier<JavaType.Parameterized> p) {
        Map<String, JavaType.Parameterized> byVariable = parameterizedCache
                .computeIfAbsent(fullyQualifiedName, dt -> new HashMap<>());

        JavaType.Parameterized parameterized = byVariable.get(typeVariableSignatures);
        if(parameterized != null) {
            return parameterized;
        }
        parameterized = p.get();
        byVariable.put(typeVariableSignatures, parameterized);
        return parameterized;
    }

    public JavaType.Variable computeVariable(String fullyQualifiedName, String variableName, Supplier<JavaType.Variable> v) {
        Map<String, JavaType.Variable> byVariable = variableCache
                .computeIfAbsent(fullyQualifiedName, n -> new HashMap<>());

        JavaType.Variable variable = byVariable.get(variableName);
        if(variable != null) {
            return variable;
        }
        variable = v.get();
        byVariable.put(variableName, variable);
        return variable;
    }
}
