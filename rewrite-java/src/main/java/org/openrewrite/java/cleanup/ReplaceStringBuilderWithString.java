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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.PartProvider;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.openrewrite.Tree.randomId;

public class ReplaceStringBuilderWithString extends Recipe {
    private static final MethodMatcher STRING_BUILDER_APPEND = new MethodMatcher("java.lang.StringBuilder append(..)");
    private static final MethodMatcher STRING_BUILDER_TO_STRING = new MethodMatcher("java.lang.StringBuilder toString()");
    private static J.Parentheses parenthesesTemplate;
    private static J.MethodInvocation stringValueOfTemplate;

    @Override
    public String getDisplayName() {
        return "Replace StringBuilder.append() with String";
    }

    @Override
    public String getDescription() {
        return "Replace `StringBuilder.append()` with String if you are only concatenating a small number of strings " +
               "and the code is simple and easy to read, as the compiler can optimize simple string concatenation " +
               "expressions into a single String object, which can be more efficient than using StringBuilder.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(new UsesMethod<>(STRING_BUILDER_APPEND), new UsesMethod<>(STRING_BUILDER_TO_STRING)), new JavaVisitor<ExecutionContext>() {
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
                    adjustExpressions(arguments);

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
        });
    }

    private J.Literal toStringLiteral(J.Literal input) {
        if (input.getType() == JavaType.Primitive.String) {
            return input;
        }

        String value = input.getValueSource();
        return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value,
                "\"" + value + "\"", null, JavaType.Primitive.String);
    }

    private void adjustExpressions(List<Expression> arguments) {
        for (int i = 0; i < arguments.size(); i++) {
            if (i == 0) {
                // the first expression must be a String type to support case like `new StringBuilder().append(1)`
                if (!TypeUtils.isString(arguments.get(0).getType())) {
                    if (arguments.get(0) instanceof J.Literal) {
                        // wrap by ""
                        arguments.set(0, toStringLiteral((J.Literal) arguments.get(0)));
                    } else {
                        J.MethodInvocation stringValueOf = getStringValueOfMethodInvocationTemplate()
                                .withArguments(Collections.singletonList(arguments.get(0)))
                                .withPrefix(arguments.get(0).getPrefix());
                        arguments.set(0, stringValueOf);
                    }
                }
            } else {
                // wrap by parentheses to support case like `.append(1+2)`
                Expression arg = arguments.get(i);
                if (!(arg instanceof J.Identifier || arg instanceof J.Literal || arg instanceof J.MethodInvocation)) {
                    arguments.set(i, wrapExpression(arg));
                }
            }
        }
    }

    /**
     * Return ture if the method calls chain is like "new StringBuilder().append("A")....append("B");"
     *
     * @param method      a StringBuilder.toString() method call
     * @param methodChain output methods chain
     * @param arguments   output expression list to be chained by '+'.
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
                arguments.add(args.get(0));
            }
        }

        if (select instanceof J.NewClass &&
            ((J.NewClass) select).getClazz() != null &&
            TypeUtils.isOfClassType(((J.NewClass) select).getClazz().getType(), "java.lang.StringBuilder")) {
            J.NewClass nc = (J.NewClass) select;
            if (nc.getArguments().size() == 1 && TypeUtils.isString(nc.getArguments().get(0).getType())) {
                arguments.add(nc.getArguments().get(0));
            }
            return true;
        }
        return false;
    }

    public static J.Parentheses getParenthesesTemplate() {
        if (parenthesesTemplate == null) {
            parenthesesTemplate = PartProvider.buildPart("class B { void foo() { (\"A\" + \"B\").length(); } } ", J.Parentheses.class);
        }
        return parenthesesTemplate;
    }

    public static J.MethodInvocation getStringValueOfMethodInvocationTemplate() {
        if (stringValueOfTemplate == null) {
            stringValueOfTemplate = PartProvider.buildPart("class C {\n" +
                                                           "    void foo() {\n" +
                                                           "        Object obj = 1 + 2;\n" +
                                                           "        String.valueOf(obj);\n" +
                                                           "    }\n" +
                                                           "}",
                    J.MethodInvocation.class);
        }
        return stringValueOfTemplate;
    }

    public static <T extends J> J.Parentheses<T> wrapExpression(Expression exp) {
        return getParenthesesTemplate().withTree(exp).withPrefix(exp.getPrefix());
    }
}
