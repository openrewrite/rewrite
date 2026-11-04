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
 * Marks a statement that was followed by an explicit {@code ;} separator in
 * the source. Scala semicolons are optional; this marker preserves them for
 * round-trip printing when they are present (e.g. multiple statements on the
 * same line: {@code a = 1; b = 2}).
 */
public class Semicolon implements Marker {
    private final UUID id;

    public Semicolon(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public Semicolon withId(UUID id) {
        return new Semicolon(id);
    }
}
