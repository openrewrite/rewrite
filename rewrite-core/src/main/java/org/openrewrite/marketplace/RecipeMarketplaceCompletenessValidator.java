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
 * Validates that a recipe marketplace CSV is in sync with the actual recipes in an environment. Ensures that:
 * <ul>
 *     <li>Every recipe listed in the CSV exists in the environment</li>
 *     <li>Every recipe in the environment has at least one entry in the CSV</li>
 * </ul>
 */
public class RecipeMarketplaceCompletenessValidator {

    /**
     * Validate that the CSV marketplace is complete and in sync with the JAR environment.
     *
     * @param csv The recipe marketplace loaded from CSV.
     * @param env The environment to validate against.
     * @return A validation result containing all errors found.
     */
    public Validated<RecipeMarketplace> validate(RecipeMarketplace csv, Environment env) {
        Validated<RecipeMarketplace> validation = Validated.none();

        Set<String> csvRecipeNames = new HashSet<>();
        for (RecipeListing recipe : csv.getAllRecipes()) {
            csvRecipeNames.add(recipe.getName());
        }

        // Get all recipe names from JAR
        Set<String> jarRecipeNames = new HashSet<>();
        for (RecipeDescriptor descriptor : env.listRecipeDescriptors()) {
            jarRecipeNames.add(descriptor.getName());
        }

        // Find recipes in CSV that don't exist in the environment (phantom recipes)
        for (String csvRecipeName : csvRecipeNames) {
            if (!jarRecipeNames.contains(csvRecipeName)) {
                validation = validation.and(Validated.invalid(csvRecipeName, csvRecipeName,
                        "Recipe listed in CSV must exist in the environment; " +
                                "remove this entry from `recipes.csv` or add the recipe to the environment."));
            }
        }

        // Find recipes in the environment that aren't in CSV (missing recipes)
        for (String envRecipeName : jarRecipeNames) {
            if (!csvRecipeNames.contains(envRecipeName)) {
                validation = validation.and(Validated.invalid(envRecipeName, envRecipeName,
                        "Recipe exists in environment but is not listed in CSV; " +
                                "run `./gradlew recipeCsvGenerate` to update `recipes.csv`."));
            }
        }

        return validation;
    }
}
