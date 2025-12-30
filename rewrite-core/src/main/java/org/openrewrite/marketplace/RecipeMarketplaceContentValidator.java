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

import static java.util.stream.Collectors.joining;

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
        return validate(marketplace.getRoot(), validation, categoryPath);
    }

    private Validated<RecipeMarketplace> validate(RecipeMarketplace.Category category,
                                                  Validated<RecipeMarketplace> validation,
                                                  List<String> categoryPath) {
        for (RecipeListing recipe : category.getRecipes()) {
            String value = recipe.getName() + (categoryPath.isEmpty() ? "" : categoryPath.stream()
                    .collect(joining(" > ", "[", "]")));
            validation = validation.and(validateDisplayName(recipe.getDisplayName(), value));
            validation = validation.and(validateDescription(recipe.getDescription(), value));
        }

        for (RecipeMarketplace.Category child : category.getCategories()) {
            List<String> nextCategoryPath = new ArrayList<>(categoryPath);
            nextCategoryPath.add(category.getDisplayName());
            validation = validate(child, validation, nextCategoryPath);
        }

        return validation;
    }

    private Validated<RecipeMarketplace> validateDisplayName(String displayName, String recipe) {
        Validated<RecipeMarketplace> validation = Validated.none();
        String property = recipe + ".displayName";
        if (displayName.isEmpty()) {
            validation = validation.and(Validated.invalid(property, displayName, "Display must not be empty"));
        }
        if (doesNotStartWithUppercase(displayName)) {
            validation = validation.and(Validated.invalid(property, displayName, "Display name must be sentence cased"));
        }
        if (displayName.endsWith(".")) {
            validation = validation.and(Validated.invalid(property, displayName, "Display name must not end with a period"));
        }
        return validation;
    }

    private Validated<RecipeMarketplace> validateDescription(String description, String recipe) {
        if (description.isEmpty()) {
            return Validated.none();
        }
        Validated<RecipeMarketplace> validation = Validated.none();
        String property = recipe + ".description";
        if (doesNotStartWithUppercase(description)) {
            validation = validation.and(Validated.invalid(property, description, "Description must be sentence cased"));
        }
        if (!description.endsWith(".")) {
            validation = validation.and(Validated.invalid(property, description, "Description must end with a period."));
        }

        return validation;
    }

    private static boolean doesNotStartWithUppercase(String text) {
        char firstChar = text.charAt(0);
        if (Character.isUpperCase(firstChar)) {
            return false;
        }
        return Character.isLetter(firstChar) || !Character.isUpperCase(text.charAt(1));
        // Allow backticks, links, etc. at the start of the text, provided the next character is uppercase
    }
}
