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

import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks an LST element that uses Scala 3 indentation-based syntax, where the body
 * is introduced by {@code :} and indentation rather than {@code {}} braces. Applied
 * to a {@link org.openrewrite.java.tree.J.Block} for braceless blocks, and to a
 * {@link org.openrewrite.java.tree.J.Package} for an indented package region.
 */
public class IndentedSyntax implements Marker {
    private final UUID id;

    public IndentedSyntax(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public IndentedSyntax withId(UUID id) {
        return new IndentedSyntax(id);
    }
}
