/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.utils;

import org.intellij.lang.annotations.Language;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public final class ExpressionUtils {
    private ExpressionUtils() {}

    private static final J.Binary ADDITIVE_BINARY_TEMPLATE;
    private static final J.Parentheses SIMPLE_PARENTHESE_TEMPLATE;

    @Language("java")
    private static final String ADDITIVE_BINARY_TEMPLATE_CODE = "class A { void foo() {String s = \"A\" + \"B\";}}";
    @Language("java")
    private static final String SIMPLE_PARENTHESE_TEMPLATE_CODE = "class B { void foo() { (\"A\" + \"B\").length(); } }";

    static {
        List<J.CompilationUnit> cus = JavaParser.fromJavaVersion().build()
            .parse(ADDITIVE_BINARY_TEMPLATE_CODE, SIMPLE_PARENTHESE_TEMPLATE_CODE);
        ADDITIVE_BINARY_TEMPLATE = new JavaIsoVisitor<List<J.Binary>>() {
            @Override
            public J.Binary visitBinary(J.Binary binary, List<J.Binary> rets) {
                rets.add(binary);
                return binary;
            }
        }.reduce(cus.get(0), new ArrayList<>(1)).get(0);

        SIMPLE_PARENTHESE_TEMPLATE = new JavaIsoVisitor<List<J.Parentheses>>() {
            @Override
            public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens,
                List<J.Parentheses> parentheses) {
                parentheses.add(parens);
                return parens;
            }
        }.reduce(cus.get(1), new ArrayList<>(1)).get(0);
    }

    /**
     * Concat two literals to an expression with '+' and surrounded with single space.
     */
    public static J.Binary concatAdditionBinary(Expression left, Expression right) {
        return ADDITIVE_BINARY_TEMPLATE.withPrefix(ADDITIVE_BINARY_TEMPLATE.getLeft().getPrefix())
            .withLeft(left)
            .withRight(right.withPrefix(Space.build(" " + right.getPrefix().getWhitespace(), emptyList())));
    }

    /**
     * Concat expressions to an expression with '+' connected.
     */
    public static Expression additiveExpression(Expression... expressions) {
        Expression expression = null;
        for (Expression element : expressions) {
            if (element != null) {
                expression = (expression == null) ? element : concatAdditionBinary(expression, element);
            }
        }
        return expression;
    }

    public static Expression additiveExpression(List<Expression> expressions) {
       return additiveExpression(expressions.toArray(new Expression[0]));
    }

    public static  <T extends J>  J.Parentheses<T> wrapExpression(Expression exp) {
        return SIMPLE_PARENTHESE_TEMPLATE.withTree(exp);
    }
}
