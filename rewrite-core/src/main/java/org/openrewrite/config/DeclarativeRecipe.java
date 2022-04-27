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
import lombok.RequiredArgsConstructor;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.openrewrite.Validated.invalid;

@RequiredArgsConstructor
public class DeclarativeRecipe extends Recipe {
    private final String name;
    private final String displayName;
    private final String description;
    private final Set<String> tags;

    @Nullable
    private final Duration estimatedEffortPerOccurrence;

    private final URI source;
    private final List<String> lazyNext = new ArrayList<>();
    private final List<String> lazyExclude = new ArrayList<>();

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
            this, r -> lazyNext.isEmpty());

    @Override
    public Set<String> getTags() {
        return tags;
    }

    void initialize(Collection<Recipe> availableRecipes) {
        for (int i = 0; i < lazyNext.size(); i++) {
            String nextName = lazyNext.get(i);
            Optional<Recipe> next = availableRecipes.stream()
                    .filter(r -> r.getName().equals(nextName)).findAny();
            if (next.isPresent()) {
                doNext(next.get());
            } else {
                validation = validation.and(
                        invalid(name + ".recipeList[" + i + "] (in " + source + ")",
                                nextName,
                                "recipe '" + nextName + "' does not exist.",
                                null));
            }
        }
        lazyNext.clear();

        for (String nextName : lazyExclude) {
            Optional<Recipe> next = availableRecipes.stream()
                    .filter(r -> r.getName().equals(nextName)).findAny();
            next.ifPresent(excludeRecipeList::add);
        }
        lazyExclude.clear();
    }

    void doNext(String recipeName) {
        try {
            doNext((Recipe) Class.forName(recipeName).getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            lazyNext.add(recipeName);
        }
    }

    void excludeRecipe(String recipeName) {
        try {
            excludeRecipe((Recipe) Class.forName(recipeName).getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            lazyExclude.add(recipeName);
        }
    }

    @JsonIgnore
    private final List<Recipe> excludeRecipeList = new CopyOnWriteArrayList<>();

    public List<Recipe> getExcludeRecipeList() {
        return excludeRecipeList;
    }

    /**
     * @param recipe {@link Recipe} to exclude from this recipe's pipeline.
     */
    public void excludeRecipe(Recipe recipe) {
        excludeRecipeList.add(recipe);
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
}
