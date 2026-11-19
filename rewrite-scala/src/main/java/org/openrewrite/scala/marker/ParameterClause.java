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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks the first parameter of a primary constructor's second or later parameter clause, as {@code e} in
 * {@code class C(a: Int)(using e: Executor)}, or the {@link org.openrewrite.java.tree.J.Empty} that stands
 * for an empty one. {@code beforeArrow} is the space before the {@code =>} that
 * precedes the clause in a given's Scala 3.6 form, and null elsewhere. {@code prefix} is the space before
 * the clause's opening parenthesis. A given's condition written as a bare type has no parentheses.
 */
@Value
@With
public class ParameterClause implements Marker {
    UUID id;

    @Nullable
    Space beforeArrow;

    Space prefix;

    boolean parenthesized;

    public static ParameterClause create(@Nullable Space beforeArrow, Space prefix, boolean parenthesized) {
        return new ParameterClause(Tree.randomId(), beforeArrow, prefix, parenthesized);
    }
}
