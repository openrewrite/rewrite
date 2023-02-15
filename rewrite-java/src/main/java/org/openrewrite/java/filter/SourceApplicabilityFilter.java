/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.filter;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.filter.RecipeApplicableTest;
import org.openrewrite.filter.RecipeSingleSourceApplicableTest;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

/**
 * Heavily Incubating
 */
@Incubating(since = "7.36.0")
@Value
@EqualsAndHashCode(callSuper = true)
public class SourceApplicabilityFilter extends Recipe {

    @AllArgsConstructor
    enum Target {
        AllSource(Target.ALL_SOURCE),
        AllSourceWhenNonTestDetected(Target.ALL_SOURCE_IF_DETECTED_IN_NON_TEST),
        NonTestSource(Target.NON_TEST_SOURCE);

        static final String ALL_SOURCE = "All Source";
        static final String ALL_SOURCE_IF_DETECTED_IN_NON_TEST = "All Source if detected in Non Test Source";
        static final String NON_TEST_SOURCE = "Non-Test Source";

        private static Target fromString(@Nullable String target) {
            if (target == null) {
                return NonTestSource;
            }
            switch (target) {
                case ALL_SOURCE:
                    return AllSource;
                case ALL_SOURCE_IF_DETECTED_IN_NON_TEST:
                    return AllSourceWhenNonTestDetected;
                default:
                    return NonTestSource;
            }
        }

        private final String description;
    }

    @Option(
            displayName = "Target",
            description = "Specify whether all recipes scheduled in this run should apply to all sources or only non-test sources. Defaults to non-test sources.",
            required = false,
            valid = {
                    Target.ALL_SOURCE,
                    Target.ALL_SOURCE_IF_DETECTED_IN_NON_TEST,
                    Target.NON_TEST_SOURCE
            },
            example = Target.ALL_SOURCE
    )
    String target;

    @Override
    public String getDisplayName() {
        return "Source Applicability Filter";
    }

    @Override
    public String getDescription() {
        return "Filters the set of sources all future recipes will be applied to.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Target target = Target.fromString(getTarget());
        if (Target.AllSource.equals(target)) {
            // No filtering required
            return super.visit(before, ctx);
        }

        if (Target.AllSourceWhenNonTestDetected.equals(target)) {
            // Must find one case of a non-test source file that is modified before applying the recipe to all
            RecipeApplicableTest.addToExecutionContext(ctx, recipe -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                    // If the target is Non Test Source, and this is a test source file, skip it.
                    if (isTestSource(cu.getSourcePath())) {
                        return cu;
                    }
                    boolean allMatch = true;
                    for (TreeVisitor<?, ExecutionContext> applicableTest : recipe.getSingleSourceApplicableTests()) {
                        if (applicableTest.visit(cu, ctx, getCursor().getParentOrThrow()) == cu) {
                            allMatch = false;
                            break;
                        }
                    }
                    if (allMatch) {
                        // Unfortunately, this couples the Applicable Test to the Recipe visitor, which could be expensive.
                        // However, it guarantees that at least one non-test source file will be modified before attempting to apply it to the entire tree.
                        return (J.CompilationUnit) getVisitor(recipe).visitNonNull(cu, executionContext, getCursor().getParentOrThrow());
                    }
                    return cu;
                }
            });
        }

        if (Target.NonTestSource.equals(target)) {
            // Must not apply the change to any test source files
            RecipeSingleSourceApplicableTest.addToExecutionContext(ctx, recipe -> new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                    if (isTestSource(cu.getSourcePath())) {
                        return cu;
                    } else {
                        return SearchResult.found(cu);
                    }
                }
            });
        }
        return super.visit(before, ctx);
    }

    private static TreeVisitor<?, ExecutionContext> getVisitor(Recipe recipe) {
        try {
            Method getVisitor = Recipe.class.getDeclaredMethod("getVisitor");
            getVisitor.setAccessible(true);
            //noinspection unchecked
            return (TreeVisitor<?, ExecutionContext>) getVisitor.invoke(recipe);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isTestSource(Path path) {
        return path.getFileSystem().getPathMatcher("glob:**/test/**").matches(path);
    }
}
