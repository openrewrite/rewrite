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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

public class ReplaceRedundantFormatWithPrintf extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace redundant String format invocations that are wrapped with PrintStream operations";
    }

    @Override
    public String getDescription() {
        return "Replaces `PrintStream.print(String.format(format, ...args))` with `PrintStream.printf(format, ...args)` (and for `println`, appends a newline to the format string).";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                method = super.visitMethodInvocation(method, ctx);

                if (!isPrintStreamPrintOrPrintln(method.getMethodType())) {
                    return method;
                }

                if (method.getArguments().size() != 1) {
                    return method;
                }

                Expression arg = method.getArguments().get(0);
                if (!(arg instanceof J.MethodInvocation)) {
                    return method;
                }

                J.MethodInvocation innerMethodInvocation = (J.MethodInvocation) arg;

                if (!isStringFormat(innerMethodInvocation.getMethodType())) {
                    return method;
                }

                List<Expression> newArguments = innerMethodInvocation.getArguments();

                boolean needsNewline = method.getMethodType().getName().equals("println");
                if (needsNewline) {
                    Integer formatStringIndex = getFormatStringIndex(innerMethodInvocation.getMethodType());
                    if (formatStringIndex == null) {
                        return method;
                    }

                    Expression formatStringExpression = newArguments.get(formatStringIndex);
                    if (!(formatStringExpression instanceof J.Literal)) {
                        return method;
                    }

                    J.Literal formatStringLiteral = (J.Literal) formatStringExpression;
                    Object formatStringValue = formatStringLiteral.getValue();
                    if (!(formatStringValue instanceof String)) {
                        return method;
                    }

                    formatStringLiteral = appendToStringLiteral(formatStringLiteral, "%n");
                    newArguments = ListUtils.concatAll(
                            ListUtils.concat(newArguments.subList(0, formatStringIndex), formatStringLiteral),
                            newArguments.subList(formatStringIndex + 1, newArguments.size())
                    );
                }

                return method.withName(method.getName().withSimpleName("printf")).withArguments(newArguments);
            }
        };
    }

    private static boolean isPrintStreamPrintOrPrintln(JavaType.Method method) {
        if (!method.getDeclaringType().getFullyQualifiedName().equals("java.io.PrintStream")) {
            return false;
        }
        if (!method.getName().equals("print") && !method.getName().equals("println")) {
            return false;
        }
        return true;
    }

    private static boolean isStringFormat(JavaType.Method method) {
        if (!method.getDeclaringType().getFullyQualifiedName().equals("java.lang.String")) {
            return false;
        }
        if (!method.getName().equals("format")) {
            return false;
        }
        return true;
    }

    private static Integer getFormatStringIndex(JavaType.Method method) {
        // Signatures are:
        //  	format(Locale l, String format, Object... args)
        // 	    format(String format, Object... args)
        // So it is either the 0th or 1st argument.
        List<JavaType> parameterTypes = method.getParameterTypes();
        for (int i = 0; i <= 1; i++) {
            JavaType type = parameterTypes.get(i);
            if (type instanceof JavaType.FullyQualified) {
                String fullyQualified = (((JavaType.FullyQualified) type).getFullyQualifiedName());
                if (fullyQualified.equals("java.lang.String")) {
                    return i;
                }
            }
        }
        return null;
    }

    @Nullable
    private static J.Literal appendToStringLiteral(J.Literal literal, String textToAppend) {
        if (literal.getType() != JavaType.Primitive.String) {
            return null;
        }

        Object value = literal.getValue();
        if (!(value instanceof String)) {
            return null;
        }
        String stringValue = (String) value;
        String newStringValue = stringValue + textToAppend;

        String valueSource = literal.getValueSource();
        if (valueSource.startsWith("\"\"\"") && valueSource.endsWith("\"\"\"")) {
            // text block
            return literal
                    .withValueSource(valueSource.substring(0, valueSource.length() - 3) + textToAppend + "\"\"\"")
                    .withValue(newStringValue);
        } else if (valueSource.startsWith("\"") && valueSource.endsWith("\"")) {
            // regular string literal
            return literal
                    .withValueSource(valueSource.substring(0, valueSource.length() - 1) + textToAppend + "\"")
                    .withValue(newStringValue);
        } else {
            return null;
        }
    }
}
