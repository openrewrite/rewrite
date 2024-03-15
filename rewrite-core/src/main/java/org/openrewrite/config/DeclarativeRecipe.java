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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;

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

    @Setter
    private List<Recipe> recipeList = new ArrayList<>();

    private final List<Recipe> uninitializedPreconditions = new ArrayList<>();

    @Setter
    private List<Recipe> preconditions = new ArrayList<>();

    public void addPrecondition(Recipe recipe) {
        uninitializedPreconditions.add(recipe);
    }

    @JsonIgnore
    private Validated<Object> validation = Validated.none();
    @JsonIgnore
    private Validated<Object> initValidation = null;

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return estimatedEffortPerOccurrence == null ? Duration.ofMinutes(0) :
                estimatedEffortPerOccurrence;
    }

    public void initialize(Collection<Recipe> availableRecipes, Map<String, List<Contributor>> recipeToContributors) {
        initValidation = Validated.none();
        initialize(uninitializedRecipes, recipeList, availableRecipes, recipeToContributors);
        initialize(uninitializedPreconditions, preconditions, availableRecipes, recipeToContributors);
    }

    private void initialize(List<Recipe> uninitialized, List<Recipe> initialized, Collection<Recipe> availableRecipes, Map<String, List<Contributor>> recipeToContributors) {
        initialized.clear();
        for (int i = 0; i < uninitialized.size(); i++) {
            Recipe recipe = uninitialized.get(i);
            if (recipe instanceof LazyLoadedRecipe) {
                String recipeFqn = ((LazyLoadedRecipe) recipe).getRecipeFqn();
                Optional<Recipe> next = availableRecipes.stream()
                        .filter(r -> recipeFqn.equals(r.getName())).findAny();
                if (next.isPresent()) {
                    Recipe subRecipe = next.get();
                    if (subRecipe instanceof DeclarativeRecipe) {
                        ((DeclarativeRecipe) subRecipe).initialize(availableRecipes, recipeToContributors);
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
                recipe.setContributors(recipeToContributors.getOrDefault(recipe.getName(), emptyList()));
                if (recipe instanceof DeclarativeRecipe) {
                    ((DeclarativeRecipe) recipe).initialize(availableRecipes, recipeToContributors);
                }
                initialized.add(recipe);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor
    static class PreconditionBellwether extends Recipe {

        @Override
        public String getDisplayName() {
            return "Precondition bellwether";
        }

        @Override
        public String getDescription() {
            return "Evaluates a precondition and makes that result available to the preconditions of other recipes. " +
                   "\"bellwether\", noun - One that serves as a leader or as a leading indicator of future trends. ";
        }

        TreeVisitor<?, ExecutionContext> precondition;

        @NonFinal
        transient boolean preconditionApplicable;

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                    return precondition.isAcceptable(sourceFile, ctx);
                }

                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    Tree t = precondition.visit(tree, ctx);
                    preconditionApplicable = t != tree;
                    return tree;
                }
            };
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    static class BellwetherDecoratedRecipe extends Recipe {

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
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(bellwether.isPreconditionApplicable(), delegate.getVisitor());
        }

        @Override
        public List<Recipe> getRecipeList() {
            return decorateWithPreconditionBellwether(bellwether, delegate.getRecipeList());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class BellwetherDecoratedScanningRecipe<T> extends ScanningRecipe<T> {

        DeclarativeRecipe.PreconditionBellwether bellwether;
        ScanningRecipe<T> delegate;

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
        public T getInitialValue(ExecutionContext ctx) {
            return delegate.getInitialValue(ctx);
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(T acc) {
            return delegate.getScanner(acc);
        }

        @Override
        public Collection<? extends SourceFile> generate(T acc, ExecutionContext ctx) {
            return delegate.generate(acc, ctx);
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(T acc) {
            return Preconditions.check(bellwether.isPreconditionApplicable(), delegate.getVisitor(acc));
        }

        @Override
        public List<Recipe> getRecipeList() {
            return decorateWithPreconditionBellwether(bellwether, delegate.getRecipeList());
        }
    }

    @Override
    public final List<Recipe> getRecipeList() {
        if (preconditions.isEmpty()) {
            return recipeList;
        }

        TreeVisitor<?, ExecutionContext> andPreconditions = null;
        for (Recipe precondition : preconditions) {
            if (isScanningRecipe(precondition)) {
                throw new IllegalArgumentException(
                        getName() + " declares the ScanningRecipe " + precondition.getName() + " as a precondition." +
                        "ScanningRecipe cannot be used as Preconditions.");
            }
            if (andPreconditions == null) {
                andPreconditions = precondition.getVisitor();
            } else {
                andPreconditions = Preconditions.and(andPreconditions, precondition.getVisitor());
            }
        }
        PreconditionBellwether bellwether = new PreconditionBellwether(andPreconditions);
        List<Recipe> recipeListWithBellwether = new ArrayList<>(recipeList.size() + 1);
        recipeListWithBellwether.add(bellwether);
        recipeListWithBellwether.addAll(decorateWithPreconditionBellwether(bellwether, recipeList));
        return recipeListWithBellwether;
    }

    private static boolean isScanningRecipe(Recipe recipe) {
        if (recipe instanceof ScanningRecipe) {
            return true;
        }
        for (Recipe r : recipe.getRecipeList()) {
            if (isScanningRecipe(r)) {
                return true;
            }
        }
        return false;
    }

    private static List<Recipe> decorateWithPreconditionBellwether(PreconditionBellwether bellwether, List<Recipe> recipeList) {
        List<Recipe> mappedRecipeList = new ArrayList<>(recipeList.size());
        for (Recipe recipe : recipeList) {
            if (recipe instanceof ScanningRecipe) {
                mappedRecipeList.add(new BellwetherDecoratedScanningRecipe<>(bellwether, (ScanningRecipe<?>) recipe));
            } else {
                mappedRecipeList.add(new BellwetherDecoratedRecipe(bellwether, recipe));
            }
        }
        return mappedRecipeList;
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
        return Validated.<Object>test("initialization",
                        "initialize(..) must be called on DeclarativeRecipe prior to use.",
                        this, r -> initValidation != null)
                .and(validation)
                .and(initValidation);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
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
