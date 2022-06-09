package org.openrewrite.java.internal.beta;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Incubating(since = "7.25.0")
@FunctionalInterface
public interface CallMatcher {
    boolean matches(Expression expression);

    default AdvancedCallMatcher advanced() {
        return new AdvancedCallMatcher(this);
    }

    static CallMatcher fromMethodMatcher(MethodMatcher methodMatcher) {
        return methodMatcher::matches;
    }

    static CallMatcher fromCallMatchers(Collection<CallMatcher> matchers) {
        if (matchers.size() > 750) {
            return expression -> matchers.parallelStream().anyMatch(matcher -> matcher.matches(expression));
        } else {
            return expression -> matchers.stream().anyMatch(matcher -> matcher.matches(expression));
        }
    }

    static CallMatcher fromCallMatchers(MethodMatcher... methodMatchers) {
        return fromCallMatchers(Stream.of(methodMatchers).map(CallMatcher::fromMethodMatcher).collect(Collectors.toList()));
    }

    static CallMatcher fromMethodMatchers(Collection<MethodMatcher> matchers) {
        return fromCallMatchers(matchers.stream().map(CallMatcher::fromMethodMatcher).collect(Collectors.toList()));
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class AdvancedCallMatcher {
        CallMatcher matcher;

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
            J stop = cursor.dropParentUntil(v -> v instanceof J.MethodInvocation || v instanceof J.NewClass || v instanceof J.Block).getValue();
            if (stop instanceof J.Block) {
                return false;
            }
            Expression call = (Expression) stop;
            return getCallArguments(call).contains(expression) && matcher.matches(call); // Do the matcher.matches(...) last as this can be expensive
        }

        public boolean isFirstParameter(Cursor cursor) {
            return isParameter(cursor, 0);
        }

        public boolean isParameter(Cursor cursor, int parameterIndex) {
            Expression expression = ensureCursorIsExpression(cursor);
            J stop = cursor.dropParentUntil(v -> v instanceof J.MethodInvocation || v instanceof J.NewClass || v instanceof J.Block).getValue();
            if (stop instanceof J.Block) {
                return false;
            }
            Expression call = (Expression) stop;
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

        private static Expression ensureCursorIsExpression(Cursor cursor) {
            if (cursor.getValue() instanceof Expression) {
                return cursor.getValue();
            } else {
                throw new IllegalArgumentException("Cursor is not an expression");
            }
        }
    }
}
