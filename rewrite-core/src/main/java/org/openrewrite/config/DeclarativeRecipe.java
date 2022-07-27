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
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;

import java.net.URI;
import java.time.Duration;
import java.util.*;

import static org.openrewrite.Validated.invalid;

@RequiredArgsConstructor
public class DeclarativeRecipe extends Recipe {
    private final String name;
    @Language("markdown")
    private final String displayName;

    @Language("markdown")
    private final String description;
    private final Set<String> tags;

    @Nullable
    private final Duration estimatedEffortPerOccurrence;

    private final URI source;

    private final /*~~>*/List<Recipe> uninitializedRecipes = new ArrayList<>();

    private final boolean causesAnotherCycle;

    @Override
    public boolean causesAnotherCycle() {
        return causesAnotherCycle || super.causesAnotherCycle();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @JsonIgnore
    private Validated validation = Validated.test("initialization",
            "initialize(..) must be called on DeclarativeRecipe prior to use.",
            this, r -> uninitializedRecipes.isEmpty());

    @Override
    public Set<String> getTags() {
        return tags;
    }

    void initialize(Collection<Recipe> availableRecipes) {
        for (int i = 0; i < uninitializedRecipes.size(); i++) {
            Recipe recipe = uninitializedRecipes.get(i);
            if(recipe instanceof LazyLoadedRecipe) {
                String recipeFqn = ((LazyLoadedRecipe) recipe).getRecipeFqn();
                Optional<Recipe> next = availableRecipes.stream()
                        .filter(r -> r.getName().equals(recipeFqn)).findAny();
                if (next.isPresent()) {
                    doNext(next.get());
                } else {
                    validation = validation.and(
                            invalid(name + ".recipeList[" + i + "] (in " + source + ")",
                                    recipeFqn,
                                    "recipe '" + recipeFqn + "' does not exist.",
                                    null));
                }
            } else {
                doNext(recipe);
            }
        }
        uninitializedRecipes.clear();
    }

    public void addUninitialized(Recipe recipe) {
        uninitializedRecipes.add(recipe);
    }

    void addUninitialized(String recipeName) {
        try {
            uninitializedRecipes.add((Recipe) Class.forName(recipeName).getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            uninitializedRecipes.add(new DeclarativeRecipe.LazyLoadedRecipe(recipeName));
        }
    }

    void addValidation(Validated validated) {
        validation = validation.and(validated);
    }

    @Override
    public Validated validate() {
        return validation;
    }

    @Override
    public String getName() {
        return name;
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
}
