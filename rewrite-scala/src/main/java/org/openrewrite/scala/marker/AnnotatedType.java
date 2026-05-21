/*
 * Copyright 2025 the original author or authors.
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

import org.openrewrite.Tree;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks a {@link org.openrewrite.scala.tree.S.AnnotatedExpression} that represents an
 * annotated type ({@code T @ann}) rather than an annotated expression ({@code e: @ann}).
 * <p>
 * When this marker is present, the printer omits the {@code :} separator between the
 * underlying tree and the annotation.
 */
public class AnnotatedType implements Marker {
    private final UUID id;

    public AnnotatedType(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public AnnotatedType withId(UUID id) {
        return new AnnotatedType(id);
    }

    public static AnnotatedType create() {
        return new AnnotatedType(Tree.randomId());
    }
}
