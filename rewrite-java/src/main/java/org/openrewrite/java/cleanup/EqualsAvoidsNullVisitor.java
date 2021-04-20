/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.UnwrapParentheses;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = true)
public class EqualsAvoidsNullVisitor<P> extends JavaIsoVisitor<P> {
    private static final MethodMatcher STRING_EQUALS = new MethodMatcher("String equals(java.lang.Object)");
    private static final MethodMatcher STRING_EQUALS_IGNORE_CASE = new MethodMatcher("String equalsIgnoreCase(java.lang.String)");

    EqualsAvoidsNullStyle style;

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);

        if(m.getSelect() == null) {
            return m;
        }

        if ((STRING_EQUALS.matches(m) || (!Boolean.TRUE.equals(style.getIgnoreEqualsIgnoreCase()) && STRING_EQUALS_IGNORE_CASE.matches(m))) &&
                m.getArguments().get(0) instanceof J.Literal &&
                !(m.getSelect() instanceof J.Literal)) {
            J parent = getCursor().dropParentUntil(J.class::isInstance).getValue();
            if (parent instanceof J.Binary) {
                J.Binary binary = (J.Binary) parent;
                if (binary.getOperator() == J.Binary.Type.And && binary.getLeft() instanceof J.Binary) {
                    J.Binary potentialNullCheck = (J.Binary) binary.getLeft();
                    if ((isNullLiteral(potentialNullCheck.getLeft()) && matchesSelect(potentialNullCheck.getRight(), m.getSelect())) ||
                            (isNullLiteral(potentialNullCheck.getRight()) && matchesSelect(potentialNullCheck.getLeft(), m.getSelect()))) {
                        doAfterVisit(new RemoveUnnecessaryNullCheck<>(binary));
                    }
                }
            }

            m = m.withSelect(((J.Literal) m.getArguments().get(0)).withPrefix(m.getSelect().getPrefix()))
                    .withArguments(singletonList(m.getSelect().withPrefix(Space.EMPTY)));
        }

        return m;
    }

    private boolean isNullLiteral(Expression expression) {
        return expression instanceof J.Literal && ((J.Literal) expression).getType() == JavaType.Primitive.Null;
    }

    private boolean matchesSelect(Expression expression, Expression select) {
        return expression.printTrimmed().replaceAll("\\s", "").equals(select.printTrimmed().replaceAll("\\s", ""));
    }

    private static class RemoveUnnecessaryNullCheck<P> extends JavaVisitor<P> {
        private final J.Binary scope;

        public RemoveUnnecessaryNullCheck(J.Binary scope) {
            this.scope = scope;
        }

        @Override
        public J visitBinary(J.Binary binary, P p) {
            J parens = getCursor().dropParentUntil(J.class::isInstance).getValue();
            if(parens instanceof J.Parentheses) {
                doAfterVisit(new UnwrapParentheses<>((J.Parentheses<?>) parens));
            }

            if (scope.isScope(binary)) {
                return binary.getRight().withPrefix(Space.EMPTY);
            }

            return super.visitBinary(binary, p);
        }
    }
}
