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
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * @return The method of {@code type} that a call of {@code name} with arguments of {@code argumentTypes} calls,
     * with the types the call has, or {@code null} when not exactly one fits it or its types can't be told.
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
            candidates.removeIf(candidate -> !accepts(type, candidate, argumentTypes));
        }
        return candidates.size() == 1 ? calledOn(type, candidates.get(0)) : null;
    }

    /**
     * @return {@code declared} with the types that a call on a receiver of {@code type} has, such as
     * {@code List<String>} for a {@code List<T>} called on a {@code Box<String>}, or {@code null} when {@code type}
     * has no argument for one of their type variables. A raw type and a wildcard are no argument, and javac infers a
     * variable of the method itself from the call.
     */
    static JavaType.@Nullable Method calledOn(JavaType.FullyQualified type, JavaType.Method declared) {
        Map<String, JavaType> typeArguments = typeArguments(type, declared);
        List<JavaType> declaredParameterTypes = declared.getParameterTypes();
        List<JavaType> declaredThrownExceptions = declared.getThrownExceptions();
        JavaType returnType = Substitutions.withArguments(declared.getReturnType(), typeArguments);
        List<JavaType> parameterTypes = withArguments(declaredParameterTypes, typeArguments);
        List<JavaType> thrownExceptions = withArguments(declaredThrownExceptions, typeArguments);
        if (returnType == null || parameterTypes == null || thrownExceptions == null) {
            return null;
        }
        if (returnType == declared.getReturnType() && parameterTypes == declaredParameterTypes &&
            thrownExceptions == declaredThrownExceptions) {
            return declared;
        }
        // the parser names the declaring type of a method with other types than it is declared with by the type's
        // own variables, as in `Box<T>`
        JavaType.FullyQualified declaringType = declared.getDeclaringType();
        return declared
                .withDeclaringType(new JavaType.Parameterized(null, declaringType, declaringType.getTypeParameters()))
                .withReturnType(returnType)
                .withParameterTypes(parameterTypes)
                .withThrownExceptions(thrownExceptions);
    }

    /**
     * @return {@code types} itself where none of them names a type variable.
     */
    private static @Nullable List<JavaType> withArguments(List<JavaType> types, Map<String, JavaType> typeArguments) {
        List<JavaType> withArguments = types;
        for (int i = 0; i < types.size(); i++) {
            JavaType type = Substitutions.withArguments(types.get(i), typeArguments);
            if (type == null) {
                return null;
            }
            if (type != types.get(i)) {
                if (withArguments == types) {
                    withArguments = new ArrayList<>(types);
                }
                withArguments.set(i, type);
            }
        }
        return withArguments;
    }

    /**
     * What the type variables of the type that declares {@code method} stand for in a receiver of {@code type}, by
     * name. A wildcard and a variable that one of {@code method}'s own hides are left out.
     */
    private static Map<String, JavaType> typeArguments(JavaType.FullyQualified type, JavaType.Method method) {
        Map<String, JavaType> typeArguments = new HashMap<>();
        JavaType.Parameterized declaringType = Substitutions.asSupertype(type,
                method.getDeclaringType().getFullyQualifiedName());
        if (declaringType != null) {
            List<JavaType> variables = declaringType.getType().getTypeParameters();
            List<JavaType> arguments = declaringType.getTypeParameters();
            for (int i = 0; i < variables.size() && i < arguments.size(); i++) {
                if (variables.get(i) instanceof JavaType.GenericTypeVariable && !isWildcard(arguments.get(i))) {
                    typeArguments.put(((JavaType.GenericTypeVariable) variables.get(i)).getName(), arguments.get(i));
                }
            }
        }
        typeArguments.keySet().removeAll(method.getDeclaredFormalTypeNames());
        return typeArguments;
    }

    private static boolean isWildcard(JavaType type) {
        return type instanceof JavaType.GenericTypeVariable &&
               "?".equals(((JavaType.GenericTypeVariable) type).getName());
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
            if (isWildcard(variable)) {
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
            if (sameTypes(candidate.getParameterTypes(), method.getParameterTypes())) {
                if (TypeUtils.isAssignableTo(candidate.getReturnType(), method.getReturnType()) &&
                    !TypeUtils.isAssignableTo(method.getReturnType(), candidate.getReturnType())) {
                    candidates.set(i, method);
                }
                return;
            }
        }
        candidates.add(method);
    }

    static boolean sameTypes(List<JavaType> types, List<JavaType> others) {
        if (types.size() != others.size()) {
            return false;
        }
        for (int i = 0; i < types.size(); i++) {
            if (!TypeUtils.isOfType(types.get(i), others.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean accepts(JavaType.FullyQualified type, JavaType.Method method,
                                   List<@Nullable JavaType> argumentTypes) {
        Map<String, JavaType> typeArguments = typeArguments(type, method);
        for (int i = 0; i < argumentTypes.size(); i++) {
            JavaType argumentType = argumentTypes.get(i);
            JavaType declaredType = method.getParameterTypes().get(i);
            JavaType parameterType = Substitutions.withArguments(declaredType, typeArguments);
            // an untyped argument, or one whose type holds a type that is only its name and so has no supertypes,
            // rules out no method
            if (argumentType != null &&
                !contains(argumentType, t -> t instanceof JavaType.Unknown || t instanceof JavaType.ShallowClass) &&
                !TypeUtils.isAssignableTo(parameterType == null ? declaredType : parameterType, argumentType)) {
                return false;
            }
        }
        return true;
    }
}
