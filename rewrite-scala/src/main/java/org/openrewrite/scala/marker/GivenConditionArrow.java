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
 * Marks a {@code given} {@link org.openrewrite.java.tree.J.ClassDeclaration} in the Scala 3.6 form,
 * as in {@code given listOrd: [T: Ord] => Ord[List[T]]}. Each condition is followed by {@code =>}, and
 * the extends clause's before-space is the space before the last one. {@code beforeNameColon} is the
 * space before the colon after a name. {@code beforeTypeParametersArrow} is the space before the arrow
 * between type parameters and a using clause, as in {@code [T] => (ord: Ord[T]) =>}.
 */
@Value
@With
public class GivenConditionArrow implements Marker {
    UUID id;

    @Nullable
    Space beforeNameColon;

    @Nullable
    Space beforeTypeParametersArrow;

    public static GivenConditionArrow create(@Nullable Space beforeNameColon, @Nullable Space beforeTypeParametersArrow) {
        return new GivenConditionArrow(Tree.randomId(), beforeNameColon, beforeTypeParametersArrow);
    }
}
