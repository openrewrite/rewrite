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
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates that a recipe marketplace CSV is in sync with the actual recipes
 * available in a recipe JAR. Ensures that:
 * <ul>
 *     <li>Every recipe listed in the CSV exists in the JAR</li>
 *     <li>Every recipe in the JAR has at least one entry in the CSV</li>
 * </ul>
 */
public class RecipeMarketplaceCompletenessValidator {

    /**
     * Validate that the CSV marketplace is complete and in sync with the JAR environment.
     *
     * @param csv The recipe marketplace loaded from CSV.
     * @param jar The environment loaded from the recipe JAR.
     * @return A validation result containing all errors found.
     */
    public Validated<RecipeMarketplace> validate(RecipeMarketplace csv, Environment jar) {
        Validated<RecipeMarketplace> validation = Validated.none();

        // Get all recipe names from CSV (distinct by name)
        Set<String> csvRecipeNames = new HashSet<>();
        for (RecipeListing recipe : csv.getAllRecipes()) {
            csvRecipeNames.add(recipe.getName());
        }

        // Get all recipe names from JAR
        Set<String> jarRecipeNames = new HashSet<>();
        for (RecipeDescriptor descriptor : jar.listRecipeDescriptors()) {
            jarRecipeNames.add(descriptor.getName());
        }

        // Find recipes in CSV that don't exist in JAR (phantom recipes)
        for (String csvRecipeName : csvRecipeNames) {
            if (!jarRecipeNames.contains(csvRecipeName)) {
                validation = validation.and(Validated.invalid(
                        "recipe",
                        csvRecipeName,
                        "Recipe listed in CSV does not exist in JAR: '" + csvRecipeName + "' [phantom recipe]"
                ));
            }
        }

        // Find recipes in JAR that aren't in CSV (missing recipes)
        for (String jarRecipeName : jarRecipeNames) {
            if (!csvRecipeNames.contains(jarRecipeName)) {
                validation = validation.and(Validated.invalid(
                        "recipe",
                        jarRecipeName,
                        "Recipe exists in JAR but is not listed in CSV: '" + jarRecipeName + "' [missing from CSV]"
                ));
            }
        }

        return validation;
    }
}
