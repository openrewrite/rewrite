/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow.internal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Matcher for finding {@link J.NewClass} and {@link J.MethodInvocation} {@link Expression}s.
 */
@Incubating(since = "7.25.0")
@FunctionalInterface
public interface InvocationMatcher {
    boolean matches(Expression expression);

    default AdvancedInvocationMatcher advanced() {
        return new AdvancedInvocationMatcher(this);
    }

    static InvocationMatcher fromMethodMatcher(MethodMatcher methodMatcher) {
        return methodMatcher::matches;
    }

    static InvocationMatcher fromInvocationMatchers(Collection<InvocationMatcher> matchers) {
        if (matchers.size() > 750) {
            return expression -> matchers.parallelStream().anyMatch(matcher -> matcher.matches(expression));
        } else {
            return expression -> matchers.stream().anyMatch(matcher -> matcher.matches(expression));
        }
    }

    static InvocationMatcher fromInvocationMatchers(MethodMatcher... methodMatchers) {
        return fromInvocationMatchers(Stream.of(methodMatchers).map(InvocationMatcher::fromMethodMatcher).collect(Collectors.toList()));
    }

    static InvocationMatcher fromMethodMatchers(Collection<MethodMatcher> matchers) {
        return fromInvocationMatchers(matchers.stream().map(InvocationMatcher::fromMethodMatcher).collect(Collectors.toList()));
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class AdvancedInvocationMatcher {
        InvocationMatcher matcher;

        public boolean isSelect(Cursor cursor) {
            Expression expression = ensureCursorIsExpression(cursor);
            assert expression == cursor.getValue() : "expression != cursor.getValue()";
            J.MethodInvocation maybeMethodInvocation =
                    cursor.getParentOrThrow().firstEnclosing(J.MethodInvocation.class);
            return maybeMethodInvocation != null &&
                    maybeMethodInvocation.getSelect() == expression &&
                    matcher.matches(maybeMethodInvocation); // Do the matcher.matches(...) last as this can be expensive
        }

        public boolean isAnyArgument(Cursor cursor) {
            Expression expression = ensureCursorIsExpression(cursor);
            return extractCallExpression(cursor).map(call -> {
                return getCallArguments(call).contains(expression)
                        && matcher.matches(call); // Do the matcher.matches(...) last as this can be expensive
            }).orElse(false);
        }

        public boolean isFirstParameter(Cursor cursor) {
            return isParameter(cursor, 0);
        }

        public boolean isParameter(Cursor cursor, int parameterIndex) {
            Expression expression = ensureCursorIsExpression(cursor);
            return extractCallExpression(cursor).map(call -> {
                List<Expression> arguments = getCallArguments(call);
                if (parameterIndex >= arguments.size()) {
                    return false;
                }
                if (doesMethodHaveVarargs(call)) {
                    // The varargs parameter is the last one, so we need to check if the expression is the last
                    // parameter or any further argument
                    final int finalParameterIndex = getType(call).getParameterTypes().size() - 1;
                    if (finalParameterIndex == parameterIndex) {
                        List<Expression> varargs = arguments.subList(finalParameterIndex, arguments.size());
                        return varargs.contains(expression) &&
                                matcher.matches(call); // Do the matcher.matches(...) last as this can be expensive
                    }
                }
                return arguments.get(parameterIndex) == expression &&
                        matcher.matches(call); // Do the matcher.matches(...) last as this can be expensive
            }).orElse(false);
        }

        private static boolean doesMethodHaveVarargs(Expression expression) {
            return getType(expression).hasFlags(Flag.Varargs);
        }

        private static JavaType.Method getType(Expression expression) {
            final JavaType.Method type;
            if (expression instanceof J.MethodInvocation) {
                type = ((J.MethodInvocation) expression).getMethodType();
            } else if (expression instanceof J.NewClass) {
                type = ((J.NewClass) expression).getConstructorType();
            } else {
                throw new IllegalArgumentException("Expression is not a method invocation or new class");
            }
            if (type == null) {
                throw new IllegalArgumentException("Type information is missing for " + expression);
            }
            return type;
        }

        private static List<Expression> getCallArguments(Expression call) {
            if (call instanceof J.MethodInvocation) {
                return ((J.MethodInvocation) call).getArguments();
            } else if (call instanceof J.NewClass) {
                List<Expression> arguments = ((J.NewClass) call).getArguments();
                return arguments == null ? Collections.emptyList() : arguments;
            } else {
                throw new IllegalArgumentException("Unknown call type: " + call.getClass());
            }
        }

        private static Optional<Expression> extractCallExpression(Cursor cursor) {
            J stop = cursor.dropParentUntil(v -> v instanceof J.MethodInvocation || v instanceof J.NewClass || v instanceof J.Block).getValue();
            if (stop instanceof J.Block) {
                return Optional.empty();
            }
            return Optional.of((Expression) stop);
        }

        private static Expression ensureCursorIsExpression(Cursor cursor) {
            if (cursor.getValue() instanceof Expression) {
                return cursor.getValue();
            } else {
                throw new IllegalArgumentException("Cursor is not an expression. Was " + cursor.getValue().getClass());
            }
        }
    }
}
