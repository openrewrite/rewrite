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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class RecipeMarketplaceContentValidatorTest {

    private final RecipeMarketplaceContentValidator validator = new RecipeMarketplaceContentValidator();

    @Test
    void validRecipe() {
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();
        marketplace.getRecipes().add(new RecipeOffering(
          "org.example.TestRecipe",
          "Test recipe",
          "Test recipe",
          "This is a valid description.",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void descriptionMustEndWithPeriod() {
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();
        marketplace.getRecipes().add(new RecipeOffering(
          "org.example.TestRecipe",
          "Test recipe",
          "Test recipe",
          "This description is missing a period",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().getFirst().getMessage()).contains("must end with a period");
    }

    @Test
    void displayNameMustNotEndWithPeriod() {
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();
        marketplace.getRecipes().add(new RecipeOffering(
          "org.example.TestRecipe",
          "Test recipe.",
          "Test recipe.",
          "Valid description.",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().getFirst().getMessage()).contains("must not end with a period");
    }

    @Test
    void displayNameMustStartWithUppercase() {
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();
        //noinspection DialogTitleCapitalization
        marketplace.getRecipes().add(new RecipeOffering(
          "org.example.TestRecipe",
          "test recipe",
          "Test recipe",
          "Valid description.",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().getFirst().getMessage()).contains("must start with an uppercase letter");
    }

    @Test
    void emptyDescriptionIsAllowed() {
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();
        marketplace.getRecipes().add(new RecipeOffering(
          "org.example.TestRecipe",
          "Test recipe",
          "Test recipe",
          "",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void multipleErrorsAreCollected() {
        RecipeMarketplace marketplace = RecipeMarketplace.newEmpty();
        marketplace.getRecipes().add(new RecipeOffering(
          "org.example.TestRecipe1",
          "Test recipe",
          "Test recipe",
          "Missing period",
          emptySet(),
          null,
          emptyList(),
          null
        ));
        //noinspection DialogTitleCapitalization
        marketplace.getRecipes().add(new RecipeOffering(
          "org.example.TestRecipe2",
          "another recipe.",
          "Another recipe",
          "Valid description",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace);
        assertThat(result.isInvalid()).isTrue();
        // TestRecipe1: missing period in description
        // TestRecipe2: lowercase displayName, period in displayName, missing period in description
        assertThat(result.failures()).hasSize(4);
    }

    @Test
    void validatesRecipesInSubcategories() {
        RecipeMarketplace root = RecipeMarketplace.newEmpty();
        RecipeMarketplace category = new RecipeMarketplace("Java", "Java recipes.");
        root.getCategories().add(category);

        //noinspection DialogTitleCapitalization
        category.getRecipes().add(new RecipeOffering(
          "org.example.JavaRecipe",
          "java recipe",
          "Java recipe",
          "Missing period",
          emptySet(),
          null,
          emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(root);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.failures()).hasSize(2); // lowercase displayName + missing period
    }
}
