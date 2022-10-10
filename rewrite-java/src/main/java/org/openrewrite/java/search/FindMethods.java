/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.dataflow.FindLocalFlowPaths;
import org.openrewrite.java.dataflow.LocalFlowSpec;
import org.openrewrite.java.dataflow.LocalTaintFlowSpec;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;

import static org.openrewrite.TreeVisitor.collect;

/**
 * Finds matching method invocations.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class FindMethods extends Recipe {

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "java.util.List add(..)")
    String methodPattern;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Option(displayName = "Show flow",
            description = "When enabled, show the data or taint flow of the method invocation.",
            valid = {"none", "data", "taint"},
            required = false
    )
    @Nullable
    String flow;

    @Override
    public String getDisplayName() {
        return "Find method usages";
    }

    @Override
    public String getDescription() {
        return "Find method usages by pattern.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(methodPattern, matchOverrides);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides);
        boolean flowEnabled = !StringUtils.isBlank(flow) && !"none".equals(flow);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(method)) {
                    if (!flowEnabled) {
                        m = SearchResult.found(m);
                    } else {
                        doAfterVisit(new FindLocalFlowPaths<>(getFlowSpec(method)));
                    }
                }
                return m;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                if (methodMatcher.matches(m.getMethodType())) {
                    if (!flowEnabled) {
                        m = m.withReference(m.getReference().withMarkers(m.getReference().getMarkers().searchResult()));
                    } else {
                        doAfterVisit(new FindLocalFlowPaths<>(getFlowSpec(memberRef)));
                    }
                }
                return m;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                if (methodMatcher.matches(newClass)) {
                    if (!flowEnabled) {
                        n = SearchResult.found(n);
                    } else {
                        doAfterVisit(new FindLocalFlowPaths<>(getFlowSpec(newClass)));
                    }
                }
                return n;
            }

            private LocalFlowSpec<Expression, Expression> getFlowSpec(Expression source) {
                switch (flow) {
                    case "data":
                        return new LocalFlowSpec<Expression, Expression>() {

                            @Override
                            public boolean isSource(Expression expression, Cursor cursor) {
                                return expression == source;
                            }

                            @Override
                            public boolean isSink(Expression expression, Cursor cursor) {
                                return true;
                            }
                        };
                    case "taint":
                        return new LocalTaintFlowSpec<Expression, Expression>() {
                            @Override
                            public boolean isSource(Expression expression, Cursor cursor) {
                                return expression == source;
                            }

                            @Override
                            public boolean isSink(Expression expression, Cursor cursor) {
                                return true;
                            }
                        };
                    default:
                        throw new IllegalStateException("Unknown flow: " + flow);
                }
            }
        };
    }

    /**
     * @param j             The subtree to search.
     * @param methodPattern A method pattern. See {@link MethodMatcher} for details about this syntax.
     * @return A set of {@link J.MethodInvocation} and {@link J.MemberReference} representing calls to this method.
     */
    public static Set<J> find(J j, String methodPattern) {
        return TreeVisitor.collect(
                new FindMethods(methodPattern, null, null).getVisitor(),
                j, new HashSet<>()
        );
    }
}
