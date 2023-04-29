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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

public class ReplaceRedundantFormatWithPrintf extends Recipe {

    private static final MethodMatcher STRING_FORMAT_MATCHER_LOCALE =
            new MethodMatcher("java.lang.String format(java.util.Locale, java.lang.String, java.lang.Object[])");
    private static final MethodMatcher STRING_FORMAT_MATCHER_NO_LOCALE =
            new MethodMatcher("java.lang.String format(java.lang.String, java.lang.Object[])");

    private static final MethodMatcher PRINTSTREAM_PRINT_MATCHER =
            new MethodMatcher("java.io.PrintStream print(java.lang.String)", true);
    private static final MethodMatcher PRINTSTREAM_PRINTLN_MATCHER =
            new MethodMatcher("java.io.PrintStream println(java.lang.String)", true);


    @Override
    public String getDisplayName() {
        return "Replace redundant String format invocations that are wrapped with PrintStream operations";
    }

    @Override
    public String getDescription() {
        return "Replaces `PrintStream.print(String.format(format, ...args))` with `PrintStream.printf(format, ...args)` (and for `println`, appends a newline to the format string).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(
                Preconditions.or(
                        new UsesMethod<>(STRING_FORMAT_MATCHER_LOCALE),
                        new UsesMethod<>(STRING_FORMAT_MATCHER_NO_LOCALE)
                ),
                Preconditions.or(
                        new UsesMethod<>(PRINTSTREAM_PRINT_MATCHER),
                        new UsesMethod<>(PRINTSTREAM_PRINTLN_MATCHER)
                )
        ), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                method = super.visitMethodInvocation(method, ctx);

                boolean needsNewline;
                if (PRINTSTREAM_PRINTLN_MATCHER.matches(method)) {
                    needsNewline = true;
                } else if (PRINTSTREAM_PRINT_MATCHER.matches(method)) {
                    needsNewline = false;
                } else {
                    return method;
                }

                Expression arg = method.getArguments().get(0);
                if (!(arg instanceof J.MethodInvocation)) {
                    return method;
                }

                J.MethodInvocation innerMethodInvocation = (J.MethodInvocation) arg;

                boolean hasLocaleArg;
                if (STRING_FORMAT_MATCHER_NO_LOCALE.matches(innerMethodInvocation)) {
                    hasLocaleArg = false;
                } else if (STRING_FORMAT_MATCHER_LOCALE.matches(innerMethodInvocation)) {
                    hasLocaleArg = true;
                } else {
                    return method;
                }

                List<Expression> originalFormatArgs = innerMethodInvocation.getArguments();

                List<Expression> printfArgs;
                if (needsNewline) {
                    Expression formatStringExpression = originalFormatArgs.get(hasLocaleArg ? 1 : 0);
                    if (!(formatStringExpression instanceof J.Literal)) {
                        return method;
                    }

                    J.Literal formatStringLiteral = (J.Literal) formatStringExpression;
                    Object formatStringValue = formatStringLiteral.getValue();
                    if (!(formatStringValue instanceof String)) {
                        return method;
                    }

                    formatStringLiteral = appendToStringLiteral(formatStringLiteral);
                    if (formatStringLiteral == null) {
                        return method;
                    }

                    List<Expression> formatStringArgs = originalFormatArgs.subList(hasLocaleArg ? 2 : 1, originalFormatArgs.size());
                    printfArgs = ListUtils.concat(formatStringLiteral, formatStringArgs);

                    if (hasLocaleArg) {
                        printfArgs = ListUtils.concat(originalFormatArgs.get(0), printfArgs);
                    }
                } else {
                    printfArgs = originalFormatArgs;
                }

                // need to build the JavaTemplate code dynamically due to varargs
                StringBuilder code = new StringBuilder();
                code.append("printf(");
                for (int i = 0; i < originalFormatArgs.size(); i++) {
                    JavaType argType = originalFormatArgs.get(i).getType();
                    if (i != 0) {
                        code.append(", ");
                    }
                    code.append("#{any(").append(argType).append(")}");
                }
                code.append(")");

                JavaTemplate template = JavaTemplate.builder(this::getCursor, code.toString()).build();
                return maybeAutoFormat(
                        method,
                        method.withTemplate(
                                template,
                                method.getCoordinates().replaceMethod(),
                                printfArgs.toArray()
                        ),
                        ctx
                );
            }
        });
    }

    private static J.@Nullable Literal appendToStringLiteral(J.Literal literal) {
        if (literal.getType() != JavaType.Primitive.String) {
            return null;
        }

        Object value = literal.getValue();
        if (!(value instanceof String)) {
            return null;
        }
        String stringValue = (String) value;
        String newStringValue = stringValue + "%n";

        String valueSource = literal.getValueSource();
        if (valueSource != null && valueSource.startsWith("\"\"\"") && valueSource.endsWith("\"\"\"")) {
            // text block
            return literal
                    .withValueSource(valueSource.substring(0, valueSource.length() - 3) + "%n" + "\"\"\"")
                    .withValue(newStringValue);
        } else if (valueSource != null && valueSource.startsWith("\"") && valueSource.endsWith("\"")) {
            // regular string literal
            return literal
                    .withValueSource(valueSource.substring(0, valueSource.length() - 1) + "%n" + "\"")
                    .withValue(newStringValue);
        } else {
            return null;
        }
    }
}
