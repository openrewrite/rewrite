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
package org.openrewrite.java.cleanup;

import org.openrewrite.Preconditions;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixStringFormatExpressions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Fix `String#format` and `String#formatted` expressions";
    }

    @Override
    public String getDescription() {
        return "Fix `String#format` and `String#formatted` expressions by replacing `\\n` newline characters with `%n` and removing any unused arguments. Note this recipe is scoped to only transform format expressions which do not specify the argument index.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-3457");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.of(5, ChronoUnit.MINUTES);
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesMethod<>(new MethodMatcher("java.lang.String format(..)")),
                        new UsesMethod<>(new MethodMatcher("java.lang.String formatted(..)"))
                ),
                new FixPrintfExpressionsVisitor()
        );
    }

    private static class FixPrintfExpressionsVisitor extends JavaIsoVisitor<ExecutionContext> {
        // %[argument_index$][flags][width][.precision][t]conversion
        private final String formatSpecifier = "%(\\d+\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
        private final Pattern fsPattern = Pattern.compile(formatSpecifier);

        MethodMatcher sFormatMatcher = new MethodMatcher("java.lang.String format(..)");
        MethodMatcher sFormattedMatcher = new MethodMatcher("java.lang.String formatted(..)");

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            if (sFormatMatcher.matches(mi) || sFormattedMatcher.matches(mi)) {
                boolean isStringFormattedExpression = false;
                J.Literal fmtArg = null;
                if (sFormatMatcher.matches(mi) && mi.getArguments().get(0) instanceof J.Literal) {
                    fmtArg = (J.Literal) mi.getArguments().get(0);
                } else if (sFormattedMatcher.matches(mi) && mi.getSelect() instanceof J.Literal) {
                    fmtArg = (J.Literal) mi.getSelect();
                    isStringFormattedExpression = true;
                }

                if (fmtArg == null || fmtArg.getValue() == null || fmtArg.getValueSource() == null) {
                    return mi;
                }

                // Replace any new line chars with %n
                if (isStringFormattedExpression) {
                    mi = mi.withSelect(replaceNewLineChars(mi.getSelect()));
                } else {
                    mi = mi.withArguments(ListUtils.mapFirst(mi.getArguments(), FixPrintfExpressionsVisitor::replaceNewLineChars));
                }

                // Trim any extra args
                String val = (String) fmtArg.getValue();
                Matcher m = fsPattern.matcher(val);
                int argIndex = isStringFormattedExpression ? 0 : 1;
                while (m.find()) {
                    if (m.group(1) != null || m.group(2).contains("<")) {
                        return mi;
                    }
                    argIndex++;
                }
                int finalArgIndex = argIndex;
                mi = mi.withArguments(ListUtils.map(mi.getArguments(), (i, arg) -> {
                    if (i == 0 || i < finalArgIndex) {
                        return arg;
                    }
                    return null;
                }));
                return mi;
            }
            return mi;
        }

        private static Expression replaceNewLineChars(Expression arg0) {
            if (arg0 instanceof J.Literal) {
                J.Literal fmt = (J.Literal) arg0;
                if (fmt.getValue() != null) {
                    fmt = fmt.withValue(fmt.getValue().toString().replace("\n", "%n"));
                }
                if (fmt.getValueSource() != null) {
                    fmt = fmt.withValueSource(fmt.getValueSource().replace("\\n", "%n"));
                }
                arg0 = fmt;
            }
            return arg0;
        }

    }
}
