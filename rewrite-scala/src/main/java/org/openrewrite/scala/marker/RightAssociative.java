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
 * Marks a J.MethodInvocation that uses Scala's right-associative infix notation.
 * In Scala, any operator ending in ':' is right-associative, so {@code xs :: ys}
 * is desugared to {@code ys.::(xs)} — the receiver is the right operand and the
 * left operand becomes the single argument.
 *
 * The AST stores the call semantically (select = right operand, argument = left
 * operand). This marker instructs the printer to reverse the order back to the
 * original source form when rendering: {@code <argument> <name> <select>}.
 *
 * Always used together with {@link InfixNotation}.
 */
@Value
@With
public class RightAssociative implements Marker {
    UUID id;

    public static RightAssociative create() {
        return new RightAssociative(Tree.randomId());
    }
}
