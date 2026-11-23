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
 * Marks a {@link org.openrewrite.java.tree.J.Switch} that uses Scala 3's selector-style
 * match, where the {@code match} keyword is invoked with a dot on the selector:
 * {@code selector.match { ... }} rather than {@code selector match { ... }}.
 * <p>
 * The printer emits a {@code .} between the selector and the {@code match} keyword when
 * this marker is present.
 */
@Value
@With
public class DottedMatch implements Marker {
    UUID id;

    public static DottedMatch create() {
        return new DottedMatch(Tree.randomId());
    }
}
