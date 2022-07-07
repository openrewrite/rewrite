/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.test;

import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.TypeValidation;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

public class RecipeSpec {
    public static RecipeSpec defaults() {
        return new RecipeSpec();
    }

    @Nullable
    Recipe recipe;

    /**
     * Default parsers to use if no more specific parser is set
     * on the {@link SourceSpec}.
     */
    List<Parser<?>> parsers = new ArrayList<>();

    @Nullable
    ExecutionContext executionContext;

    @Nullable
    Path relativeTo;

    @Nullable
    Integer cycles;

    @Nullable
    Integer expectedCyclesThatMakeChanges;

    @Nullable
    TypeValidation typeValidation;

    boolean serializationValidation = true;

    Consumer<List<SourceFile>> beforeRecipe = s -> {
    };

    Consumer<List<Result>> afterRecipe = r -> {
    };

    // The before and after here don't mean anything
    SourceSpec<SourceFile> allSources = new SourceSpec<>(SourceFile.class, null, "", null);

    /**
     * Configuration that applies to all source file inputs.
     */
    public SourceSpec<SourceFile> allSources() {
        return allSources;
    }

    public RecipeSpec recipe(Recipe recipe) {
        this.recipe = recipe;
        return this;
    }

    public RecipeSpec recipe(InputStream yaml, String... recipes) {
        return recipe(Environment.builder()
                .load(new YamlResourceLoader(yaml, URI.create("rewrite.yml"), new Properties()))
                .build()
                .activateRecipes(recipes));
    }

    public RecipeSpec recipe(String yamlResource, String... recipes) {
        return recipe(Objects.requireNonNull(RecipeSpec.class.getResourceAsStream(yamlResource)), recipes);
    }

    public RecipeSpec parser(Parser<?> parser) {
        this.parsers.add(parser);
        return this;
    }

    public RecipeSpec executionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        return this;
    }

    public RecipeSpec relativeTo(@Nullable Path relativeTo) {
        this.relativeTo = relativeTo;
        return this;
    }

    public RecipeSpec cycles(int cycles) {
        this.cycles = cycles;
        return this;
    }

    public RecipeSpec beforeRecipe(Consumer<List<SourceFile>> beforeRecipe) {
        this.beforeRecipe = beforeRecipe;
        return this;
    }

    public RecipeSpec afterRecipe(Consumer<List<Result>> afterRecipe) {
        this.afterRecipe = afterRecipe;
        return this;
    }

    public RecipeSpec validateRecipeSerialization(boolean validate) {
        this.serializationValidation = validate;
        return this;
    }

    public RecipeSpec expectedCyclesThatMakeChanges(int expectedCyclesThatMakeChanges) {
        this.expectedCyclesThatMakeChanges = expectedCyclesThatMakeChanges;
        return this;
    }

    int getCycles() {
        return cycles == null ? 2 : cycles;
    }

    int getExpectedCyclesThatMakeChanges(int cycles) {
        return expectedCyclesThatMakeChanges == null ? cycles - 1 :
                expectedCyclesThatMakeChanges;
    }

    public RecipeSpec typeValidationOptions(TypeValidation typeValidation) {
        this.typeValidation = typeValidation;
        return this;
    }

    @Nullable
    ExecutionContext getExecutionContext() {
        return executionContext;
    }
}
