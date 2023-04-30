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
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;

import java.net.URI;
import java.time.Duration;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.openrewrite.Validated.invalid;

@RequiredArgsConstructor
public class DeclarativeRecipe extends ScanningRecipe<Map<Recipe, Object>> {
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

    private final List<Recipe> uninitializedApplicabilityTests = new ArrayList<>();
    private final List<Recipe> applicabilityTests = new ArrayList<>();
    private final List<Recipe> uninitializedRecipes = new ArrayList<>();
    private final List<Recipe> recipeList = new ArrayList<>();

    @JsonIgnore
    private Validated validation = Validated.test("initialization",
            "initialize(..) must be called on DeclarativeRecipe prior to use.",
            this, r -> uninitializedRecipes.isEmpty());

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return estimatedEffortPerOccurrence == null ? super.getEstimatedEffortPerOccurrence() :
                estimatedEffortPerOccurrence;
    }

    public void initialize(Collection<Recipe> availableRecipes, Map<String, List<Contributor>> recipeToContributors) {
        initialize(availableRecipes, recipeToContributors, uninitializedApplicabilityTests, applicabilityTests);
        initialize(availableRecipes, recipeToContributors, uninitializedRecipes, recipeList);
    }

    private void initialize(
            Collection<Recipe> availableRecipes,
            Map<String, List<Contributor>> recipeToContributors,
            List<Recipe> uninitialized,
            List<Recipe> initialized) {
        for (int i = 0; i < uninitialized.size(); i++) {
            Recipe recipe = uninitialized.get(i);
            if (recipe instanceof LazyLoadedRecipe) {
                String recipeFqn = ((LazyLoadedRecipe) recipe).getRecipeFqn();
                Optional<Recipe> next = availableRecipes.stream()
                        .filter(r -> r.getName().equals(recipeFqn)).findAny();
                if (next.isPresent()) {
                    initialized.add(next.get());
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
                initialized.add(recipe);
            }
        }
        uninitialized.clear();
    }

    public void addUninitialized(Recipe recipe) {
        uninitializedRecipes.add(recipe);
    }

    public void addUninitialized(String recipeName, @Nullable ClassLoader classLoader) {
        try {
            uninitializedRecipes.add((Recipe) Class.forName(recipeName, true, classLoader != null ? classLoader : this.getClass().getClassLoader())
                    .getDeclaredConstructor()
                    .newInstance());
        } catch (Exception e) {
            uninitializedRecipes.add(new DeclarativeRecipe.LazyLoadedRecipe(recipeName));
        }
    }

    public void addValidation(Validated validated) {
        validation = validation.and(validated);
    }

    @Override
    public Validated validate() {
        return validation;
    }

    @Override
    public Map<Recipe, Object> getInitialValue() {
        HashMap<Recipe, Object> acc = new HashMap<>();
        initialValues(acc, applicabilityTests);
        initialValues(acc, recipeList);
        return acc;
    }
    private static void initialValues(Map<Recipe, Object> acc, List<Recipe> recipes) {
        for (Recipe recipe : recipes) {
            if (recipe instanceof ScanningRecipe) {
                acc.put(recipe, ((ScanningRecipe<?>) recipe).getInitialValue());
            }
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<Recipe, Object> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                for(Recipe applicabilityTest : recipeList) {
                    if(!(applicabilityTest instanceof ScanningRecipe)) {
                        continue;
                    }
                    Object testAcc = acc.get(applicabilityTest);
                    if(testAcc == null) {
                        continue;
                    }
                    //noinspection unchecked,rawtypes
                    ((ScanningRecipe) applicabilityTest).getScanner(testAcc).visit(tree, executionContext);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<Recipe, Object> acc) {
        TreeVisitor<?, ExecutionContext> visitor = new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t = tree;
                for(Recipe recipe : recipeList) {
                    if(recipe instanceof ScanningRecipe) {
                        //noinspection unchecked,rawtypes
                        t = ((ScanningRecipe)recipe).getVisitor(acc.get(recipe)).visit(t, ctx);
                    } else {
                        t = recipe.getVisitor().visit(t, ctx);
                    }
                }
                return t;
            }
        };
        if(applicabilityTests.isEmpty()) {
            return visitor;
        }
        List<TreeVisitor<?, ExecutionContext>> tests = new ArrayList<>(applicabilityTests.size());
        for (Recipe applicabilityTest : applicabilityTests) {
            if(applicabilityTest instanceof ScanningRecipe) {
                //noinspection unchecked,rawtypes
                tests.add(((ScanningRecipe)applicabilityTest).getVisitor(acc.get(applicabilityTest)));
            } else {
                tests.add(applicabilityTest.getVisitor());
            }
        }
        //noinspection unchecked
        TreeVisitor<?, ExecutionContext>[] applicabilityTestArr = tests.toArray(new TreeVisitor[0]);
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(applicabilityTestArr);

        return Preconditions.check(check, visitor);
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class LazyLoadedRecipe extends Recipe {
        String recipeFqn;

        @Override
        public String getDisplayName() {
            return "Lazy loaded recipe";
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
