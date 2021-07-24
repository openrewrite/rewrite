/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.cleanup.SimplifyBooleanExpressionVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.Collections;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

public class InvertCondition<P> extends JavaVisitor<P> {
    private static final class Inverted implements Marker {
        @Override
        public UUID getId() {
            return randomId();
        }
    }

    @Nullable
    @Override
    public J visit(@Nullable Tree tree, P p) {
        J t;
        if (tree instanceof Expression && !(tree instanceof J.ControlParentheses) && !(tree instanceof J.Binary)) {
            Expression expression = (Expression) tree;
            t = new J.Unary(randomId(), expression.getPrefix(), Markers.EMPTY,
                    JLeftPadded.build(J.Unary.Type.Not), expression.withPrefix(Space.EMPTY), expression.getType());
        } else {
            t = super.visit(tree, p);
        }

        return new SimplifyBooleanExpressionVisitor<>().visit(t, p, getCursor().getParentOrThrow());
    }

    @Override
    public J visitBinary(J.Binary binary, P p) {
        J after;
        switch (binary.getOperator()) {
            case LessThan:
                after = binary.withOperator(J.Binary.Type.GreaterThanOrEqual);
                break;
            case GreaterThan:
                after = binary.withOperator(J.Binary.Type.LessThanOrEqual);
                break;
            case LessThanOrEqual:
                after = binary.withOperator(J.Binary.Type.GreaterThan);
                break;
            case GreaterThanOrEqual:
                after = binary.withOperator(J.Binary.Type.LessThan);
                break;
            case Equal:
                after = binary.withOperator(J.Binary.Type.NotEqual);
                break;
            case NotEqual:
                after = binary.withOperator(J.Binary.Type.Equal);
                break;
            default:
                after = new J.Unary(
                        randomId(),
                        binary.getPrefix(),
                        Markers.EMPTY,
                        JLeftPadded.build(J.Unary.Type.Not),
                        new J.Parentheses<>(
                                randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                JRightPadded.build(binary.withPrefix(Space.EMPTY))
                        ),
                        binary.getType());
        }

        return after.withMarkers(Markers.build(Collections.singletonList(new Inverted())));
    }
}
