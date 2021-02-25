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
import org.openrewrite.Recipe;
import org.openrewrite.Validated;

import java.net.URI;
import java.util.*;

import static org.openrewrite.Validated.invalid;

public class DeclarativeRecipe extends Recipe {
    private final String name;
    private final String displayName;
    private final String description;
    private final Set<String> tags;
    private final URI source;
    private final List<String> lazyNext = new ArrayList<>();

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

    public DeclarativeRecipe(String name, String displayName, String description, Set<String> tags, URI source) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.tags = tags;
        this.source = source;
    }

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
                                "refers to a recipe that doesn't exist.",
                                null));
            }
        }
        lazyNext.clear();
    }

    void doNext(String recipeName) {
        try {
            doNext((Recipe) Class.forName(recipeName).getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            lazyNext.add(recipeName);
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
}
