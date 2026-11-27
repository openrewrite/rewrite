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
 * Marks a {@code given}'s body {@link org.openrewrite.java.tree.J.Block} that opens with
 * {@code with}, as in {@code given Ord[Int] with { ... }}. The block's prefix is the space before
 * {@code with}, and {@code beforeBrace} the space between {@code with} and <code>{</code>. An
 * indentation-based body after {@code with} also carries {@link IndentedSyntax}.
 */
@Value
@With
public class GivenWith implements Marker {
    UUID id;
    Space beforeBrace;

    public static GivenWith create(Space beforeBrace) {
        return new GivenWith(Tree.randomId(), beforeBrace);
    }
}
