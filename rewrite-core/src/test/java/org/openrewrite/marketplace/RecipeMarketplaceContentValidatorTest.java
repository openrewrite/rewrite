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

import java.util.Collections;

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
          Collections.emptySet(),
          null,
          Collections.emptyList(),
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
          Collections.emptySet(),
          null,
          Collections.emptyList(),
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
          Collections.emptySet(),
          null,
          Collections.emptyList(),
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
        marketplace.getRecipes().add(new RecipeOffering(
          "org.example.TestRecipe",
          "Test recipe",
          "Test recipe",
          "Valid description.",
          Collections.emptySet(),
          null,
          Collections.emptyList(),
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
          Collections.emptySet(),
          null,
          Collections.emptyList(),
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
          Collections.emptySet(),
          null,
          Collections.emptyList(),
          null
        ));
        marketplace.getRecipes().add(new RecipeOffering(
          "org.example.TestRecipe2",
          "Another recipe.",
          "Another recipe.",
          "Valid description.",
          Collections.emptySet(),
          null,
          Collections.emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(marketplace);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.failures()).hasSize(3); // lowercase displayName, missing period in description, period in displayName
    }

    @Test
    void validatesRecipesInSubcategories() {
        RecipeMarketplace root = RecipeMarketplace.newEmpty();
        RecipeMarketplace category = new RecipeMarketplace("Java", "Java recipes.");
        root.getCategories().add(category);

        category.getRecipes().add(new RecipeOffering(
          "org.example.JavaRecipe",
          "Java recipe",
          "Java recipe",
          "Missing period",
          Collections.emptySet(),
          null,
          Collections.emptyList(),
          null
        ));

        Validated<RecipeMarketplace> result = validator.validate(root);
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.failures()).hasSize(2); // lowercase displayName + missing period
    }
}
