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
package org.openrewrite.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.NonFinal;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.openrewrite.Validated.invalid;

@RequiredArgsConstructor
public class DeclarativeRecipe extends ScanningRecipe<DeclarativeRecipe.Accumulator> implements RecipePreconditions {
    @Getter
    private final String name;

    @Getter
    @Language("markdown")
    private final String displayName;

    @Getter
    @Language("markdown")
    @Nullable // in YAML the description is not always present
    private final String description;

    @Getter
    private final Set<String> tags;

    @Nullable
    private final Duration estimatedEffortPerOccurrence;

    private final URI source;

    private final boolean causesAnotherCycle;

    @Override
    public boolean causesAnotherCycle() {
        return causesAnotherCycle || super.causesAnotherCycle();
    }

    @Getter
    private final List<Maintainer> maintainers;

    private final List<Recipe> uninitializedRecipes = new ArrayList<>();

    @Setter
    private volatile List<Recipe> recipeList = emptyList();

    private final List<Recipe> uninitializedPreconditions = new ArrayList<>();

    @Getter
    @Setter
    private volatile List<Recipe> preconditions = emptyList();

    public void addPrecondition(Recipe recipe) {
        uninitializedPreconditions.add(recipe);
    }

    @JsonIgnore
    private Validated<Object> validation = Validated.none();

    @JsonIgnore
    private Validated<Object> initValidation = Validated.none();

    @JsonIgnore
    private volatile boolean initialized;

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return estimatedEffortPerOccurrence == null ? Duration.ofMinutes(0) :
                estimatedEffortPerOccurrence;
    }

    public void initialize(Collection<Recipe> availableRecipes) {
        Map<String, Recipe> recipeMap = new HashMap<>();
        availableRecipes.forEach(r -> recipeMap.putIfAbsent(r.getName(), r));
        initialize(recipeMap::get);
    }

    @Deprecated
    public void initialize(Collection<Recipe> availableRecipes,
                           @SuppressWarnings("unused") Map<String, List<Contributor>> recipeToContributors) {
        this.initialize(availableRecipes);
    }

    public void initialize(Function<String, @Nullable Recipe> availableRecipes) {
        initialize(availableRecipes, new LinkedHashSet<>());
    }

    @Deprecated
    public void initialize(Function<String, @Nullable Recipe> availableRecipes,
                           @SuppressWarnings("unused") Map<String, List<Contributor>> recipeToContributors) {
        this.initialize(availableRecipes);
    }

    private void initialize(Function<String, @Nullable Recipe> availableRecipes, Set<String> initializingRecipes) {
        if (initialized) {
            return;
        }
        if (!initializingRecipes.add(name)) {
            throw new RecipeIntrospectionException("Recipe '" + name + "' creates a cycle: " +
                                                   String.join(" -> ", initializingRecipes) + " -> " + name);
        }
        recipeList = resolve(uninitializedRecipes, "recipeList", availableRecipes, initializingRecipes);
        preconditions = resolve(uninitializedPreconditions, "preconditions", availableRecipes, initializingRecipes);
        initialized = true;
        initializingRecipes.remove(name);
    }

    private List<Recipe> resolve(List<Recipe> uninitialized, String property,
                                 Function<String, @Nullable Recipe> availableRecipes, Set<String> initializingRecipes) {
        List<Recipe> result = new ArrayList<>();
        for (int i = 0; i < uninitialized.size(); i++) {
            Recipe recipe = uninitialized.get(i);
            if (recipe instanceof LazyLoadedRecipe) {
                String recipeFqn = ((LazyLoadedRecipe) recipe).getRecipeFqn();
                recipe = availableRecipes.apply(recipeFqn);
                if (recipe == null) {
                    initValidation = initValidation.and(
                            invalid(name + "." + property + "[" + i + "] (in " + source + ")",
                                    recipeFqn,
                                    "refers to a recipe that doesn't exist.",
                                    null));
                    continue;
                }
            }
            if (recipe instanceof DeclarativeRecipe) {
                ((DeclarativeRecipe) recipe).initialize(availableRecipes, initializingRecipes);
            }
            result.add(recipe);
        }
        return unmodifiableList(result);
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        Accumulator acc = new Accumulator();
        for (Recipe precondition : preconditions) {
            registerNestedScanningRecipes(precondition, acc, ctx);
        }
        return acc;
    }

    private void registerNestedScanningRecipes(Recipe recipe, Accumulator acc, ExecutionContext ctx) {
        if (recipe instanceof ScanningRecipe && isScanningRequired(recipe)) {
            acc.recipeToAccumulator.put(recipe, ((ScanningRecipe<?>) recipe).getInitialValue(ctx));
        }
        for (Recipe nested : recipe.getRecipeList()) {
            registerNestedScanningRecipes(nested, acc, ctx);
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                for (Recipe precondition : preconditions) {
                    scanNestedScanningRecipes(precondition, acc, tree, ctx);
                }
                return tree;
            }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void scanNestedScanningRecipes(Recipe recipe, Accumulator acc, @Nullable Tree tree, ExecutionContext ctx) {
        if (recipe instanceof ScanningRecipe && isScanningRequired(recipe)) {
            ScanningRecipe scanningRecipe = (ScanningRecipe) recipe;
            Object recipeAcc = acc.recipeToAccumulator.get(recipe);
            scanningRecipe.getScanner(recipeAcc).visit(tree, ctx);
        }
        for (Recipe nested : recipe.getRecipeList()) {
            scanNestedScanningRecipes(nested, acc, tree, ctx);
        }
    }

    public static class Accumulator {
        Map<Recipe, Object> recipeToAccumulator = new HashMap<>();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    static class PreconditionBellwether extends Recipe {

        String displayName = "Precondition bellwether";

        String description = "Evaluates a precondition and makes that result available to the preconditions of other recipes. " +
                   "\"bellwether\", noun - One that serves as a leader or as a leading indicator of future trends.";

        DeclarativeRecipe declarativeRecipe;

        @NonFinal
        transient boolean preconditionApplicable;

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Nullable
                TreeVisitor<?, ExecutionContext> p;

                @Override
                public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                    // p is lazily resolved in visit() where the cursor is available.
                    // Before that, conservatively accept all source files.
                    return p == null || p.isAcceptable(sourceFile, ctx);
                }

                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    Tree t = resolve(ctx).visit(tree, ctx);
                    preconditionApplicable = t != tree;
                    return tree;
                }

                private TreeVisitor<?, ExecutionContext> resolve(ExecutionContext ctx) {
                    if (p == null) {
                        Cursor rootCursor = getCursor().getRoot();
                        List<TreeVisitor<?, ExecutionContext>> andVisitors = new ArrayList<>();
                        for (Recipe precondition : declarativeRecipe.preconditions) {
                            andVisitors.add(declarativeRecipe.orVisitors(precondition, rootCursor, ctx));
                        }
                        //noinspection unchecked
                        p = Preconditions.and(andVisitors.toArray(new TreeVisitor[0]));
                    }
                    return p;
                }
            };
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    static class BellwetherDecoratedRecipe extends Recipe implements DelegatingRecipe, RecipePreconditions {

        DeclarativeRecipe.PreconditionBellwether bellwether;
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

        DeclarativeRecipe.PreconditionBellwether bellwether;
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

    @Override
    public final List<Recipe> getRecipeList() {
        List<Recipe> filtered = deduplicateSingletonsRecursively(recipeList, new HashSet<>());

        if (preconditions.isEmpty()) {
            return filtered;
        }

        PreconditionBellwether bellwether = new PreconditionBellwether(this);
        List<Recipe> recipeListWithBellwether = new ArrayList<>(filtered.size() + 1);
        recipeListWithBellwether.add(bellwether);
        recipeListWithBellwether.addAll(decorateWithPreconditionBellwether(bellwether, filtered));
        return recipeListWithBellwether;
    }

    /**
     * Walk {@code recipes} and every {@link DeclarativeRecipe} reachable through it, dropping
     * later occurrences of any DeclarativeRecipe (by name) whose preconditions include
     * {@link Singleton}. The {@code seen} set is threaded through the entire traversal so a
     * Singleton-gated recipe that appears under one branch is also filtered from sibling or
     * nested branches. The {@code Singleton} precondition would make the later occurrences
     * no-op at runtime; removing them from the list avoids scheduling them at all.
     */
    private static List<Recipe> deduplicateSingletonsRecursively(List<Recipe> recipes, Set<String> seen) {
        return ListUtils.map(recipes, recipe -> {
            if (recipe instanceof DeclarativeRecipe) {
                DeclarativeRecipe dr = (DeclarativeRecipe) recipe;
                if (hasSingletonPrecondition(dr) && !seen.add(dr.getName())) {
                    //noinspection DataFlowIssue
                    return null;
                }
                return dr.withDeduplicatedChildren(seen);
            }
            return recipe;
        });
    }

    /**
     * Return a view of this recipe whose {@code recipeList} has been recursively filtered to
     * remove Singleton-gated duplicates using the shared {@code seen} set. If nothing changes
     * the original instance is returned; otherwise a copy is produced so the original tree is
     * left untouched.
     */
    DeclarativeRecipe withDeduplicatedChildren(Set<String> seen) {
        List<Recipe> deduplicated = deduplicateSingletonsRecursively(recipeList, seen);
        if (deduplicated == recipeList) {
            return this;
        }
        return copyWithRecipeList(this, deduplicated);
    }

    /**
     * Factory used by {@link #withDeduplicatedChildren(Set)} to produce an otherwise identical
     * recipe whose {@code recipeList} has been filtered. Shares immutable (or effectively
     * immutable) state — {@code preconditions}, {@code validation}, {@code initValidation} —
     * with {@code source} and leaves {@code uninitializedRecipes} empty so {@link #validate()}
     * doesn't reject the filtered list as "uninitialized". The new instance's
     * {@link ScanningRecipe} field initializer gives it a fresh {@code recipeAccMessage}.
     * <p>
     * Implemented as a static factory rather than an additional constructor so that Jackson
     * can still unambiguously pick the Lombok-generated required-args constructor during
     * recipe deserialization.
     */
    private static DeclarativeRecipe copyWithRecipeList(DeclarativeRecipe source, List<Recipe> recipeList) {
        DeclarativeRecipe copy = new DeclarativeRecipe(source.name, source.displayName,
                source.description, source.tags, source.estimatedEffortPerOccurrence,
                source.source, source.causesAnotherCycle, source.maintainers);
        copy.recipeList = recipeList;
        copy.preconditions = source.preconditions;
        copy.validation = source.validation;
        copy.initValidation = source.initValidation;
        copy.initialized = source.initialized;
        return copy;
    }

    private static boolean hasSingletonPrecondition(DeclarativeRecipe recipe) {
        for (Recipe precondition : recipe.getPreconditions()) {
            if (precondition instanceof Singleton) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TreeVisitor<?, ExecutionContext> orVisitors(Recipe recipe, Cursor rootCursor, ExecutionContext ctx) {
        List<TreeVisitor<?, ExecutionContext>> conditions = new ArrayList<>();
        if (recipe instanceof ScanningRecipe) {
            ScanningRecipe scanning = (ScanningRecipe) recipe;
            Accumulator acc = getAccumulator(rootCursor, ctx);
            Object recipeAcc = acc.recipeToAccumulator.get(scanning);
            conditions.add(scanning.getVisitor(recipeAcc));
        } else {
            conditions.add(recipe.getVisitor());
        }
        for (Recipe r : recipe.getRecipeList()) {
            conditions.add(orVisitors(r, rootCursor, ctx));
        }
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        return Preconditions.or(conditions.toArray(new TreeVisitor[0]));
    }

    private static List<Recipe> decorateWithPreconditionBellwether(PreconditionBellwether bellwether, List<Recipe> recipeList) {
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

    private static boolean isScanningRequired(Recipe recipe) {
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

    public void addUninitialized(Recipe recipe) {
        uninitializedRecipes.add(recipe);
    }

    public void addUninitialized(String recipeName) {
        uninitializedRecipes.add(new DeclarativeRecipe.LazyLoadedRecipe(recipeName));
    }

    public void addUninitializedPrecondition(Recipe recipe) {
        uninitializedPreconditions.add(recipe);
    }

    public void addUninitializedPrecondition(String recipeName) {
        uninitializedPreconditions.add(new DeclarativeRecipe.LazyLoadedRecipe(recipeName));
    }

    public void addValidation(Validated<Object> validated) {
        validation = validation.and(validated);
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = Validated.none();
        if (!initialized) {
            validated = validated
                    .and(requireInitialized(uninitializedRecipes, recipeList, "recipeList"))
                    .and(requireInitialized(uninitializedPreconditions, preconditions, "preconditions"));
        }
        return validated.and(validation).and(initValidation);
    }

    @Override
    public Collection<Validated<Object>> validateAll(ExecutionContext ctx, Collection<Validated<Object>> acc) {
        super.validateAll(ctx, acc);
        for (Recipe precondition : preconditions) {
            precondition.validateAll(ctx, acc);
        }
        return acc;
    }

    private Validated<Object> requireInitialized(List<Recipe> uninitialized, List<Recipe> resolved, String property) {
        if (uninitialized.isEmpty() || uninitialized.size() == resolved.size()) {
            return Validated.none();
        }
        List<String> declared = new ArrayList<>(uninitialized.size());
        for (Recipe recipe : uninitialized) {
            declared.add(recipe instanceof LazyLoadedRecipe ? ((LazyLoadedRecipe) recipe).getRecipeFqn() : recipe.getName());
        }
        return invalid(name + "." + property + " (in " + source + ")", declared,
                "has not been initialized; call initialize() on the DeclarativeRecipe before using it.");
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class LazyLoadedRecipe extends Recipe {
        String recipeFqn;

        String displayName = "Lazy loaded recipe";

        String description = "Recipe that is loaded lazily.";
    }

    @Override
    public DeclarativeRecipe clone() {
        return (DeclarativeRecipe) super.clone();
    }

    @Override
    protected RecipeDescriptor createRecipeDescriptor() {
        List<RecipeDescriptor> preconditionDescriptors = new ArrayList<>();
        for (Recipe childRecipe : preconditions) {
            preconditionDescriptors.add(childRecipe.getDescriptor());
        }
        List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
        for (Recipe childRecipe : deduplicateSingletonsRecursively(recipeList, new HashSet<>())) {
            recipeDescriptors.add(childRecipe.getDescriptor());
        }
        return new RecipeDescriptor(getName(), getDisplayName(), getInstanceName(), getDescription() != null ? getDescription() : "",
                getTags(), getEstimatedEffortPerOccurrence(),
                emptyList(), preconditionDescriptors, recipeDescriptors, aggregateDataTableDescriptors(recipeDescriptors), getMaintainers(), getContributors(),
                getExamples(), source);
    }

    @Override
    public List<Contributor> getContributors() {
        return emptyList();
    }

}
