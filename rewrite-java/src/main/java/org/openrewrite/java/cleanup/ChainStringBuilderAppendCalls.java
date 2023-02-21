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
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.utils.ExpressionUtils;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

public class ChainStringBuilderAppendCalls extends Recipe {
    private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(String)");
    private static final JavaType STRING_BUILDER_TYPE = JavaType.buildType("java.lang.StringBuilder");
    private static final String APPEND_METHOD_NAME = "append";

    @Override
    public String getDisplayName() {
        return "Chain `StringBuilder.append()` calls";
    }

    @Override
    public String getDescription() {
        return "String concatenation within calls to `StringBuilder.append()` causes unnecessary memory allocation. Except for concatenations of String literals, which are joined together at compile time. Replaces inefficient concatenations with chained calls to `StringBuilder.append()`";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(STRING_BUILDER_APPEND);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (STRING_BUILDER_APPEND.matches(method)) {
                    List<Expression> arguments = method.getArguments();
                    if (arguments.size() != 1) {
                        return method;
                    }

                    List<Expression> flattenExpressions = new ArrayList<>();
                    boolean flattenable = flatAdditiveExpressions(arguments.get(0), flattenExpressions);
                    if (!flattenable) {
                        return method;
                    }

                    if (flattenExpressions.stream().allMatch(exp -> exp instanceof J.Literal)) {
                        return method;
                    }

                    // group expressions
                    List<Expression> groups = new ArrayList<>();
                    List<Expression> group = new ArrayList<>();
                    for (Expression exp : flattenExpressions) {
                        if (exp instanceof J.Literal) {
                            group.add(exp);
                        } else {
                            addToGroups(group, groups);
                            groups.add(exp);
                        }
                    }
                    addToGroups(group, groups);
                    J.MethodInvocation chainedMethods = method.withArguments(singletonList(groups.get(0)));
                    for (int i = 1; i < groups.size(); i++) {
                        chainedMethods = buildAppendMethodInvocation(chainedMethods, groups.get(i));
                    }

                    return chainedMethods;
                }

                return method;
            }
        };
    }

    /**
     * Concat an additive expression in a group and add to groups
     */
    private static void addToGroups(List<Expression> group, List<Expression> groups) {
        if (!group.isEmpty()) {
            groups.add(ExpressionUtils.additiveExpression(group));
            group.clear();
        }
    }

    private static J.MethodInvocation buildAppendMethodInvocation(
        @Nullable Expression select,
        Expression param
    ) {
        return new J.MethodInvocation(
            Tree.randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            select != null ? new JRightPadded<>(select, Space.EMPTY, Markers.EMPTY) : null,
            null,
            new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, APPEND_METHOD_NAME,
                JavaType.Primitive.Boolean, null),
            JContainer.build(singletonList(new JRightPadded<>(param, Space.EMPTY,
                Markers.EMPTY))),
            new JavaType.Method(
                null,
                Flag.Public.getBitMask(),
                TypeUtils.asFullyQualified(STRING_BUILDER_TYPE),
                APPEND_METHOD_NAME,
                STRING_BUILDER_TYPE,
                singletonList("str"),
                singletonList(JavaType.Primitive.String),
                null, null, null
            )
        );
    }

    private static boolean flatAdditiveExpressions(Expression expression, List<Expression> expressionList) {
        if (expression instanceof J.Binary) {
            J.Binary b = (J.Binary) expression;
            if (b.getOperator() != J.Binary.Type.Addition) {
                return false;
            }

            return flatAdditiveExpressions(b.getLeft(), expressionList)
                && flatAdditiveExpressions(b.getRight(), expressionList);
        } else if (expression instanceof J.Literal || expression instanceof J.Identifier || expression instanceof J.MethodInvocation) {
            expressionList.add(expression.withPrefix(Space.EMPTY));
            return true;
        }

        return false;
    }
}
