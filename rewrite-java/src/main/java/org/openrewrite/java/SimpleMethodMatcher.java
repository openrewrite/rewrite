/*
 * Copyright 2023 the original author or authors.
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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

/**
 * A simpler version of {@link MethodMatcher} that allows for custom matching logic.
 */
public interface SimpleMethodMatcher {

    default boolean matches(@Nullable Expression maybeMethod) {
        return (maybeMethod instanceof J.MethodInvocation && matches((J.MethodInvocation) maybeMethod)) ||
               (maybeMethod instanceof J.NewClass && matches((J.NewClass) maybeMethod)) ||
               (maybeMethod instanceof J.MemberReference && matches((J.MemberReference) maybeMethod));
    }

    default boolean matches(@Nullable J.MethodInvocation method) {
        if (method == null) {
            return false;
        }
        if (method.getMethodType() == null) {
            return false;
        }
        if (!matchesTargetType(method.getMethodType().getDeclaringType())) {
            return false;
        }

        if (!matchesMethodName(method.getSimpleName())) {
            return false;
        }

        return matchesParameterTypes(method.getMethodType().getParameterTypes());
    }

    default boolean matches(@Nullable J.NewClass constructor) {
        if (constructor == null) {
            return false;
        }
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(constructor.getType());
        if (type == null || constructor.getConstructorType() == null) {
            return false;
        }

        if (!matchesTargetType(type)) {
            return false;
        }

        if (!matchesMethodName("<constructor>")) {
            return false;
        }

        return matchesParameterTypes(constructor.getConstructorType().getParameterTypes());
    }

    default boolean matches(@Nullable J.MemberReference memberReference) {
        if (memberReference == null) {
            return false;
        }
        return matches(memberReference.getMethodType());
    }

    default boolean matches(@Nullable JavaType.Method type) {
        if (type == null || !matchesTargetType(type.getDeclaringType())) {
            return false;
        }

        if (!matchesMethodName(type.getName())) {
            return false;
        }

        return matchesParameterTypes(type.getParameterTypes());
    }


    boolean matchesTargetType(@Nullable JavaType.FullyQualified type);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean matchesMethodName(String methodName);

    boolean matchesParameterTypes(List<JavaType> parameterTypes);

}
