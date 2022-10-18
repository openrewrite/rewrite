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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class RecipeExceptionDemonstration extends Recipe {
    @Option(displayName = "Throw on matching method pattern",
            required = false,
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "org.mockito.Matchers anyVararg()")
    @Nullable
    String throwOnMethodPattern;

    @Option(displayName = "Throw in the recipe's `visit(List<SourceFile>, ExecutionContext)` method.",
            required = false)
    @Nullable
    Boolean throwOnVisitAll;

    @Option(displayName = "Throw in the recipe's `visit(List<SourceFile>, ExecutionContext)` method inside " +
            "a visitor internal to that method.",
            required = false)
    @Nullable
    Boolean throwOnVisitAllVisitor;

    @Option(displayName = "Throw on the project-level applicable test.",
            required = false)
    @Nullable
    Boolean throwOnApplicableTest;

    @Option(displayName = "Throw on the project-level applicable test inside a visitor.",
            required = false)
    @Nullable
    Boolean throwOnApplicableTestVisitor;

    @Option(displayName = "Throw on the single-source applicable test.",
            required = false)
    @Nullable
    Boolean throwOnSingleSourceApplicableTest;

    @Option(displayName = "Throw on the single-source applicable test inside a visitor.",
            required = false)
    @Nullable
    Boolean throwOnSingleSourceApplicableTestVisitor;

    @Override
    @Nullable
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        if (Boolean.TRUE.equals(throwOnApplicableTestVisitor)) {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public Tree preVisit(Tree tree, ExecutionContext executionContext) {
                    throw new RuntimeException("Throwing on the project-level applicable test.");
                }
            };
        } else if (Boolean.TRUE.equals(throwOnApplicableTest)) {
            throw new RuntimeException("Throwing on the project-level applicable test.");
        }
        return null;
    }

    @Override
    @Nullable
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (Boolean.TRUE.equals(throwOnSingleSourceApplicableTestVisitor)) {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public Tree preVisit(Tree tree, ExecutionContext executionContext) {
                    throw new RuntimeException("Demonstrating an exception thrown on the single-source applicable test.");
                }
            };
        } else if (Boolean.TRUE.equals(throwOnSingleSourceApplicableTest)) {
            throw new RuntimeException("Demonstrating an exception thrown on the single-source applicable test.");
        }
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Demonstrate rendering of recipe exceptions";
    }

    @Override
    public String getDescription() {
        return "Show how recipe exceptions are rendered in various forms of OpenRewrite tooling.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        if (Boolean.TRUE.equals(throwOnVisitAllVisitor)) {
            for (SourceFile sourceFile : before) {
                new TreeVisitor<Tree, ExecutionContext>() {
                    @Override
                    public Tree preVisit(Tree tree, ExecutionContext executionContext) {
                        throw new RuntimeException("Demonstrating an exception thrown in the recipe's `visit(List<SourceFile>, ExecutionContext)` method.");
                    }
                }.visit(sourceFile, ctx);
            }
        } else if (Boolean.TRUE.equals(throwOnVisitAll)) {
            throw new RuntimeException("Demonstrating an exception thrown in the recipe's `visit(List<SourceFile>, ExecutionContext)` method.");
        }
        return before;
    }

    @Override
    protected TreeVisitor<J, ExecutionContext> getVisitor() {
        if (!StringUtils.isBlank(throwOnMethodPattern)) {
            return new JavaVisitor<ExecutionContext>() {
                final MethodMatcher methodMatcher = new MethodMatcher(throwOnMethodPattern);

                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                    if (methodMatcher.matches(method)) {
                        throw new RuntimeException("Demonstrating an exception thrown on a matching method.");
                    }
                    return super.visitMethodInvocation(method, executionContext);
                }
            };
        }
        return TreeVisitor.noop();
    }
}
