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
 * Marks a {@link org.openrewrite.java.tree.J.MethodInvocation} whose argument is
 * passed as a block without parentheses. In Scala, methods can be called with
 * a block argument: {@code list.foreach { x => println(x) }} instead of
 * {@code list.foreach(x => println(x))}.
 * <p>
 * When this marker is present, the printer omits the parentheses around the
 * argument list and prints the argument (a block or lambda) directly.
 */
public class BlockArgument implements Marker {
    private final UUID id;

    public BlockArgument(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public BlockArgument withId(UUID id) {
        return new BlockArgument(id);
    }
}
