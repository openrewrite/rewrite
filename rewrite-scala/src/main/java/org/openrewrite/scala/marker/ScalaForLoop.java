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
 * Marker indicating that a {@link org.openrewrite.java.tree.J.ForEachLoop} represents
 * a Scala for-comprehension. The printer uses this to emit Scala syntax
 * ({@code for (x <- iterable) body}) instead of Java syntax
 * ({@code for (Type x : iterable) body}).
 */
public class ScalaForLoop implements Marker {
    private final UUID id;

    public ScalaForLoop(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ScalaForLoop withId(UUID id) {
        return new ScalaForLoop(id);
    }

    public static ScalaForLoop create() {
        return new ScalaForLoop(Tree.randomId());
    }
}
