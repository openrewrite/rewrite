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

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
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
    private List<Recipe> recipeList = new ArrayList<>();

    private final List<Recipe> uninitializedPreconditions = new ArrayList<>();

    @Getter
    @Setter
    private List<Recipe> preconditions = new ArrayList<>();

    public void addPrecondition(Recipe recipe) {
        uninitializedPreconditions.add(recipe);
    }

    @JsonIgnore
    private Validated<Object> validation = Validated.none();

    @JsonIgnore
    private Validated<Object> initValidation = Validated.none();

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return estimatedEffortPerOccurrence == null ? Duration.ofMinutes(0) :
                estimatedEffortPerOccurrence;
    }

    public void initialize(Collection<Recipe> availableRecipes) {
        Map<String, Recipe> recipeMap = new HashMap<>();
        availableRecipes.forEach(r -> recipeMap.putIfAbsent(r.getName(), r));
        Set<String> initializingRecipes = new HashSet<>();
        initialize(uninitializedRecipes, recipeList, recipeMap::get, initializingRecipes);
        initialize(uninitializedPreconditions, preconditions, recipeMap::get, initializingRecipes);
    }

    @Deprecated
    public void initialize(Collection<Recipe> availableRecipes,
                           @SuppressWarnings("unused") Map<String, List<Contributor>> recipeToContributors) {
        this.initialize(availableRecipes);
    }

    public void initialize(Function<String, @Nullable Recipe> availableRecipes) {
        Set<String> initializingRecipes = new HashSet<>();
        initialize(uninitializedRecipes, recipeList, availableRecipes, initializingRecipes);
        initialize(uninitializedPreconditions, preconditions, availableRecipes, initializingRecipes);
    }

    @Deprecated
    public void initialize(Function<String, @Nullable Recipe> availableRecipes,
                           @SuppressWarnings("unused") Map<String, List<Contributor>> recipeToContributors) {
        this.initialize(availableRecipes);
    }

    private void initialize(List<Recipe> uninitialized, List<Recipe> initialized, Function<String, @Nullable Recipe> availableRecipes, Set<String> initializingRecipes) {
        initialized.clear();
        for (int i = 0; i < uninitialized.size(); i++) {
            Recipe recipe = uninitialized.get(i);
            if (recipe instanceof LazyLoadedRecipe) {
                String recipeFqn = ((LazyLoadedRecipe) recipe).getRecipeFqn();
                Recipe subRecipe = availableRecipes.apply(recipeFqn);
                if (subRecipe != null) {
                    if (subRecipe instanceof DeclarativeRecipe) {
                        initializeDeclarativeRecipe((DeclarativeRecipe) subRecipe, recipeFqn, availableRecipes, initializingRecipes);
                    }
                    initialized.add(subRecipe);
                } else {
                    initValidation = initValidation.and(
                            invalid(name + ".recipeList" +
                                    "[" + i + "] (in " + source + ")",
                                    recipeFqn,
                                    "recipe '" + recipeFqn + "' does not exist.",
                                    null));
                }
            } else {
                if (recipe instanceof DeclarativeRecipe) {
                    initializeDeclarativeRecipe((DeclarativeRecipe) recipe, recipe.getName(), availableRecipes, initializingRecipes);
                }
                initialized.add(recipe);
            }
        }
    }

    private void initializeDeclarativeRecipe(DeclarativeRecipe declarativeRecipe, String recipeIdentifier,
                                             Function<String, @Nullable Recipe> availableRecipes, Set<String> initializingRecipes) {
        String recipeName = declarativeRecipe.getName();
        if (initializingRecipes.contains(recipeName)) {
            // Cycle detected - throw exception to fail fast
            String cycle = String.join(" -> ", initializingRecipes) + " -> " + recipeName;
            throw new RecipeIntrospectionException(
                    "Recipe '" + recipeIdentifier + "' creates a cycle: " + cycle);
        } else {
            initializingRecipes.add(recipeName);
            declarativeRecipe.initialize(declarativeRecipe.uninitializedRecipes, declarativeRecipe.recipeList, availableRecipes, initializingRecipes);
            declarativeRecipe.initialize(declarativeRecipe.uninitializedPreconditions, declarativeRecipe.preconditions, availableRecipes, initializingRecipes);
            initializingRecipes.remove(recipeName);
        }
    }

    @JsonIgnore
    private transient ThreadLocal<Accumulator> accumulator = new ThreadLocal<>();

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        Accumulator acc = new Accumulator();
        for (Recipe precondition : preconditions) {
            registerNestedScanningRecipes(precondition, acc, ctx);
        }
        accumulator.set(acc);
        return acc;
    }

    private void registerNestedScanningRecipes(Recipe recipe, Accumulator acc, ExecutionContext ctx) {
        if (recipe instanceof ScanningRecipe && isScanningRequired(recipe)) {
            acc.recipeToAccumulator.put(recipe, ((ScanningRecipe<?>) recipe).getInitialValue(ctx));
        }
        // Recurse into DeclarativeRecipes using raw fields (preconditions + recipeList)
        // to avoid getRecipeList() which wraps entries with bellwethers.
        // Don't recurse into leaf ScanningRecipes — they have no precondition children.
        if (recipe instanceof DeclarativeRecipe) {
            DeclarativeRecipe dr = (DeclarativeRecipe) recipe;
            for (Recipe precondition : dr.preconditions) {
                registerNestedScanningRecipes(precondition, acc, ctx);
            }
            for (Recipe r : dr.recipeList) {
                registerNestedScanningRecipes(r, acc, ctx);
            }
        } else if (!(recipe instanceof ScanningRecipe)) {
            for (Recipe nested : recipe.getRecipeList()) {
                registerNestedScanningRecipes(nested, acc, ctx);
            }
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
        // Recurse into nested DeclarativeRecipes used as preconditions, scanning their
        // raw preconditions and recipeList fields directly. We avoid getRecipeList() which
        // wraps entries with bellwethers. Leaf ScanningRecipes (e.g. AddDependency) are
        // not recursed into — their recipeList is scanned during recipe execution.
        if (recipe instanceof DeclarativeRecipe) {
            DeclarativeRecipe nested = (DeclarativeRecipe) recipe;
            for (Recipe precondition : nested.preconditions) {
                scanNestedScanningRecipes(precondition, acc, tree, ctx);
            }
            for (Recipe r : nested.recipeList) {
                scanNestedScanningRecipes(r, acc, tree, ctx);
            }
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

        Supplier<TreeVisitor<?, ExecutionContext>> precondition;

        @NonFinal
        transient boolean preconditionApplicable;

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new TreeVisitor<Tree, ExecutionContext>() {
                final TreeVisitor<?, ExecutionContext> p = precondition.get();

                @Override
                public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                    return p.isAcceptable(sourceFile, ctx);
                }

                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    Tree t = p.visit(tree, ctx);
                    preconditionApplicable = t != tree;
                    return tree;
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
        if (preconditions.isEmpty()) {
            return recipeList;
        }

        List<Supplier<TreeVisitor<?, ExecutionContext>>> andPreconditions = new ArrayList<>();
        for (Recipe precondition : preconditions) {
            andPreconditions.add(() -> orVisitors(precondition));
        }
        //noinspection unchecked
        PreconditionBellwether bellwether = new PreconditionBellwether(Preconditions.and(andPreconditions.toArray(new Supplier[]{})));
        List<Recipe> recipeListWithBellwether = new ArrayList<>(recipeList.size() + 1);
        recipeListWithBellwether.add(bellwether);
        recipeListWithBellwether.addAll(decorateWithPreconditionBellwether(bellwether, recipeList));
        return recipeListWithBellwether;
    }

    private TreeVisitor<?, ExecutionContext> orVisitors(Recipe recipe) {
        List<TreeVisitor<?, ExecutionContext>> conditions = new ArrayList<>();
        if (recipe instanceof ScanningRecipe) {
            //noinspection rawtypes
            ScanningRecipe scanning = (ScanningRecipe) recipe;
            //noinspection unchecked
            Accumulator acc = accumulator.get();
            conditions.add(scanning.getVisitor(acc != null ? acc.recipeToAccumulator.get(scanning) : null));
        } else {
            conditions.add(recipe.getVisitor());
        }
        for (Recipe r : recipe.getRecipeList()) {
            conditions.add(orVisitors(r));
        }
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        //noinspection unchecked
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

        if (!uninitializedRecipes.isEmpty() && uninitializedRecipes.size() != recipeList.size()) {
            validated = validated.and(Validated.invalid("initialization", recipeList, "DeclarativeRecipe must not contain uninitialized recipes. Be sure to call .initialize() on DeclarativeRecipe."));
        }
        if (!uninitializedPreconditions.isEmpty() && uninitializedPreconditions.size() != preconditions.size()) {
            validated = validated.and(Validated.invalid("initialization", preconditions, "DeclarativeRecipe must not contain uninitialized preconditions. Be sure to call .initialize() on DeclarativeRecipe."));
        }

        return validated.and(validation)
                .and(initValidation == null ? Validated.none() : initValidation);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class LazyLoadedRecipe extends Recipe {
        String recipeFqn;

        String displayName = "Lazy loaded recipe";

        String description = "Recipe that is loaded lazily.";
    }

    @Override
    public void onComplete(ExecutionContext ctx) {
        accumulator.remove();
    }

    @Override
    public DeclarativeRecipe clone() {
        DeclarativeRecipe cloned = (DeclarativeRecipe) super.clone();
        cloned.accumulator = new ThreadLocal<>();
        return cloned;
    }

    @Override
    protected RecipeDescriptor createRecipeDescriptor() {
        List<RecipeDescriptor> preconditionDescriptors = new ArrayList<>();
        for (Recipe childRecipe : preconditions) {
            preconditionDescriptors.add(childRecipe.getDescriptor());
        }
        List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
        for (Recipe childRecipe : recipeList) {
            recipeDescriptors.add(childRecipe.getDescriptor());
        }
        return new RecipeDescriptor(getName(), getDisplayName(), getInstanceName(), getDescription() != null ? getDescription() : "",
                getTags(), getEstimatedEffortPerOccurrence(),
                emptyList(), preconditionDescriptors, recipeDescriptors, getDataTableDescriptors(), getMaintainers(), getContributors(),
                getExamples(), source);
    }

    @Override
    public List<Contributor> getContributors() {
        return emptyList();
    }

    @Override
    public List<DataTableDescriptor> getDataTableDescriptors() {
        List<DataTableDescriptor> dataTableDescriptors = null;
        for (Recipe recipe : getRecipeList()) {
            List<DataTableDescriptor> dtds = recipe.getDataTableDescriptors();
            if (!dtds.isEmpty()) {
                if (dataTableDescriptors == null) {
                    dataTableDescriptors = new ArrayList<>();
                }
                for (DataTableDescriptor dtd : dtds) {
                    if (!dataTableDescriptors.contains(dtd)) {
                        dataTableDescriptors.add(dtd);
                    }
                }
            }
        }
        return dataTableDescriptors == null ? super.getDataTableDescriptors() : dataTableDescriptors;
    }
}
