/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.internal;

import lombok.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.Recipe.DelegatingRecipe;
import org.openrewrite.config.*;

import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;

public final class PreconditionBellwether {
    private PreconditionBellwether() {
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    private static class Bellwether extends Recipe {
        private final BiFunction<Cursor, ExecutionContext, TreeVisitor<?, ExecutionContext>> condition;

        @Getter
        private transient boolean preconditionApplicable;

        @Override
        public String getDisplayName() {
            return "Precondition bellwether";
        }

        @Override
        public String getDescription() {
            return "Evaluates an edit precondition for a recipe subtree.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    preconditionApplicable = false;
                    TreeVisitor<?, ExecutionContext> visitor = condition.apply(getCursor().getRoot(), ctx);
                    TreeVisitor<?, ExecutionContext> gated = Preconditions.check(visitor, new TreeVisitor<Tree, ExecutionContext>() {
                        @Override
                        public @Nullable Tree visit(@Nullable Tree source, ExecutionContext executionContext) {
                            preconditionApplicable = true;
                            return source;
                        }
                    });
                    if (tree instanceof SourceFile && gated.isAcceptable((SourceFile) tree, ctx)) {
                        gated.visit(tree, ctx);
                    }
                    return tree;
                }
            };
        }
    }

    public static List<Recipe> guard(List<Recipe> recipes,
                                    BiFunction<Cursor, ExecutionContext, TreeVisitor<?, ExecutionContext>> condition) {
        Bellwether bellwether = new Bellwether(condition);
        List<Recipe> guarded = new ArrayList<>(recipes.size() + 1);
        guarded.add(bellwether);
        guarded.addAll(decorateWithPreconditionBellwether(bellwether, recipes));
        return guarded;
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    static class BellwetherDecoratedRecipe extends Recipe implements DelegatingRecipe, RecipePreconditions {

        Bellwether bellwether;
        Recipe delegate;

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public String getDisplayName() {
            return delegate.getDisplayName();
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public String getInstanceName() {
            return delegate.getInstanceName();
        }

        @Override
        public String getInstanceNameSuffix() {
            return delegate.getInstanceNameSuffix();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(bellwether.isPreconditionApplicable(), delegate.getVisitor());
        }

        @Override
        public List<Recipe> getRecipeList() {
            return decorateWithPreconditionBellwether(bellwether, delegate.getRecipeList());
        }

        @Override
        public boolean causesAnotherCycle() {
            return delegate.causesAnotherCycle();
        }

        @Override
        public @Nullable Duration getEstimatedEffortPerOccurrence() {
            return delegate.getEstimatedEffortPerOccurrence();
        }

        @Override
        public List<Maintainer> getMaintainers() {
            return delegate.getMaintainers();
        }

        @Override
        public List<Contributor> getContributors() {
            return delegate.getContributors();
        }

        @Override
        public List<org.openrewrite.config.RecipeExample> getExamples() {
            return delegate.getExamples();
        }

        @Override
        public Set<String> getTags() {
            return delegate.getTags();
        }

        @Override
        public int maxCycles() {
            return delegate.maxCycles();
        }

        @Override
        public List<DataTableDescriptor> getDataTableDescriptors() {
            return delegate.getDataTableDescriptors();
        }

        @Override
        public void onComplete(ExecutionContext ctx) {
            delegate.onComplete(ctx);
        }

        @Override
        public Validated<Object> validate() {
            return delegate.validate();
        }

        @Override
        public Validated<Object> validate(ExecutionContext ctx) {
            return delegate.validate(ctx);
        }

        @Override
        public Collection<Validated<Object>> validateAll(ExecutionContext ctx, Collection<Validated<Object>> acc) {
            return delegate.validateAll(ctx, acc);
        }

        @Override
        public List<Recipe> getPreconditions() {
            if (delegate instanceof RecipePreconditions) {
                return ((RecipePreconditions) delegate).getPreconditions();
            }
            return emptyList();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class BellwetherDecoratedScanningRecipe<T> extends ScanningRecipe<T> implements DelegatingRecipe, RecipePreconditions {

        Bellwether bellwether;
        ScanningRecipe<T> delegate;

        @Override
        public T getAccumulator(Cursor cursor, ExecutionContext ctx) {
            return delegate.getAccumulator(cursor, ctx);
        }

        @Override
        public T getInitialValue(ExecutionContext ctx) {
            return delegate.getInitialValue(ctx);
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(T acc) {
            return delegate.getScanner(acc);
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(T acc) {
            return Preconditions.check(bellwether.isPreconditionApplicable(), delegate.getVisitor(acc));
        }

        @Override
        public Collection<? extends SourceFile> generate(T acc, Collection<SourceFile> generatedInThisCycle, ExecutionContext ctx) {
            return delegate.generate(acc, generatedInThisCycle, ctx);
        }

        @Override
        public Collection<? extends SourceFile> generate(T acc, ExecutionContext ctx) {
            return delegate.generate(acc, ctx);
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public String getDisplayName() {
            return delegate.getDisplayName();
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public String getInstanceName() {
            return delegate.getInstanceName();
        }

        @Override
        public String getInstanceNameSuffix() {
            return delegate.getInstanceNameSuffix();
        }

        @Override
        public List<Recipe> getRecipeList() {
            return decorateWithPreconditionBellwether(bellwether, delegate.getRecipeList());
        }

        @Override
        public boolean causesAnotherCycle() {
            return delegate.causesAnotherCycle();
        }

        @Override
        public @Nullable Duration getEstimatedEffortPerOccurrence() {
            return delegate.getEstimatedEffortPerOccurrence();
        }

        @Override
        public List<Maintainer> getMaintainers() {
            return delegate.getMaintainers();
        }

        @Override
        public List<Contributor> getContributors() {
            return delegate.getContributors();
        }

        @Override
        public List<org.openrewrite.config.RecipeExample> getExamples() {
            return delegate.getExamples();
        }

        @Override
        public Set<String> getTags() {
            return delegate.getTags();
        }

        @Override
        public int maxCycles() {
            return delegate.maxCycles();
        }

        @Override
        public List<DataTableDescriptor> getDataTableDescriptors() {
            return delegate.getDataTableDescriptors();
        }

        @Override
        public void onComplete(ExecutionContext ctx) {
            delegate.onComplete(ctx);
        }

        @Override
        public Validated<Object> validate() {
            return delegate.validate();
        }

        @Override
        public Validated<Object> validate(ExecutionContext ctx) {
            return delegate.validate(ctx);
        }

        @Override
        public Collection<Validated<Object>> validateAll(ExecutionContext ctx, Collection<Validated<Object>> acc) {
            return delegate.validateAll(ctx, acc);
        }

        @Override
        public List<Recipe> getPreconditions() {
            if (delegate instanceof RecipePreconditions) {
                return ((RecipePreconditions) delegate).getPreconditions();
            }
            return emptyList();
        }
    }

    private static List<Recipe> decorateWithPreconditionBellwether(Bellwether bellwether, List<Recipe> recipeList) {
        List<Recipe> mappedRecipeList = new ArrayList<>(recipeList.size());
        for (Recipe recipe : recipeList) {
            if (recipe instanceof ScanningRecipe && isScanningRequired(recipe)) {
                mappedRecipeList.add(new BellwetherDecoratedScanningRecipe<>(bellwether, (ScanningRecipe<?>) recipe));
            } else {
                mappedRecipeList.add(new BellwetherDecoratedRecipe(bellwether, recipe));
            }
        }
        return mappedRecipeList;
    }

    public static boolean isScanningRequired(Recipe recipe) {
        if (recipe instanceof ScanningRecipe) {
            // DeclarativeRecipe is technically a ScanningRecipe, but it only needs the
            // scanning phase if it or one of its sub-recipes or preconditions is a ScanningRecipe
            if (recipe instanceof DeclarativeRecipe) {
                for (Recipe precondition : ((DeclarativeRecipe) recipe).getPreconditions()) {
                    if (isScanningRequired(precondition)) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        for (Recipe r : recipe.getRecipeList()) {
            if (isScanningRequired(r)) {
                return true;
            }
        }
        return false;
    }

}
