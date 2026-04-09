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

import static org.assertj.core.api.Assertions.assertThat;

class RecipeMarketplaceCompletenessValidatorTest {

    private final RecipeMarketplaceCompletenessValidator validator = new RecipeMarketplaceCompletenessValidator();

    private Environment envWithRecipe(String recipeName) {
        //language=yml
        String yaml = """
                type: specs.openrewrite.org/v1beta/recipe
                name: %s
                displayName: Test recipe
                recipeList:
                    - org.openrewrite.text.ChangeText:
                        toText: Hello
                """.formatted(recipeName);
        return Environment.builder()
                .load(new YamlResourceLoader(
                        new ByteArrayInputStream(yaml.getBytes()),
                        URI.create("rewrite.yml"),
                        new Properties()))
                .build();
    }

    @Test
    void crossPackageRecipesSkippedInPhantomCheck() {
        // CSV contains a recipe from a different package than the project
        RecipeMarketplace csv = new RecipeMarketplaceReader().fromCsv("""
                ecosystem,packageName,name,displayName,description
                maven,org.example:my-recipes,org.example.MyRecipe,My recipe,A recipe from this project.
                maven,org.openrewrite:rewrite-java,org.openrewrite.java.SomeRecipe,Some recipe,A recipe from a dependency.
                """);

        // Environment only contains the project's own recipe (not the dependency's)
        Environment env = envWithRecipe("org.example.MyRecipe");

        // Without projectPackageName, the cross-package recipe is flagged as phantom
        Validated<RecipeMarketplace> withoutPackage = validator.validate(csv, env);
        assertThat(withoutPackage.isInvalid()).isTrue();
        assertThat(withoutPackage.failures())
                .anyMatch(f -> f.getProperty().equals("org.openrewrite.java.SomeRecipe"));

        // With projectPackageName, only the project's own recipes are checked
        Validated<RecipeMarketplace> withPackage = validator.validate(csv, env, "org.example:my-recipes");
        assertThat(withPackage.isValid()).isTrue();
    }

    @Test
    void missingOwnPackageRecipeStillDetected() {
        // CSV only has one recipe but environment has two from the same package
        RecipeMarketplace csv = new RecipeMarketplaceReader().fromCsv("""
                ecosystem,packageName,name,displayName,description
                maven,org.example:my-recipes,org.example.RecipeA,Recipe A,First recipe.
                """);

        //language=yml
        String yaml = """
                type: specs.openrewrite.org/v1beta/recipe
                name: org.example.RecipeA
                displayName: Recipe A
                recipeList:
                    - org.openrewrite.text.ChangeText:
                        toText: Hello
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: org.example.RecipeB
                displayName: Recipe B
                recipeList:
                    - org.openrewrite.text.ChangeText:
                        toText: World
                """;
        Environment env = Environment.builder()
                .load(new YamlResourceLoader(
                        new ByteArrayInputStream(yaml.getBytes()),
                        URI.create("rewrite.yml"),
                        new Properties()))
                .build();

        Validated<RecipeMarketplace> validation = validator.validate(csv, env, "org.example:my-recipes");
        assertThat(validation.isInvalid()).isTrue();
        assertThat(validation.failures())
                .anyMatch(f -> f.getProperty().equals("org.example.RecipeB")
                        && f.getMessage().contains("Recipe exists in environment but is not listed in CSV"));
    }

    @Test
    void phantomRecipeFromOwnPackageStillDetected() {
        // CSV has a recipe that doesn't exist in the JAR, from the project's own package
        RecipeMarketplace csv = new RecipeMarketplaceReader().fromCsv("""
                ecosystem,packageName,name,displayName,description
                maven,org.example:my-recipes,org.example.RealRecipe,Real recipe,Exists in JAR.
                maven,org.example:my-recipes,org.example.PhantomRecipe,Phantom recipe,Does not exist.
                """);

        Environment env = envWithRecipe("org.example.RealRecipe");

        Validated<RecipeMarketplace> validation = validator.validate(csv, env, "org.example:my-recipes");
        assertThat(validation.isInvalid()).isTrue();
        assertThat(validation.failures())
                .anyMatch(f -> f.getProperty().equals("org.example.PhantomRecipe")
                        && f.getMessage().contains("Recipe listed in CSV must exist in the environment"));
    }
}
