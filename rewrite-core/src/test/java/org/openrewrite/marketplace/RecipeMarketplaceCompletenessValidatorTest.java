/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.marketplace;

import org.junit.jupiter.api.Test;
import org.openrewrite.Validated;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class RecipeMarketplaceCompletenessValidatorTest {

    private final RecipeMarketplaceCompletenessValidator validator = new RecipeMarketplaceCompletenessValidator();

    @Test
    void validMarketplace() {
        // Create an environment with a recipe
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yaml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.TestRecipe
              displayName: Test Recipe
              description: A test recipe.
              recipeList:
                - org.openrewrite.text.ChangeText:
                    toText: Hello
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        // Create a marketplace with the same recipe
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();
        marketplace.getRecipes().add(new RecipeOffering(
          "test.TestRecipe",
          "Test recipe",
          "Test recipe",
          "A test recipe.",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace, env);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void detectsPhantomRecipe() {
        // Create an environment with no recipes
        Environment env = Environment.builder().build();

        // Create a marketplace with a recipe that doesn't exist in the JAR
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();
        marketplace.getRecipes().add(new RecipeOffering(
          "test.PhantomRecipe",
          "Phantom recipe",
          "Phantom recipe",
          "This recipe doesn't exist in JAR.",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace, env);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().getFirst().getMessage()).contains("does not exist in JAR");
        assertThat(result.failures().getFirst().getMessage()).contains("test.PhantomRecipe");
    }

    @Test
    void detectsMissingRecipe() {
        // Create an environment with a recipe
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yaml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.MissingRecipe
              displayName: Missing Recipe
              description: A recipe missing from CSV.
              recipeList:
                - org.openrewrite.text.ChangeText:
                    toText: Hello
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        // Create an empty marketplace (missing the recipe)
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();

        Validated<RecipeMarketplace> result = validator.validate(marketplace, env);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().getFirst().getMessage()).contains("not listed in CSV");
        assertThat(result.failures().getFirst().getMessage()).contains("test.MissingRecipe");
    }

    @Test
    void detectsBothPhantomAndMissingRecipes() {
        // Create an environment with one recipe
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yaml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.JarRecipe
              displayName: JAR Recipe
              description: A recipe in the JAR.
              recipeList:
                - org.openrewrite.text.ChangeText:
                    toText: Hello
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        // Create a marketplace with a different recipe
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();
        marketplace.getRecipes().add(new RecipeOffering(
          "test.CsvRecipe",
          "CSV recipe",
          "CSV recipe",
          "A recipe only in CSV.",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace, env);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.failures()).hasSize(2);

        // Should have one phantom recipe error
        assertThat(result.failures()).anyMatch(failure ->
          failure.getMessage().contains("test.CsvRecipe") &&
          failure.getMessage().contains("does not exist in JAR")
        );

        // Should have one missing recipe error
        assertThat(result.failures()).anyMatch(failure ->
          failure.getMessage().contains("test.JarRecipe") &&
          failure.getMessage().contains("not listed in CSV")
        );
    }

    @Test
    void allowsSameRecipeInMultipleCategories() {
        // Create an environment with one recipe
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yaml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.SharedRecipe
              displayName: Shared Recipe
              description: A recipe that appears in multiple categories.
              recipeList:
                - org.openrewrite.text.ChangeText:
                    toText: Hello
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        // Create a marketplace with the same recipe in multiple categories
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();
        RecipeMarketplace category1 = new RecipeMarketplace("Category1", "");
        RecipeMarketplace category2 = new RecipeMarketplace("Category2", "");
        marketplace.getCategories().add(category1);
        marketplace.getCategories().add(category2);

        category1.getRecipes().add(new RecipeOffering(
          "test.SharedRecipe",
          "Shared recipe",
          "Shared recipe",
          "A recipe that appears in multiple categories.",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        category2.getRecipes().add(new RecipeOffering(
          "test.SharedRecipe",
          "Shared recipe",
          "Shared recipe",
          "A recipe that appears in multiple categories.",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace, env);
        assertThat(result.isValid()).isTrue();
    }
}
