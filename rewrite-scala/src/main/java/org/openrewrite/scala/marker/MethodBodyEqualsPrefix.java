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
 * Stores the whitespace between a method signature and the {@code =} that
 * introduces its body, e.g. the space in {@code def f(): Unit ={ }}. Attached
 * to the {@link org.openrewrite.java.tree.J.MethodDeclaration}. The space after
 * the {@code =} is stored as the body block's prefix.
 */
@Value
@With
public class MethodBodyEqualsPrefix implements Marker {
    UUID id;
    Space prefix;

    public static MethodBodyEqualsPrefix create(Space prefix) {
        return new MethodBodyEqualsPrefix(Tree.randomId(), prefix);
    }
}
