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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.dataflow.FindLocalFlowPaths;
import org.openrewrite.java.dataflow.LocalFlowSpec;
import org.openrewrite.java.dataflow.LocalTaintFlowSpec;
import org.openrewrite.java.dataflow.internal.InvocationMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds either Taint or Data flow between specified start and end methods.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class FindFlowBetweenMethods extends Recipe {

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Start method pattern", description = "A method pattern that is used to find matching the start point's method invocations.", example = "java.util.List add(..)")
    String startMethodPattern;

    @Option(displayName = "Match start method on overrides", description = "When enabled, find methods that are overrides of the method pattern.", required = false)
    @Nullable Boolean startMatchOverrides;
    @Option(displayName = "End method pattern", description = "A method pattern that is used to find matching the end point's method invocations.", example = "java.util.List add(..)")
    String endMethodPattern;

    @Option(displayName = "Match end method on overrides", description = "When enabled, find methods that are overrides of the method pattern.", required = false)
    @Nullable Boolean endMatchOverrides;

    @Option(displayName = "To target", description = "The part of the method flow should traverse to", required = true, valid = {"Select", "Arguments", "Both"})
    String target;

    @Option(displayName = "Show flow", description = "When enabled, show the data or taint flow of the method invocation.", valid = {"Data", "Taint"}, required = true)
    @Nullable String flow;


    @Override
    public String getDisplayName() {
        return "Finds flow between two methods.";
    }

    @Override
    public String getDescription() {
        return "Takes two patterns for the start/end methods to find flow between.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesAllMethods<>(
                new MethodMatcher(startMethodPattern, startMatchOverrides),
                new MethodMatcher(endMethodPattern, endMatchOverrides)
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher startMethodMatcher = new MethodMatcher(startMethodPattern, startMatchOverrides);
        MethodMatcher endMethodMatcher = new MethodMatcher(endMethodPattern, endMatchOverrides);

        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (startMethodMatcher.matches(method)) {
                    doAfterVisit(new FindLocalFlowPaths(getFlowSpec(method, endMethodMatcher)));
                }
                return m;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                if (startMethodMatcher.matches(m.getMethodType())) {
                    doAfterVisit(new FindLocalFlowPaths(getFlowSpec(memberRef, endMethodMatcher)));
                }
                return m;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                if (startMethodMatcher.matches(newClass)) {
                    doAfterVisit(new FindLocalFlowPaths(getFlowSpec(newClass, endMethodMatcher)));
                }
                return n;
            }

            private boolean conditional (InvocationMatcher sinkMatcher, Cursor cursor) {
                switch (target) {
                    case "Select":
                        return sinkMatcher.advanced().isSelect(cursor);
                    case "Arguments":
                        return sinkMatcher.advanced().isAnyArgument(cursor);
                    case "Both":
                        return sinkMatcher.advanced().isAnyArgument(cursor) ||
                                sinkMatcher.advanced().isSelect(cursor);
                    default:
                        throw new IllegalStateException("Unknown target: " + target);

                }
            }

            private LocalFlowSpec<Expression, Expression> getFlowSpec(Expression source, MethodMatcher sink) {
                final InvocationMatcher sinkMatcher = InvocationMatcher.fromMethodMatcher(sink);
                switch (flow) {
                    case "Data":
                        return new LocalFlowSpec<Expression, Expression>() {

                            @Override
                            public boolean isSource(Expression expression, Cursor cursor) {
                                return expression == source;
                            }

                            @Override
                            public boolean isSink(Expression expression, Cursor cursor) {
                                return conditional(sinkMatcher, cursor);
                            }
                        };
                    case "Taint":
                        return new LocalTaintFlowSpec<Expression, Expression>() {
                            @Override
                            public boolean isSource(Expression expression, Cursor cursor) {
                                return expression == source;
                            }

                            @Override
                            public boolean isSink(Expression expression, Cursor cursor) {
                                return conditional(sinkMatcher, cursor);
                            }
                        };
                    default:
                        throw new IllegalStateException("Unknown flow: " + flow);
                }
            }
        };
    }
}
