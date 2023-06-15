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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;

/**
 * The most basic version of a {@link MethodMatcher} that allows implementers to craft custom matching logic.
 */
@FunctionalInterface
public interface JavaTypeMethodMatcher {

    /**
     * Whether the method invocation or constructor matches the criteria of this matcher.
     *
     * @param type The type of the method invocation or constructor.
     * @return True if the invocation or constructor matches the criteria of this matcher.
     */
    boolean matches(@Nullable JavaType.Method type);

    default boolean matches(@Nullable MethodCall methodCall) {
        if (methodCall == null) {
            return false;
        }
        return matches(methodCall.getMethodType());
    }

    /**
     * Whether the method invocation or constructor matches the criteria of this matcher.
     *
     * @param maybeMethod Any {@link Expression} that might be a method invocation or constructor.
     * @return True if the invocation or constructor matches the criteria of this matcher.
     */
    default boolean matches(@Nullable Expression maybeMethod) {
        return maybeMethod instanceof MethodCall && matches(((MethodCall) maybeMethod).getMethodType());
    }

    static JavaTypeMethodMatcher fromMethodMatcher(MethodMatcher methodMatcher) {
        return methodMatcher::matches;
    }

}
