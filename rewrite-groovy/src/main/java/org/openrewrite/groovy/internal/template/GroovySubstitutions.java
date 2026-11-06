/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.groovy.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.groovy.marker.AsStyleTypeCast;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;

public class GroovySubstitutions extends Substitutions {
    public GroovySubstitutions(String code, Object[] parameters) {
        super(code, emptySet(), parameters);
    }

    /**
     * Groovy casts a prefix expression however the cast is written, and `x as T` binds below the shifts but above
     * `<`, `in` and `==`, so a cast operand needs parentheses only where it binds more loosely than that.
     */
    @Override
    public Expression maybeParenthesize(Expression newTree, Cursor cursor) {
        Object parent = cursor.getParentTreeCursor().getValue();
        if (parent instanceof J.TypeCast) {
            if (isAsCast((J.TypeCast) parent) ? bindsAsTightlyAsAs(newTree) :
                    newTree instanceof J.Unary && !hasPowerOperand((J.Unary) newTree)) {
                return newTree;
            } else if (newTree instanceof G.Binary && ((G.Binary) newTree).getOperator() != G.Binary.Type.Access ||
                       newTree instanceof G.Range || newTree instanceof J.Unary ||
                       newTree instanceof J.TypeCast && isAsCast((J.TypeCast) newTree)) {
                return new J.Parentheses<>(randomId(), newTree.getPrefix(), Markers.EMPTY,
                        JRightPadded.build(newTree.withPrefix(Space.EMPTY)));
            }
        }
        return super.maybeParenthesize(newTree, cursor);
    }

    private static boolean isAsCast(J.TypeCast typeCast) {
        return typeCast.getMarkers().findFirst(AsStyleTypeCast.class).isPresent();
    }

    // `-a ** b` is `-(a ** b)`, so `(T) -a ** b` would cast `-a` alone
    private static boolean hasPowerOperand(J.Unary unary) {
        Expression operand = unary.getExpression();
        return operand instanceof J.Unary ? hasPowerOperand((J.Unary) operand) :
                operand instanceof G.Binary && ((G.Binary) operand).getOperator() == G.Binary.Type.Power;
    }

    private static boolean bindsAsTightlyAsAs(Expression e) {
        if (e instanceof J.Unary || e instanceof G.Range) {
            return true;
        } else if (e instanceof J.Binary) {
            switch (((J.Binary) e).getOperator()) {
                case Multiplication:
                case Division:
                case Modulo:
                case Addition:
                case Subtraction:
                case LeftShift:
                case RightShift:
                case UnsignedRightShift:
                    return true;
                default:
                    return false;
            }
        } else if (e instanceof G.Binary) {
            G.Binary.Type operator = ((G.Binary) e).getOperator();
            return operator == G.Binary.Type.Power || operator == G.Binary.Type.Access ||
                   operator == G.Binary.Type.NotInstanceOf;
        }
        // `x instanceof T as boolean` is `(x instanceof T) as boolean`
        return e instanceof J.InstanceOf || e instanceof J.TypeCast;
    }
}
