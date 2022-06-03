package org.openrewrite.java.internal.beta;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FunctionalInterface
public interface CallMatcher {
    boolean matches(Expression expression);

    default AdvancedCallMatcher advanced() {
        return new AdvancedCallMatcher(this);
    }

    static CallMatcher fromMethodMatcher(MethodMatcher methodMatcher) {
        return methodMatcher::matches;
    }

    static CallMatcher from(Collection<CallMatcher> matchers) {
        if (matchers.size() > 750) {
            return expression -> matchers.parallelStream().anyMatch(matcher -> matcher.matches(expression));
        } else {
            return expression -> matchers.stream().anyMatch(matcher -> matcher.matches(expression));
        }
    }

    static CallMatcher from(MethodMatcher... methodMatchers) {
        return from(Stream.of(methodMatchers).map(CallMatcher::fromMethodMatcher).collect(Collectors.toList()));
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class AdvancedCallMatcher {
        CallMatcher matcher;

        public boolean isSelect(Expression expression, Cursor cursor) {
            assert expression == cursor.getValue() : "expression != cursor.getValue()";
            J.MethodInvocation maybeMethodInvocation =
                    cursor.getParentOrThrow().firstEnclosing(J.MethodInvocation.class);
            return maybeMethodInvocation != null &&
                    maybeMethodInvocation.getSelect() == expression &&
                    matcher.matches(maybeMethodInvocation); // Do the matcher.matches(...) last as this can be expensive
        }

        public boolean isAnyArgument(Expression expression, Cursor cursor) {
            assert expression == cursor.getValue() : "expression != cursor.getValue()";
            J stop = cursor.dropParentUntil(v -> v instanceof J.MethodInvocation || v instanceof J.NewClass || v instanceof J.Block).getValue();
            if (stop instanceof J.Block) {
                return false;
            }
            Expression call = (Expression) stop;
            return getCallArguments(call).contains(expression) && matcher.matches(call); // Do the matcher.matches(...) last as this can be expensive
        }

        public boolean isFirstArgument(Expression expression, Cursor cursor) {
            return isArgument(expression, cursor, 0);
        }

        public boolean isArgument(Expression expression, Cursor cursor, int argumentIndex) {
            assert expression == cursor.getValue() : "expression != cursor.getValue()";
            J stop = cursor.dropParentUntil(v -> v instanceof J.MethodInvocation || v instanceof J.NewClass || v instanceof J.Block).getValue();
            if (stop instanceof J.Block) {
                return false;
            }
            Expression call = (Expression) stop;
            List<Expression> arguments = getCallArguments(call);
            return  arguments.size() > argumentIndex &&
                    arguments.get(argumentIndex) == expression &&
                    matcher.matches(call); // Do the matcher.matches(...) last as this can be expensive
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
    }
}
