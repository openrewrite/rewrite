/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the method of a type that a call resolves to, from the members of the type and its supertypes, when exactly
 * one of them fits the call.
 */
final class CalledMethod {

    private CalledMethod() {
    }

    /**
     * @return The method of {@code type} that a call of {@code name} with arguments of {@code argumentTypes} calls, or
     * {@code null} when not exactly one fits it or the signature of the one that does has a type variable. A call
     * has the types javac substitutes for a type variable, which the method's declaration doesn't hold.
     */
    static JavaType.@Nullable Method find(JavaType.FullyQualified type, String name,
                                          List<@Nullable JavaType> argumentTypes) {
        List<JavaType.Method> candidates = new ArrayList<>();
        collect(type, name, argumentTypes.size(), candidates, new HashSet<>());
        if (candidates.size() > 1) {
            candidates.removeIf(candidate -> !accepts(candidate, argumentTypes));
        }
        if (candidates.size() != 1) {
            return null;
        }
        JavaType.Method method = candidates.get(0);
        if (hasTypeVariable(method.getReturnType())) {
            return null;
        }
        for (JavaType parameterType : method.getParameterTypes()) {
            if (hasTypeVariable(parameterType)) {
                return null;
            }
        }
        return method;
    }

    private static boolean hasTypeVariable(JavaType type) {
        if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable variable = (JavaType.GenericTypeVariable) type;
            // a wildcard is no variable of its own, but its bound may be one
            if (!"?".equals(variable.getName())) {
                return true;
            }
            for (JavaType bound : variable.getBounds()) {
                if (hasTypeVariable(bound)) {
                    return true;
                }
            }
        } else if (type instanceof JavaType.Parameterized) {
            for (JavaType typeParameter : ((JavaType.Parameterized) type).getTypeParameters()) {
                if (hasTypeVariable(typeParameter)) {
                    return true;
                }
            }
        } else if (type instanceof JavaType.Array) {
            return hasTypeVariable(((JavaType.Array) type).getElemType());
        }
        return false;
    }

    private static void collect(JavaType.@Nullable FullyQualified type, String name, int arity,
                                List<JavaType.Method> candidates, Set<String> visited) {
        if (type == null || !visited.add(type.getFullyQualifiedName())) {
            return;
        }
        for (JavaType.Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().size() == arity &&
                !method.hasFlags(Flag.Private) && !overridden(method, candidates)) {
                candidates.add(method);
            }
        }
        collect(type.getSupertype(), name, arity, candidates, visited);
        for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
            collect(anInterface, name, arity, candidates, visited);
        }
    }

    /**
     * Whether a method of a subtype, which is collected before those of its supertypes, overrides {@code method}.
     */
    private static boolean overridden(JavaType.Method method, List<JavaType.Method> candidates) {
        for (JavaType.Method candidate : candidates) {
            if (sameParameterTypes(candidate, method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameParameterTypes(JavaType.Method method, JavaType.Method other) {
        for (int i = 0; i < method.getParameterTypes().size(); i++) {
            if (!TypeUtils.isOfType(method.getParameterTypes().get(i), other.getParameterTypes().get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean accepts(JavaType.Method method, List<@Nullable JavaType> argumentTypes) {
        for (int i = 0; i < argumentTypes.size(); i++) {
            JavaType argumentType = argumentTypes.get(i);
            // an argument the source's parser couldn't type rules out no method
            if (argumentType != null && !(argumentType instanceof JavaType.Unknown) &&
                !TypeUtils.isAssignableTo(method.getParameterTypes().get(i), argumentType)) {
                return false;
            }
        }
        return true;
    }
}
