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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.cleanup.SimplifyBooleanExpressionVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static org.openrewrite.Tree.randomId;

public class InvertCondition extends JavaVisitor<ExecutionContext> {

    @SuppressWarnings("unchecked")
    public static <J2 extends J> J.ControlParentheses<J2> invert(J.ControlParentheses<J2> controlParentheses, Cursor cursor) {
        //noinspection ConstantConditions
        return (J.ControlParentheses<J2>) new InvertCondition()
                .visit(controlParentheses, new InMemoryExecutionContext(), cursor.getParentOrThrow());
    }

    @Nullable
    @Override
    public J visit(@Nullable Tree tree, ExecutionContext ctx) {
        J t;
        if (tree instanceof Expression && !(tree instanceof J.ControlParentheses) && !(tree instanceof J.Binary)) {
            Expression expression = (Expression) tree;
            t = new J.Unary(randomId(), expression.getPrefix(), Markers.EMPTY,
                    JLeftPadded.build(J.Unary.Type.Not), expression.withPrefix(Space.EMPTY), expression.getType());
        } else {
            t = super.visit(tree, ctx);
        }

        return new SimplifyBooleanExpressionVisitor().visit(t, ctx, getCursor().getParentOrThrow());
    }

    @Override
    public J visitBinary(J.Binary binary, ExecutionContext ctx) {
        switch (binary.getOperator()) {
            case LessThan:
                return binary.withOperator(J.Binary.Type.GreaterThanOrEqual);
            case GreaterThan:
                return binary.withOperator(J.Binary.Type.LessThanOrEqual);
            case LessThanOrEqual:
                return binary.withOperator(J.Binary.Type.GreaterThan);
            case GreaterThanOrEqual:
                return binary.withOperator(J.Binary.Type.LessThan);
            case Equal:
                return binary.withOperator(J.Binary.Type.NotEqual);
            case NotEqual:
                return binary.withOperator(J.Binary.Type.Equal);
        }

        return new J.Unary(
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
}
