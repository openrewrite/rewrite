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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.*;

import static org.openrewrite.Validated.invalid;

@RequiredArgsConstructor
public class DeclarativeRecipe extends CompositeRecipe {
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

    private final Map<RecipeUse, List<Recipe>> uninitializedRecipes = new HashMap<>();

    private final boolean causesAnotherCycle;

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return estimatedEffortPerOccurrence == null ?
                super.getEstimatedEffortPerOccurrence() :
                estimatedEffortPerOccurrence;
    }

    @Override
    public boolean causesAnotherCycle() {
        return causesAnotherCycle || super.causesAnotherCycle();
    }

    @JsonIgnore
    private Validated validation = Validated.test("initialization",
            "initialize(..) must be called on DeclarativeRecipe prior to use.",
            this, r -> uninitializedRecipes.isEmpty());

    public void initialize(Collection<Recipe> availableRecipes) {
        for (Map.Entry<RecipeUse, List<Recipe>> recipesByUse : uninitializedRecipes.entrySet()) {
            for (int i = 0; i < recipesByUse.getValue().size(); i++) {
                Recipe recipe = recipesByUse.getValue().get(i);
                if (recipe instanceof LazyLoadedRecipe) {
                    String recipeFqn = ((LazyLoadedRecipe) recipe).getRecipeFqn();
                    Optional<Recipe> next = availableRecipes.stream()
                            .filter(r -> r.getName().equals(recipeFqn)).findAny();
                    if (next.isPresent()) {
                        configureByUse(recipesByUse.getKey(), next.get());
                    } else {
                        String useProperty = "";
                        switch (recipesByUse.getKey()) {
                            case SingleSourceApplicability:
                                useProperty = "applicability.singleSource";
                                break;
                            case AnySourceApplicability:
                                useProperty = "applicability.anySource";
                                break;
                            case Recipe:
                                useProperty = "recipeList";
                                break;
                        }
                        validation = validation.and(
                                invalid(name + "." + useProperty +
                                        "[" + i + "] (in " + source + ")",
                                        recipeFqn,
                                        "recipe '" + recipeFqn + "' does not exist.",
                                        null));
                    }
                } else {
                    configureByUse(recipesByUse.getKey(), recipe);
                }
            }
        }
        uninitializedRecipes.clear();
    }

    private void configureByUse(RecipeUse use, Recipe recipe) {
        switch(use) {
            case SingleSourceApplicability:
                addSingleSourceApplicableTest(getVisitor(recipe));
                break;
            case AnySourceApplicability:
                addApplicableTest(getVisitor(recipe));
                break;
            case Recipe:
                doNext(recipe);
                break;
        }
    }

    private TreeVisitor<?, ExecutionContext> getVisitor(Recipe recipe) {
        try {
            Method getVisitor = recipe.getClass().getDeclaredMethod("getVisitor");
            getVisitor.setAccessible(true);
            //noinspection unchecked
            return (TreeVisitor<?, ExecutionContext>) getVisitor.invoke(recipe);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void addUninitialized(RecipeUse use, Recipe recipe) {
        uninitializedRecipes.computeIfAbsent(use, u -> new ArrayList<>()).add(recipe);
    }

    public void addUninitialized(RecipeUse use, String recipeName, @Nullable ClassLoader classLoader) {
        try {
            uninitializedRecipes.computeIfAbsent(use, u -> new ArrayList<>())
                    .add((Recipe) Class.forName(recipeName, true, classLoader != null ? classLoader : this.getClass().getClassLoader())
                            .getDeclaredConstructor()
                            .newInstance());
        } catch (Exception e) {
            uninitializedRecipes.computeIfAbsent(use, u -> new ArrayList<>())
                    .add(new DeclarativeRecipe.LazyLoadedRecipe(recipeName));
        }
    }

    public void addValidation(Validated validated) {
        validation = validation.and(validated);
    }

    @Override
    public Validated validate() {
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
    }


    public enum RecipeUse {
        /**
         * If multiple applicable tests configured for SingleSourceApplicability or AnySourceApplicability, the final
         * result of the applicable test depends on all conditions being met, that is, a logical 'AND' relationship.
         */
        SingleSourceApplicability,
        AnySourceApplicability,
        Recipe
    }
}
