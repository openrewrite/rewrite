/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.table.MethodCalls;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.marker.SearchResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDistinctMethods extends ScanningRecipe<Map<String, UUID>> {
    transient MethodCalls methodCalls = new MethodCalls(this);

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_INVOCATIONS_DESCRIPTION,
            example = "java.util.List add(..)",
            required = false)
    @Nullable
    String methodPattern;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Find distinct methods in use";
    }

    @Override
    public String getDescription() {
        return "A sample of every distinct method in use in a repository. The code sample in the " +
               "method calls data table will be a representative use of the method, though there " +
               "may be many other such uses of the method.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public Map<String, UUID> getInitialValue(ExecutionContext ctx) {
        return new LinkedHashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<String, UUID> acc) {
        return Preconditions.check(new UsesMethod<>(getMethodPattern(), matchOverrides), new JavaVisitor<ExecutionContext>() {
            final MethodMatcher methodMatcher = new MethodMatcher(getMethodPattern(), matchOverrides);

            @Override
            public J visitExpression(Expression expression, ExecutionContext ctx) {
                if (expression instanceof MethodCall) {
                    MethodCall methodCall = (MethodCall) expression;
                    if (methodMatcher.matches(methodCall) && methodCall.getMethodType() != null) {
                        acc.computeIfAbsent(methodCall.getMethodType().toString(), signature -> expression.getId());
                    }
                }
                return super.visitExpression(expression, ctx);
            }
        });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<String, UUID> acc) {
        return Preconditions.check(new UsesMethod<>(getMethodPattern(), matchOverrides), new JavaVisitor<ExecutionContext>() {
            final MethodMatcher methodMatcher = new MethodMatcher(getMethodPattern(), matchOverrides);

            @Override
            public J visitExpression(Expression expression, ExecutionContext ctx) {
                J e = super.visitExpression(expression, ctx);
                if (expression instanceof MethodCall) {
                    MethodCall methodCall = (MethodCall) expression;
                    if (methodMatcher.matches(methodCall) && methodCall.getMethodType() != null) {
                        String key = methodCall.getMethodType().toString();
                        if (methodCall.getId().equals(acc.get(key))) {
                            Cursor methodCursor = getCursor();
                            methodCalls.insertRow(ctx, new MethodCalls.Row(
                                    methodCursor.firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString(),
                                    methodCall.printTrimmed(methodCursor),
                                    requireNonNull(methodCall.getMethodType()).getDeclaringType().getFullyQualifiedName(),
                                    methodCall.getMethodType().getName(),
                                    methodCall.getMethodType().getParameterTypes().stream().map(Object::toString)
                                            .collect(joining(","))
                            ));
                            acc.remove(key);
                            e = SearchResult.found(e);
                        }
                    }
                }
                return e;
            }
        });
    }

    private String getMethodPattern() {
        return methodPattern == null ? "*..* *(..)" : methodPattern;
    }
}
