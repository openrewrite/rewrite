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
import java.util.function.Predicate;

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
        collect(type, false, name, candidates, new HashSet<>());
        for (JavaType.Method candidate : candidates) {
            // a varargs method takes any number of arguments, so a call's number of arguments can't rule it out
            if (candidate.hasFlags(Flag.Varargs)) {
                return null;
            }
        }
        candidates.removeIf(candidate -> candidate.getParameterTypes().size() != argumentTypes.size());
        // The only method with the call's name and number of arguments is the one javac calls once the program
        // compiles, while an argument's type may still be one that a later recipe changes.
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
        return contains(type, t -> t instanceof JavaType.GenericTypeVariable &&
                                   !"?".equals(((JavaType.GenericTypeVariable) t).getName()));
    }

    /**
     * Whether {@code type} or a type it is made of passes {@code test}.
     */
    private static boolean contains(JavaType type, Predicate<JavaType> test) {
        if (test.test(type)) {
            return true;
        }
        if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable variable = (JavaType.GenericTypeVariable) type;
            // a named variable's bound may refer back to it, as in `T extends Comparable<T>`
            if ("?".equals(variable.getName())) {
                for (JavaType bound : variable.getBounds()) {
                    if (contains(bound, test)) {
                        return true;
                    }
                }
            }
        } else if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            if (contains(parameterized.getType(), test)) {
                return true;
            }
            for (JavaType typeParameter : parameterized.getTypeParameters()) {
                if (contains(typeParameter, test)) {
                    return true;
                }
            }
        } else if (type instanceof JavaType.Array) {
            return contains(((JavaType.Array) type).getElemType(), test);
        }
        return false;
    }

    private static void collect(JavaType.@Nullable FullyQualified type, boolean supertype, String name,
                                List<JavaType.Method> candidates, Set<String> visited) {
        if (type == null || !visited.add(type.getFullyQualifiedName())) {
            return;
        }
        // an interface's static methods aren't inherited
        boolean staticMethods = !supertype || type.getKind() != JavaType.FullyQualified.Kind.Interface;
        for (JavaType.Method method : type.getMethods()) {
            if (method.getName().equals(name) && !method.hasFlags(Flag.Private) &&
                (staticMethods || !method.hasFlags(Flag.Static))) {
                add(method, candidates);
            }
        }
        collect(type.getSupertype(), true, name, candidates, visited);
        for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
            collect(anInterface, true, name, candidates, visited);
        }
    }

    /**
     * Adds {@code method}, or replaces the method with its parameter types where {@code method} returns a narrower type.
     * A subtype's methods come before those of its supertypes, which they override. An interface can come after one it
     * overrides, as in {@code New extends Wide, Narrow}.
     */
    private static void add(JavaType.Method method, List<JavaType.Method> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            JavaType.Method candidate = candidates.get(i);
            if (sameParameterTypes(candidate, method)) {
                if (TypeUtils.isAssignableTo(candidate.getReturnType(), method.getReturnType()) &&
                    !TypeUtils.isAssignableTo(method.getReturnType(), candidate.getReturnType())) {
                    candidates.set(i, method);
                }
                return;
            }
        }
        candidates.add(method);
    }

    private static boolean sameParameterTypes(JavaType.Method method, JavaType.Method other) {
        if (method.getParameterTypes().size() != other.getParameterTypes().size()) {
            return false;
        }
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
            // an untyped argument, or one whose type holds a type that is only its name and so has no supertypes,
            // rules out no method
            if (argumentType != null &&
                !contains(argumentType, t -> t instanceof JavaType.Unknown || t instanceof JavaType.ShallowClass) &&
                !TypeUtils.isAssignableTo(method.getParameterTypes().get(i), argumentType)) {
                return false;
            }
        }
        return true;
    }
}
