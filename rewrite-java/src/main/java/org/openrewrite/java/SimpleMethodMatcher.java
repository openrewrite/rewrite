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

import lombok.NoArgsConstructor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

/**
 * A simpler version of {@link MethodMatcher} that allows for custom matching logic.
 * <p>
 * Defines four predicates that, in the general sense, can be used to define if a method matches:
 * <ul>
 *     <li>{@link #isMatchOverrides()}</li>
 *     <li>{@link #matchesTargetTypeName(String)}</li>
 *     <li>{@link #matchesMethodName(String)}</li>
 *     <li>{@link #matchesParameterTypes(List)}</li>
 * </ul>
 */
public interface SimpleMethodMatcher extends JavaTypeMethodMatcher {

    @Override
    default boolean matches(@Nullable JavaType.Method type) {
        if (type == null) {
            return false;
        }
        if (!matchesTargetType(type.getDeclaringType())) {
            return false;
        }

        if (!matchesMethodName(type.getName())) {
            return false;
        }

        return matchesParameterTypes(type.getParameterTypes());
    }


    /**
     * @param type The declaring type of the method invocation or constructor.
     * @return True if the declaring type matches the criteria of this matcher.
     * @implNote {@link #isMatchOverrides()} will be used to determine if parent types should also be checked
     */
    default boolean matchesTargetType(@Nullable JavaType.FullyQualified type) {
        if (type == null || type instanceof JavaType.Unknown) {
            return false;
        }

        if (matchesTargetTypeName(type.getFullyQualifiedName())) {
            return true;
        }

        if (isMatchOverrides()) {
            if (!"java.lang.Object".equals(type.getFullyQualifiedName()) &&
                matchesTargetType(SimpleMethodMatcherHolder.OBJECT_CLASS)) {
                return true;
            }

            if (matchesTargetType(type.getSupertype())) {
                return true;
            }

            for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
                if (matchesTargetType(anInterface)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @return True if this method matcher should match on overrides of the target type.
     * @implNote This will be used to determine if the target type should be checked against the supertypes
     * passing super types to {@link #matchesTargetTypeName(String)}.
     * @implSpec MUST return a constant value for all invocations.
     */
    boolean isMatchOverrides();

    boolean matchesTargetTypeName(String fullyQualifiedTypeName);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean matchesMethodName(String methodName);

    boolean matchesParameterTypes(List<JavaType> parameterTypes);

}

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
final class SimpleMethodMatcherHolder {
    static final JavaType.ShallowClass OBJECT_CLASS =
            JavaType.ShallowClass.build("java.lang.Object");
}
