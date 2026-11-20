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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.RecipeExample;
import org.openrewrite.config.RecipePreconditions;
import org.openrewrite.scheduling.RecipeRunCycle;

import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptyList;

/**
 * Prepares a recipe tree for execution so that the {@link ScanningRecipe#getEditPreconditions() edit preconditions}
 * of its recipes limit which source files they and their recipe lists edit.
 * <p>
 * A recipe with edit preconditions is replaced by a decorator that also scans for its {@link ScanningRecipe}
 * conditions, decides whether a source file matches right before its own editing visitor runs on it, and decorates
 * its recipe list so that their editing visitors only run on a source file that matched. The decorators delegate
 * everything else, so the recipes keep their metadata, accumulators, scanning and generation.
 */
public final class EditPreconditions {
    private static final String ACCUMULATORS = EditPreconditions.class.getName() + ".accumulators";

    private EditPreconditions() {
    }

    /**
     * @param recipe The recipe at the root of a recipe run.
     * @return The recipe to run in its place.
     */
    public static Recipe prepare(Recipe recipe) {
        return guard(recipe, emptyList());
    }

    /**
     * @param recipe A recipe that {@link #prepare(Recipe)} returned, or one in a recipe list returned by this method.
     * @return The recipe list to run after it.
     */
    public static List<Recipe> recipeList(Recipe recipe) {
        return guard(recipe.getRecipeList(), emptyList());
    }

    /**
     * @param recipe A recipe that {@link #prepare(Recipe)} or {@link #recipeList(Recipe)} returned.
     * @return The recipes that decide its edit preconditions, which run while it scans and edits.
     */
    public static List<Recipe> conditionRecipes(Recipe recipe) {
        return recipe instanceof Guarded ? ((Guarded) recipe).conditionRecipes : emptyList();
    }

    private static List<Recipe> guard(List<Recipe> recipes, List<Guarded> guards) {
        return ListUtils.map(recipes, recipe -> guard(recipe, guards));
    }

    @SuppressWarnings("unchecked")
    private static Recipe guard(Recipe recipe, List<Guarded> guards) {
        if (recipe instanceof ScanningRecipe) {
            ScanningRecipe<Object> scanningRecipe = (ScanningRecipe<Object>) recipe;
            List<Recipe> conditions = scanningRecipe.getEditPreconditions();
            if (!conditions.isEmpty()) {
                return new Guarded(scanningRecipe, conditions, guards);
            }
            return guards.isEmpty() ? recipe : new GatedScanningRecipe(scanningRecipe, guards);
        }
        return guards.isEmpty() ? recipe : new GatedRecipe(recipe, guards);
    }

    private static boolean allMatch(List<Guarded> guards, SourceFile sourceFile, ExecutionContext ctx) {
        for (Guarded guard : guards) {
            if (!guard.matched(sourceFile, ctx)) {
                return false;
            }
        }
        return true;
    }

    private static int cycle(ExecutionContext ctx) {
        RecipeRunCycle<?> cycle = ctx.getMessage(ExecutionContext.CURRENT_CYCLE);
        return cycle == null ? 0 : cycle.getCycle();
    }

    static final class Accumulator {
        final Object delegate;
        final @Nullable Object[] conditions;

        Accumulator(Object delegate, @Nullable Object[] conditions) {
            this.delegate = delegate;
            this.conditions = conditions;
        }
    }

    private static final class Condition {
        final Recipe recipe;

        // A position rather than the recipe instance identifies the accumulator, because a recipe may create new
        // condition instances each time it is asked for them, so the scanning and editing cycles may not share them
        final int slot;

        final List<Condition> conditions;
        final List<Condition> recipeList;

        private Condition(Recipe recipe, int slot, List<Condition> conditions, List<Condition> recipeList) {
            this.recipe = recipe;
            this.slot = slot;
            this.conditions = conditions;
            this.recipeList = recipeList;
        }

        static List<Condition> of(List<Recipe> recipes, List<Condition> scanning) {
            List<Condition> conditions = new ArrayList<>(recipes.size());
            for (Recipe recipe : recipes) {
                boolean isScanning = recipe instanceof ScanningRecipe;
                List<Condition> ownConditions = of(isScanning ?
                        ((ScanningRecipe<?>) recipe).getEditPreconditions() : emptyList(), scanning);
                List<Condition> recipeList = of(recipe.getRecipeList(), scanning);
                Condition condition = new Condition(recipe, isScanning ? scanning.size() : -1, ownConditions, recipeList);
                if (isScanning) {
                    scanning.add(condition);
                }
                conditions.add(condition);
            }
            return conditions;
        }

        static void collectRecipes(List<Condition> conditions, List<Recipe> recipes) {
            for (Condition condition : conditions) {
                recipes.add(condition.recipe);
                collectRecipes(condition.conditions, recipes);
                collectRecipes(condition.recipeList, recipes);
            }
        }

        @SuppressWarnings("unchecked")
        ScanningRecipe<Object> scanningRecipe() {
            return (ScanningRecipe<Object>) recipe;
        }

        @SuppressWarnings("unchecked")
        TreeVisitor<?, ExecutionContext> visitor(@Nullable Object[] accumulators) {
            TreeVisitor<?, ExecutionContext> visitor = slot < 0 ? recipe.getVisitor() :
                    scanningRecipe().getVisitor(accumulators[slot]);
            if (!recipeList.isEmpty()) {
                TreeVisitor<?, ExecutionContext>[] anyOf = new TreeVisitor[recipeList.size() + 1];
                anyOf[0] = visitor;
                for (int i = 0; i < recipeList.size(); i++) {
                    anyOf[i + 1] = recipeList.get(i).visitor(accumulators);
                }
                visitor = Preconditions.or(anyOf);
            }
            return conditions.isEmpty() ? visitor : Preconditions.check(allOf(conditions, accumulators), visitor);
        }

        @SuppressWarnings("unchecked")
        static TreeVisitor<?, ExecutionContext> allOf(List<Condition> conditions, @Nullable Object[] accumulators) {
            TreeVisitor<?, ExecutionContext>[] allOf = new TreeVisitor[conditions.size()];
            for (int i = 0; i < conditions.size(); i++) {
                allOf[i] = conditions.get(i).visitor(accumulators);
            }
            return Preconditions.and(allOf);
        }
    }

    private abstract static class GatedVisitor extends TreeVisitor<Tree, ExecutionContext> {
        private final TreeVisitor<?, ExecutionContext> delegate;

        GatedVisitor(TreeVisitor<?, ExecutionContext> delegate) {
            this.delegate = delegate;
        }

        abstract boolean matches(SourceFile sourceFile, ExecutionContext ctx);

        @Override
        public void setCursor(@Nullable Cursor cursor) {
            super.setCursor(cursor);
            delegate.setCursor(cursor);
        }

        @Override
        public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
            return matches(sourceFile, ctx) && delegate.isAcceptable(sourceFile, ctx);
        }

        @Override
        public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
            return tree instanceof SourceFile && !matches((SourceFile) tree, ctx) ? tree : delegate.visit(tree, ctx);
        }

        @Override
        public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx, Cursor parent) {
            return tree instanceof SourceFile && !matches((SourceFile) tree, ctx) ? tree : delegate.visit(tree, ctx, parent);
        }
    }

    private static final class Guarded extends DecoratedScanningRecipe<Accumulator> {
        private final List<Condition> conditions;
        private final List<Condition> scanningConditions = new ArrayList<>();
        private final List<Recipe> conditionRecipes = new ArrayList<>();
        private final List<Guarded> outerGuards;
        private final List<Guarded> guards;

        private int lastDecidedInCycle;

        private @Nullable UUID lastDecidedFor;

        private boolean lastMatched;

        Guarded(ScanningRecipe<Object> delegate, List<Recipe> conditions, List<Guarded> outerGuards) {
            super(delegate);
            this.conditions = Condition.of(conditions, scanningConditions);
            Condition.collectRecipes(this.conditions, conditionRecipes);
            this.outerGuards = outerGuards;
            List<Guarded> guards = new ArrayList<>(outerGuards.size() + 1);
            guards.addAll(outerGuards);
            guards.add(this);
            this.guards = guards;
        }

        @Override
        public Accumulator getAccumulator(Cursor cursor, ExecutionContext ctx) {
            Map<Recipe, Accumulator> accumulators = cursor.getRoot()
                    .computeMessageIfAbsent(ACCUMULATORS, k -> new IdentityHashMap<>());
            Accumulator acc = accumulators.get(delegate);
            if (acc == null) {
                acc = new Accumulator(delegate.getAccumulator(cursor, ctx), conditionInitialValues(ctx));
                accumulators.put(delegate, acc);
            }
            return acc;
        }

        @Override
        public Accumulator getInitialValue(ExecutionContext ctx) {
            return new Accumulator(delegate.getInitialValue(ctx), conditionInitialValues(ctx));
        }

        private @Nullable Object[] conditionInitialValues(ExecutionContext ctx) {
            @Nullable Object[] initialValues = new Object[scanningConditions.size()];
            for (int i = 0; i < initialValues.length; i++) {
                initialValues[i] = scanningConditions.get(i).scanningRecipe().getInitialValue(ctx);
            }
            return initialValues;
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
            TreeVisitor<?, ExecutionContext> scanner = delegate.getScanner(acc.delegate);
            if (scanningConditions.isEmpty()) {
                return scanner;
            }
            List<TreeVisitor<?, ExecutionContext>> conditionScanners = new ArrayList<>(scanningConditions.size());
            for (Condition condition : scanningConditions) {
                conditionScanners.add(condition.scanningRecipe().getScanner(acc.conditions[condition.slot]));
            }
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    Tree scanned = scan(scanner, tree, ctx, null);
                    for (TreeVisitor<?, ExecutionContext> conditionScanner : conditionScanners) {
                        scan(conditionScanner, tree, ctx, null);
                    }
                    return scanned;
                }

                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx, Cursor parent) {
                    Tree scanned = scan(scanner, tree, ctx, parent);
                    for (TreeVisitor<?, ExecutionContext> conditionScanner : conditionScanners) {
                        scan(conditionScanner, tree, ctx, parent);
                    }
                    return scanned;
                }
            };
        }

        private static @Nullable Tree scan(TreeVisitor<?, ExecutionContext> scanner, @Nullable Tree tree,
                                           ExecutionContext ctx, @Nullable Cursor parent) {
            if (tree instanceof SourceFile && !scanner.isAcceptable((SourceFile) tree, ctx)) {
                return tree;
            }
            return parent == null ? scanner.visit(tree, ctx) : scanner.visit(tree, ctx, parent);
        }

        @Override
        public Collection<? extends SourceFile> generate(Accumulator acc, Collection<SourceFile> generatedInThisCycle, ExecutionContext ctx) {
            return delegate.generate(acc.delegate, generatedInThisCycle, ctx);
        }

        @Override
        public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
            return delegate.generate(acc.delegate, ctx);
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
            return new GatedVisitor(delegate.getVisitor(acc.delegate)) {
                private @Nullable UUID decidedFor;
                private boolean matched;

                @Override
                boolean matches(SourceFile sourceFile, ExecutionContext ctx) {
                    if (!sourceFile.getId().equals(decidedFor)) {
                        matched = decide(sourceFile, ctx, acc);
                        decidedFor = sourceFile.getId();
                    }
                    return matched;
                }
            };
        }

        private boolean decide(SourceFile sourceFile, ExecutionContext ctx, Accumulator acc) {
            lastDecidedFor = null;
            boolean matched = false;
            if (allMatch(outerGuards, sourceFile, ctx)) {
                TreeVisitor<?, ExecutionContext> allOf = Condition.allOf(conditions, acc.conditions);
                matched = allOf.isAcceptable(sourceFile, ctx) && allOf.visit(sourceFile, ctx) != sourceFile;
            }
            lastMatched = matched;
            lastDecidedInCycle = cycle(ctx);
            lastDecidedFor = sourceFile.getId();
            return matched;
        }

        boolean matched(SourceFile sourceFile, ExecutionContext ctx) {
            return lastMatched && sourceFile.getId().equals(lastDecidedFor) && lastDecidedInCycle == cycle(ctx);
        }

        @Override
        public List<Recipe> getRecipeList() {
            return guard(delegate.getRecipeList(), guards);
        }
    }

    private static final class GatedScanningRecipe extends DecoratedScanningRecipe<Object> {
        private final List<Guarded> guards;

        GatedScanningRecipe(ScanningRecipe<Object> delegate, List<Guarded> guards) {
            super(delegate);
            this.guards = guards;
        }

        @Override
        public Object getAccumulator(Cursor cursor, ExecutionContext ctx) {
            return delegate.getAccumulator(cursor, ctx);
        }

        @Override
        public Object getInitialValue(ExecutionContext ctx) {
            return delegate.getInitialValue(ctx);
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(Object acc) {
            return delegate.getScanner(acc);
        }

        @Override
        public Collection<? extends SourceFile> generate(Object acc, Collection<SourceFile> generatedInThisCycle, ExecutionContext ctx) {
            return delegate.generate(acc, generatedInThisCycle, ctx);
        }

        @Override
        public Collection<? extends SourceFile> generate(Object acc, ExecutionContext ctx) {
            return delegate.generate(acc, ctx);
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(Object acc) {
            return new GatedVisitor(delegate.getVisitor(acc)) {
                @Override
                boolean matches(SourceFile sourceFile, ExecutionContext ctx) {
                    return allMatch(guards, sourceFile, ctx);
                }
            };
        }

        @Override
        public List<Recipe> getRecipeList() {
            return guard(delegate.getRecipeList(), guards);
        }
    }

    private static final class GatedRecipe extends Recipe implements Recipe.DelegatingRecipe, RecipePreconditions {
        private final Recipe delegate;
        private final List<Guarded> guards;

        GatedRecipe(Recipe delegate, List<Guarded> guards) {
            this.delegate = delegate;
            this.guards = guards;
        }

        @Override
        public Recipe getDelegate() {
            return delegate;
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new GatedVisitor(delegate.getVisitor()) {
                @Override
                boolean matches(SourceFile sourceFile, ExecutionContext ctx) {
                    return allMatch(guards, sourceFile, ctx);
                }
            };
        }

        @Override
        public List<Recipe> getRecipeList() {
            return guard(delegate.getRecipeList(), guards);
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
        public Set<String> getTags() {
            return delegate.getTags();
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
        public List<RecipeExample> getExamples() {
            return delegate.getExamples();
        }

        @Override
        public int maxCycles() {
            return delegate.maxCycles();
        }

        @Override
        public boolean causesAnotherCycle() {
            return delegate.causesAnotherCycle();
        }

        @Override
        public List<DataTableDescriptor> getDataTableDescriptors() {
            return delegate.getDataTableDescriptors();
        }

        @Override
        protected RecipeDescriptor createRecipeDescriptor() {
            return delegate.getDescriptor();
        }

        @Override
        public List<Recipe> getPreconditions() {
            return delegate instanceof RecipePreconditions ? ((RecipePreconditions) delegate).getPreconditions() : emptyList();
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
    }

    private abstract static class DecoratedScanningRecipe<T> extends ScanningRecipe<T> implements Recipe.DelegatingRecipe, RecipePreconditions {
        final ScanningRecipe<Object> delegate;

        DecoratedScanningRecipe(ScanningRecipe<Object> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Recipe getDelegate() {
            return delegate;
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
        public Set<String> getTags() {
            return delegate.getTags();
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
        public List<RecipeExample> getExamples() {
            return delegate.getExamples();
        }

        @Override
        public int maxCycles() {
            return delegate.maxCycles();
        }

        @Override
        public boolean causesAnotherCycle() {
            return delegate.causesAnotherCycle();
        }

        @Override
        public List<DataTableDescriptor> getDataTableDescriptors() {
            return delegate.getDataTableDescriptors();
        }

        @Override
        protected RecipeDescriptor createRecipeDescriptor() {
            return delegate.getDescriptor();
        }

        @Override
        public List<Recipe> getPreconditions() {
            return delegate instanceof RecipePreconditions ? ((RecipePreconditions) delegate).getPreconditions() : emptyList();
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
    }
}
