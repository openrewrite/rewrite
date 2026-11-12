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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
     * @return The method that a call of {@code name} with {@code arguments} on a receiver of {@code type} calls, as it
     * is declared, or {@code null} when not exactly one fits the call.
     */
    static JavaType.@Nullable Method find(JavaType.FullyQualified type, String name, List<Expression> arguments) {
        // a type in the hierarchy that is only its name may declare the method that is called
        if (isNameOnly(type, new HashSet<>())) {
            return null;
        }
        List<JavaType.Method> candidates = candidates(type, name, arguments);
        for (JavaType.Method candidate : candidates) {
            // whether a call passes its arguments one by one or as an array takes more than their types to tell
            if (candidate.hasFlags(Flag.Varargs)) {
                return null;
            }
        }
        List<@Nullable JavaType> argumentTypes = new ArrayList<>();
        for (Expression argument : arguments) {
            if (!(argument instanceof J.Empty)) {
                argumentTypes.add(tellingType(argument));
            }
        }
        candidates.removeIf(candidate -> !accepts(type, candidate, argumentTypes));
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    /**
     * @return The methods named {@code name} that take as many arguments as {@code arguments}, among those that
     * {@code type} declares and inherits. A method that another one overrides is not among them.
     */
    static List<JavaType.Method> candidates(JavaType.FullyQualified type, String name, List<Expression> arguments) {
        int count = 0;
        for (Expression argument : arguments) {
            if (!(argument instanceof J.Empty)) {
                count++;
            }
        }
        List<JavaType.Method> candidates = new ArrayList<>();
        collect(type, false, name, candidates, new HashSet<>());
        for (Iterator<JavaType.Method> methods = candidates.iterator(); methods.hasNext(); ) {
            JavaType.Method method = methods.next();
            int parameters = method.getParameterTypes().size();
            if (method.hasFlags(Flag.Varargs) ? parameters - 1 > count : parameters != count) {
                methods.remove();
            }
        }
        return candidates;
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
            for (JavaType typeParameter : ((JavaType.Parameterized) type).getTypeParameters()) {
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
        // a subtype inherits neither a private method nor a static method of an interface
        boolean staticMethods = !supertype || type.getKind() != JavaType.FullyQualified.Kind.Interface;
        for (JavaType.Method method : type.getMethods()) {
            if (method.getName().equals(name) && (!supertype || !method.hasFlags(Flag.Private)) &&
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
     * Adds {@code method}, or replaces the method with its parameter types where {@code method} returns a narrower
     * type. A subtype's methods come before those of its supertypes, which they override. An interface can come after
     * one it overrides, as in {@code New extends Wide, Narrow}.
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

    /**
     * Whether {@code type} or one of its supertypes is only its name, which tells neither its methods nor its own
     * supertypes. {@link ChangeType} gives the new type as such a type, also where it is another type's supertype.
     */
    private static boolean isNameOnly(JavaType.@Nullable FullyQualified type, Set<String> visited) {
        if (type instanceof JavaType.Parameterized) {
            type = ((JavaType.Parameterized) type).getType();
        }
        if (type == null || !visited.add(type.getFullyQualifiedName())) {
            return false;
        }
        if (type instanceof JavaType.ShallowClass || type instanceof JavaType.Unknown ||
            isNameOnly(type.getSupertype(), visited)) {
            return true;
        }
        for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
            if (isNameOnly(anInterface, visited)) {
                return true;
            }
        }
        return false;
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

    /**
     * The type of {@code argument} where it tells which parameters take it. A lambda and a method reference have the
     * type of the parameter they were written for, and a type with a supertype that is only its name doesn't tell all
     * that it is assignable to.
     */
    private static @Nullable JavaType tellingType(Expression argument) {
        Expression unwrapped = argument.unwrap();
        JavaType type = argument.getType();
        if (type == null || unwrapped instanceof J.Lambda || unwrapped instanceof J.MemberReference ||
            contains(type, t -> t instanceof JavaType.FullyQualified &&
                                isNameOnly((JavaType.FullyQualified) t, new HashSet<>()))) {
            return null;
        }
        return type;
    }

    private static boolean accepts(JavaType.FullyQualified type, JavaType.Method method,
                                   List<@Nullable JavaType> argumentTypes) {
        Map<String, JavaType> typeArguments = typeArguments(type, method);
        for (int i = 0; i < argumentTypes.size(); i++) {
            JavaType argumentType = argumentTypes.get(i);
            JavaType declaredType = method.getParameterTypes().get(i);
            JavaType parameterType = Substitutions.withArguments(declaredType, typeArguments);
            if (argumentType != null && !mayTake(parameterType == null ? declaredType : parameterType, argumentType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether javac may pass an argument of type {@code argument} for {@code parameter}. A type variable takes what
     * javac infers for it and a parameterized type takes a raw one, so only the classes of the two types tell.
     */
    private static boolean mayTake(JavaType parameter, JavaType argument) {
        if (parameter instanceof JavaType.GenericTypeVariable) {
            return true;
        }
        if (parameter instanceof JavaType.Array && argument instanceof JavaType.Array) {
            return mayTake(((JavaType.Array) parameter).getElemType(), ((JavaType.Array) argument).getElemType());
        }
        if (parameter instanceof JavaType.Parameterized) {
            parameter = ((JavaType.Parameterized) parameter).getType();
        }
        return TypeUtils.isAssignableTo(parameter, argument);
    }
}
