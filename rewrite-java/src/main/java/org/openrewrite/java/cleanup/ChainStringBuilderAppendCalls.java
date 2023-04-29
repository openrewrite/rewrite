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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.PartProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class ChainStringBuilderAppendCalls extends Recipe {
    private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(String)");
    private static J.Binary additiveBinaryTemplate = null;

    @Override
    public String getDisplayName() {
        return "Chain `StringBuilder.append()` calls";
    }

    @Override
    public String getDescription() {
        return "String concatenation within calls to `StringBuilder.append()` causes unnecessary memory allocation. Except for concatenations of String literals, which are joined together at compile time. Replaces inefficient concatenations with chained calls to `StringBuilder.append()`.";
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(STRING_BUILDER_APPEND), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (STRING_BUILDER_APPEND.matches(m)) {
                    List<Expression> arguments = m.getArguments();
                    if (arguments.size() != 1) {
                        return m;
                    }

                    List<Expression> flattenExpressions = new ArrayList<>();
                    boolean flattenable = flatAdditiveExpressions(arguments.get(0).unwrap(), flattenExpressions);
                    if (!flattenable) {
                        return m;
                    }

                    if (flattenExpressions.stream().allMatch(exp -> exp instanceof J.Literal)) {
                        return m;
                    }

                    // group expressions
                    List<Expression> groups = new ArrayList<>();
                    List<Expression> group = new ArrayList<>();
                    boolean appendToString = false;
                    for (Expression exp : flattenExpressions) {
                        if (appendToString) {
                            if (exp instanceof J.Literal
                                && (((J.Literal) exp).getType() == JavaType.Primitive.String)
                            ) {
                                group.add(exp);
                            } else {
                                addToGroups(group, groups);
                                groups.add(exp);
                            }
                        } else {
                            if (exp instanceof J.Literal
                                && (((J.Literal) exp).getType() == JavaType.Primitive.String)) {
                                addToGroups(group, groups);
                                appendToString = true;
                            } else if ((exp instanceof J.Identifier || exp instanceof J.MethodInvocation) && exp.getType() != null) {
                                JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(exp.getType());
                                if (fullyQualified != null && fullyQualified.getFullyQualifiedName().equals("java.lang.String")) {
                                    addToGroups(group, groups);
                                    appendToString = true;
                                }
                            }
                            group.add(exp);

                        }
                    }
                    addToGroups(group, groups);

                    J.MethodInvocation chainedMethods = m.withArguments(singletonList(groups.get(0)));
                    for (int i = 1; i < groups.size(); i++) {
                        chainedMethods = chainedMethods.withSelect(chainedMethods)
                                .withArguments(singletonList(groups.get(i).unwrap()))
                                .withPrefix(Space.EMPTY);
                    }

                    return chainedMethods;
                }

                return m;
            }
        });
    }

    /**
     * Concat two literals to an expression with '+' and surrounded with single space.
     */
    public static J.Binary concatAdditionBinary(Expression left, Expression right) {
        J.Binary b = getAdditiveBinaryTemplate();
        return b.withPrefix(b.getLeft().getPrefix())
                .withLeft(left)
                .withRight(right.withPrefix(Space.build(" " + right.getPrefix().getWhitespace(), emptyList())));
    }

    /**
     * Concat expressions to an expression with '+' connected.
     */
    @Nullable
    public static Expression additiveExpression(Expression... expressions) {
        Expression expression = null;
        for (Expression element : expressions) {
            if (element != null) {
                expression = (expression == null) ? element : concatAdditionBinary(expression, element);
            }
        }
        return expression;
    }

    @Nullable
    public static Expression additiveExpression(List<Expression> expressions) {
        return additiveExpression(expressions.toArray(new Expression[0]));
    }

    public static J.Binary getAdditiveBinaryTemplate() {
        if (additiveBinaryTemplate == null) {
            additiveBinaryTemplate = PartProvider.buildPart("class A { void foo() {String s = \"A\" + \"B\";}}", J.Binary.class);
        }
        return additiveBinaryTemplate;
    }

    /**
     * Concat an additive expression in a group and add to groups
     */
    private static void addToGroups(List<Expression> group, List<Expression> groups) {
        if (!group.isEmpty()) {
            groups.add(additiveExpression(group));
            group.clear();
        }
    }

    public static boolean flatAdditiveExpressions(Expression expression, List<Expression> expressionList) {
        if (expression instanceof J.Binary) {
            J.Binary b = (J.Binary) expression;
            if (b.getOperator() != J.Binary.Type.Addition) {
                return false;
            }

            return flatAdditiveExpressions(b.getLeft(), expressionList)
                   && flatAdditiveExpressions(b.getRight(), expressionList);
        } else if (expression instanceof J.Literal ||
                   expression instanceof J.Identifier ||
                   expression instanceof J.MethodInvocation ||
                   expression instanceof J.Parentheses) {
            expressionList.add(expression.withPrefix(Space.EMPTY));
            return true;
        }

        return false;
    }
}
