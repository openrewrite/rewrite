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
    public List<Recipe> getEditPreconditions() {
        return preconditions;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return TreeVisitor.noop();
    }

    public static class Accumulator {
    }

    @Override
    public final List<Recipe> getRecipeList() {
        return deduplicateSingletonsRecursively(recipeList, new HashSet<>());
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
