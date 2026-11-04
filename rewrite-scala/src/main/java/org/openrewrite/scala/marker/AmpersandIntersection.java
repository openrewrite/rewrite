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
 * Marks a {@link org.openrewrite.java.tree.J.IntersectionType} that uses Scala 3's
 * operator form ({@code A & B}) rather than the {@code with} form ({@code A with B}).
 * Both map to {@code J.IntersectionType}; this marker tells the printer to emit
 * {@code &} as the separator instead of {@code with}.
 */
@Value
@With
public class AmpersandIntersection implements Marker {
    UUID id;

    public static AmpersandIntersection create() {
        return new AmpersandIntersection(Tree.randomId());
    }
}
