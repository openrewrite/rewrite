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
 * Marker for a context-function arrow: {@code ?=>} instead of {@code =>}.
 * Applied to {@link org.openrewrite.java.tree.J.Lambda} when the source uses
 * Scala 3 context-function syntax (e.g. {@code _ ?=> body}).
 */
public class ContextFunctionArrow implements Marker {
    private final UUID id;

    public ContextFunctionArrow(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ContextFunctionArrow withId(UUID id) {
        return new ContextFunctionArrow(id);
    }
}
