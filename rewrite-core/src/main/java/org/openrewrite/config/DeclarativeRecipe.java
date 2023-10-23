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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;

import java.net.URI;
import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.openrewrite.Validated.invalid;

@RequiredArgsConstructor
public class DeclarativeRecipe extends Recipe {
    @Getter
    private final String name;

    @Getter
    @Language("markdown")
    private final String displayName;

    @Getter
    @Language("markdown")
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
    private final List<Recipe> recipeList = new ArrayList<>();
    private final List<Recipe> preconditions = new ArrayList<>();

    public void addPrecondition(Recipe recipe) {
        preconditions.add(recipe);
    }

    @JsonIgnore
    private Validated<Object> validation = Validated.test("initialization",
            "initialize(..) must be called on DeclarativeRecipe prior to use.",
            this, r -> uninitializedRecipes.isEmpty());

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return estimatedEffortPerOccurrence == null ? super.getEstimatedEffortPerOccurrence() :
                estimatedEffortPerOccurrence;
    }

    public void initialize(Collection<Recipe> availableRecipes, Map<String, List<Contributor>> recipeToContributors) {
        for (int i = 0; i < uninitializedRecipes.size(); i++) {
            Recipe recipe = uninitializedRecipes.get(i);
            if (recipe instanceof LazyLoadedRecipe) {
                String recipeFqn = ((LazyLoadedRecipe) recipe).getRecipeFqn();
                Optional<Recipe> next = availableRecipes.stream()
                        .filter(r -> r.getName().equals(recipeFqn)).findAny();
                if (next.isPresent()) {
                    Recipe subRecipe = next.get();
                    if (subRecipe instanceof DeclarativeRecipe) {
                        ((DeclarativeRecipe) subRecipe).initialize(availableRecipes, recipeToContributors);
                    }
                    recipeList.add(subRecipe);
                } else {
                    validation = validation.and(
                            invalid(name + ".recipeList" +
                                    "[" + i + "] (in " + source + ")",
                                    recipeFqn,
                                    "recipe '" + recipeFqn + "' does not exist.",
                                    null));
                }
            } else {
                recipe.setContributors(recipeToContributors.getOrDefault(recipe.getName(), emptyList()));
                if (recipe instanceof DeclarativeRecipe) {
                    ((DeclarativeRecipe) recipe).initialize(availableRecipes, recipeToContributors);
                }
                recipeList.add(recipe);
            }
        }
        uninitializedRecipes.clear();
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    @RequiredArgsConstructor
    static class PreconditionBellwether extends Recipe {

        @Override
        public String getDisplayName() {
            return "Precondition bellwether";
        }

        @Override
        public String getDescription() {
            return "Evaluates a precondition and makes that result available to the preconditions of other recipes.";
        }

        TreeVisitor<?, ExecutionContext> precondition;

        @NonFinal
        transient boolean preconditionApplicable;

        /**
         * Returns a visitor, suitable for being used as a precondition, that returns as its result whatever this
         * bellwether evaluated to.
         */
        public TreeVisitor<?, ExecutionContext> getFollower() {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if(preconditionApplicable) {
                        return SearchResult.found(tree);
                    }
                    return tree;
                }
            };
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    Tree t = precondition.visit(tree, ctx);
                    preconditionApplicable = t != tree;
                    return tree;
                }
            };
        }
    }


    @Override
    public List<Recipe> getRecipeList() {
        if(preconditions.isEmpty()) {
            return recipeList;
        }

        //noinspection unchecked
        TreeVisitor<?, ExecutionContext> andPreconditions = Preconditions.and(
                preconditions.stream().map(Recipe::getVisitor).toArray(TreeVisitor[]::new));
        PreconditionBellwether bellwether = new PreconditionBellwether(andPreconditions);
        TreeVisitor<?, ExecutionContext> bellwetherFollower = bellwether.getFollower();
        List<Recipe> recipeListWithBellwether = new ArrayList<>(recipeList.size() + 1);
        recipeListWithBellwether.add(bellwether);
        for (Recipe recipe : recipeList) {
            if(recipe instanceof ScanningRecipe) {
                recipeListWithBellwether.add(new PreconditionDecoratedScanningRecipe<>(bellwetherFollower, (ScanningRecipe<?>) recipe));
            } else {
                recipeListWithBellwether.add(new PreconditionDecoratedRecipe(bellwetherFollower, recipe));
            }
        }

        return recipeListWithBellwether;
    }

    public void addUninitialized(Recipe recipe) {
        uninitializedRecipes.add(recipe);
    }

    public void addUninitialized(String recipeName) {
        uninitializedRecipes.add(new DeclarativeRecipe.LazyLoadedRecipe(recipeName));
    }

    public void addValidation(Validated<Object> validated) {
        validation = validation.and(validated);
    }

    @Override
    public Validated<Object> validate() {
        return validation;
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class LazyLoadedRecipe extends Recipe {
        String recipeFqn;

        @Override
        public String getDisplayName() {
            return "Lazy loaded recipe";
        }

        @Override
        public String getDescription() {
            return "Recipe that is loaded lazily.";
        }
    }

    @Override
    protected RecipeDescriptor createRecipeDescriptor() {
        List<RecipeDescriptor> recipeList = new ArrayList<>();
        for (Recipe childRecipe : getRecipeList()) {
            recipeList.add(childRecipe.getDescriptor());
        }
        return new RecipeDescriptor(getName(), getDisplayName(), getDescription(),
                getTags(), getEstimatedEffortPerOccurrence(),
                emptyList(), recipeList, getDataTableDescriptors(), getMaintainers(), getContributors(),
                getExamples(), source);
    }

    @Value
    private static class NameEmail {
        String name;
        String email;
    }

    @Override
    public List<Contributor> getContributors() {
        if (contributors == null) {
            Map<NameEmail, Integer> contributorToLineCount = new HashMap<>();
            contributors = new ArrayList<>();
            for (Recipe recipe : getRecipeList()) {
                for (Contributor contributor : recipe.getContributors()) {
                    NameEmail nameEmail = new NameEmail(contributor.getName(), contributor.getEmail());
                    contributorToLineCount.put(nameEmail, contributorToLineCount.getOrDefault(nameEmail, 0) + contributor.getLineCount());
                }
            }
            for (Map.Entry<NameEmail, Integer> contributorEntry : contributorToLineCount.entrySet()) {
                contributors.add(new Contributor(contributorEntry.getKey().getName(), contributorEntry.getKey().getEmail(), contributorEntry.getValue()));
            }
            contributors.sort(Comparator.comparing(Contributor::getLineCount).reversed());
        }
        return contributors;
    }
}
