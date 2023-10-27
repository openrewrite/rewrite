/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public interface Expression extends J {
    @Nullable
    JavaType getType();

    <T extends J> T withType(@Nullable JavaType type);

    /**
     * @return A list of the side effects emitted by the statement if the statement was decomposed.
     * So for a binary operation, there are up to two potential side effects (the left and right side) and as
     * few as zero if both sides of the expression are something like constants or variable references.
     */
    default List<J> getSideEffects() {
        return emptyList();
    }

    /**
     * If this expression is a {@link J.Parentheses} return the expression inside the parentheses {@code this}.
     * Otherwise, return this. This operation is performed recursively to return the first non-parenthetical
     * expression.
     *
     * @return The expression as if all surround parentheses were removed. Never a {@link J.Parentheses} instance.
     */
    default Expression unwrap() {
        return requireNonNull(unwrap(this));
    }

    @Nullable
    static Expression unwrap(@Nullable Expression expr) {
        if (expr instanceof J.Parentheses<?> && ((J.Parentheses<?>) expr).getTree() instanceof Expression) {
            return ((Expression) ((J.Parentheses<?>) expr).getTree()).unwrap();
        } else {
            return expr;
        }
    }

    CoordinateBuilder.Expression getCoordinates();
}
