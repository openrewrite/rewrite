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

import org.openrewrite.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the content quality and format of recipe marketplace entries.
 * Checks that descriptions and display names follow proper formatting rules.
 */
public class RecipeMarketplaceContentValidator {

    /**
     * Validate the content of a recipe marketplace.
     *
     * @param marketplace The marketplace to validate.
     * @return A validation result containing all errors found.
     */
    public Validated<RecipeMarketplace> validate(RecipeMarketplace marketplace) {
        Validated<RecipeMarketplace> validation = Validated.none();
        List<String> categoryPath = new ArrayList<>();
        return validateRecursive(marketplace, validation, categoryPath);
    }

    private Validated<RecipeMarketplace> validateRecursive(RecipeMarketplace marketplace,
                                                            Validated<RecipeMarketplace> validation,
                                                            List<String> categoryPath) {
        // Validate category itself (if not root)
        if (!marketplace.isRoot()) {
            validation = validation.and(validateDisplayName(marketplace.getDisplayName(), "category: " + marketplace.getDisplayName()));
            if (!marketplace.getDescription().isEmpty()) {
                validation = validation.and(validateDescription(marketplace.getDescription(), "category: " + marketplace.getDisplayName()));
            }
        }

        // Validate recipes in this category
        for (RecipeListing recipe : marketplace.getRecipes()) {
            String categoryContext = categoryPath.isEmpty() ? "root" : String.join(" > ", categoryPath);
            validation = validation.and(validateDisplayName(recipe.getDisplayName(), "displayName in " + categoryContext));
            validation = validation.and(validateDescription(recipe.getDescription(), "description in " + categoryContext));
        }

        // Recursively validate subcategories
        List<String> newPath = new ArrayList<>(categoryPath);
        if (!marketplace.isRoot()) {
            newPath.add(marketplace.getDisplayName());
        }
        for (RecipeMarketplace category : marketplace.getCategories()) {
            validation = validateRecursive(category, validation, newPath);
        }

        return validation;
    }

    private Validated<RecipeMarketplace> validateDisplayName(String displayName, String context) {
        if (displayName.isEmpty()) {
            return Validated.invalid("displayName", displayName, "Display name is empty [" + context + "]");
        }

        Validated<RecipeMarketplace> validation = Validated.none();

        // Display names should start with uppercase
        if (!Character.isUpperCase(displayName.charAt(0))) {
            validation = validation.and(Validated.invalid(
                    "displayName",
                    displayName,
                    "Display name must start with an uppercase letter: '" + displayName + "' [" + context + "]"
            ));
        }

        // Display names should NOT end with a period
        if (displayName.endsWith(".")) {
            validation = validation.and(Validated.invalid(
                    "displayName",
                    displayName,
                    "Display name must not end with a period: '" + displayName + "' [" + context + "]"
            ));
        }

        return validation;
    }

    private Validated<RecipeMarketplace> validateDescription(String description, String context) {
        if (description.isEmpty()) {
            // Empty descriptions are allowed (they may be optional)
            return Validated.none();
        }

        Validated<RecipeMarketplace> validation = Validated.none();

        // Descriptions should end with a period
        if (!description.endsWith(".")) {
            validation = validation.and(Validated.invalid(
                    "description",
                    description,
                    "Description must end with a period: '" + description + "' [" + context + "]"
            ));
        }

        return validation;
    }
}
