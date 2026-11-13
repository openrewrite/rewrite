/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.Tree;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks a {@link org.openrewrite.java.tree.J.ParameterizedType} that uses Scala's
 * type-level infix notation ({@code A op B}) rather than the desugared prefix form
 * ({@code op[A, B]}). Scala parses a type-level infix operator such as
 * {@code AsyncDb @@ InsightDb} as {@code @@[AsyncDb, InsightDb]}; this marker tells
 * the printer to re-emit the operator between its two operands instead of as a
 * bracketed type application. The {@code clazz} of the parameterized type holds the
 * operator identifier and the two type parameters hold the left and right operands.
 */
@Value
@With
public class InfixTypeNotation implements Marker {
    UUID id;

    public static InfixTypeNotation create() {
        return new InfixTypeNotation(Tree.randomId());
    }
}
