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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReplaceStringBuilderWithString extends Recipe {
    private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(String)");
    private static final MethodMatcher STRING_BUILDER_TO_STRING = new MethodMatcher("java.lang.StringBuilder toString()");
    private static J.Parentheses parenthesesTemplate = null;

    @Override
    public String getDisplayName() {
        return "Replace StringBuilder.append() with String";
    }

    @Override
    public String getDescription() {
        return "Replace StringBuilder.append() with String if you are only concatenating a small number of strings " +
               "and the code is simple and easy to read, as the compiler can optimize simple string concatenation " +
               "expressions into a single String object, which can be more efficient than using StringBuilder.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(STRING_BUILDER_APPEND);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);

                if (STRING_BUILDER_TO_STRING.matches(method)) {

                    List<Expression> methodCallsChain = new ArrayList<>();
                    List<Expression> arguments = new ArrayList<>();
                    boolean isFlattenable = flatMethodInvocationChain(method, methodCallsChain, arguments);
                    if (!isFlattenable) {
                        return m;
                    }

                    Collections.reverse(arguments);
                    Expression additive = ChainStringBuilderAppendCalls.additiveExpression(arguments)
                        .withPrefix(method.getPrefix());

                    if (isAMethodSelect(method)) {
                        additive = wrapExpression(additive);
                    }

                    return additive;
                }
                return m;
            }

            // Check if a method call is a select of another method call
            private boolean isAMethodSelect(J.MethodInvocation method) {
                Cursor parent = getCursor().getParent(2); // 2 means skip right padded cursor
                if (parent == null || !(parent.getValue() instanceof J.MethodInvocation)) {
                    return false;
                }
                return ((J.MethodInvocation) parent.getValue()).getSelect() == method;
            }
        };

    }

    /**
     * Return ture if the method calls chain is like "new StringBuilder().append("A")....append("B");"
     * @param method a StringBuilder.toString() method call
     * @param methodChain output methods chain
     * @param arguments output expression list to be chained by '+'.
     */
    private static boolean flatMethodInvocationChain(J.MethodInvocation method,
        List<Expression> methodChain,
        List<Expression> arguments
    ) {
        Expression select = method.getSelect();
        while (select != null) {
            methodChain.add(select);
            if (!(select instanceof J.MethodInvocation)) {
                break;
            }

            J.MethodInvocation selectMethod = (J.MethodInvocation) select;
            select = selectMethod.getSelect();

            if (!STRING_BUILDER_APPEND.matches(selectMethod)) {
                return false;
            }

            List<Expression> args = selectMethod.getArguments();
            if (args.size() != 1) {
                return false;
            } else {
                Expression arg = args.get(0);
                if (arg instanceof J.Identifier || arg instanceof J.Literal || arg instanceof J.MethodInvocation) {
                    arguments.add(arg);
                } else {
                    return false;
                }
            }
        }

        return select instanceof J.NewClass
               && TypeUtils.isOfClassType(((J.NewClass) select).getClazz().getType(), "java.lang.StringBuilder");
    }

    public static J.Parentheses getParenthesesTemplate() {
        if (parenthesesTemplate == null) {
            return PartProvider.buildPart("class B { void foo() { (\"A\" + \"B\").length(); } } ", J.Parentheses.class);
        }
        return parenthesesTemplate;
    }

    public static <T extends J> J.Parentheses<T> wrapExpression(Expression exp) {
        return getParenthesesTemplate().withTree(exp);
    }
}
