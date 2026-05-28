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

import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marker for a partial-function literal: {@code { case pat => ... }}.
 * Modeled as a {@link org.openrewrite.java.tree.J.Lambda} with no declared parameters
 * whose body holds the case clauses. The printer uses this marker to emit the
 * {@code { case ... => ... }} form instead of the desugared lambda shape.
 */
public class PartialFunctionLiteral implements Marker {
    private final UUID id;

    public PartialFunctionLiteral(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public PartialFunctionLiteral withId(UUID id) {
        return new PartialFunctionLiteral(id);
    }
}
