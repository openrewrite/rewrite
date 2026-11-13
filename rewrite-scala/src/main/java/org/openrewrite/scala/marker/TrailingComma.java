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
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks a constructor parameter (or other comma-separated element) that was
 * followed by a trailing comma in the source. The printer emits a {@code ,}
 * after the marked element even though it is the last in its list.
 * <p>
 * {@code prefix} holds the whitespace between the element and the trailing
 * comma (e.g. the space in {@code (x: Int ,)}); the whitespace after the comma
 * is stored in the element's own right-padding.
 */
@Value
@With
public class TrailingComma implements Marker {
    UUID id;
    Space prefix;

    public static TrailingComma create(Space prefix) {
        return new TrailingComma(Tree.randomId(), prefix);
    }
}
