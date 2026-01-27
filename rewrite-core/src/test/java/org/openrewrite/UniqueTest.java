/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.ChangeText;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.openrewrite.test.SourceSpecs.text;

class UniqueTest implements RewriteTest {

    @Test
    void onlyFirstRecipeWithSharedUniquePreconditionMakesChanges() {
        Recipe recipe = new Recipe() {
            @Override
            public String getDisplayName() {
                return "First";
            }

            @Override
            public String getDescription() {
                return "If Unique works correctly the result of this recipe should be a text file that says \"first\"";
            }

            @Override
            public List<Recipe> getRecipeList() {
                Unique unique = new Unique();
                return List.of(
                  new Unique.UniqueDecoratedRecipe(new ChangeText("first"), unique),
                  new Unique.UniqueDecoratedRecipe(new ChangeText("second"), unique)
                );
            }
        };

        rewriteRun(
          spec -> spec.recipe(recipe).validateRecipeSerialization(false),
          text(
            """
              """,
            """
              first
              """
          )
        );
    }

    @Test
    void onlyFirstRecipeWithUniqueDecoratesMakesChanges() {
        // Using Unique.decorate() on the same recipe instance returns a wrapper that shares a Unique instance
        ChangeText changeText = new ChangeText("decorated");
        Recipe recipe = new Recipe() {
            @Override
            public String getDisplayName() {
                return "Test Unique.decorate()";
            }

            @Override
            public String getDescription() {
                return "If Unique.decorate() works correctly, only the first decorated instance makes changes";
            }

            @Override
            public List<Recipe> getRecipeList() {
                return List.of(
                  Unique.decorate(changeText),
                  Unique.decorate(changeText)
                );
            }
        };

        rewriteRun(
          spec -> spec.recipe(recipe).validateRecipeSerialization(false),
          text(
            """
              """,
            """
              decorated
              """
          )
        );
    }

    @Test
    void yamlRecipeWithUniquePreconditionIncludedMultipleTimes() {
        // This test demonstrates that when a YAML recipe with org.openrewrite.Unique precondition.
        // is included multiple times the unique instance is shared only the first gets to make changes.
        rewriteRun(
          spec -> spec
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .recipeFromYaml(
            """
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.ChildWithUniquePrecondition
              displayName: Child recipe with Unique precondition
              description: This recipe should only run once when included multiple times.
              preconditions:
                - org.openrewrite.Unique
              recipeList:
                - org.openrewrite.text.AppendToTextFile:
                    relativeFileName: test.txt
                    content: once
                    existingFileStrategy: Continue
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.ParentRecipe
              displayName: Parent recipe that includes child multiple times
              description: Includes the same child recipe twice to test that only the first instance makes changes.
              recipeList:
                - org.openrewrite.ChildWithUniquePrecondition
                - org.openrewrite.ChildWithUniquePrecondition
              """,
            "org.openrewrite.ParentRecipe"
          ),
          text(
            "",
            """

              once
              """,
            spec -> spec.path("test.txt").noTrim()
          )
        );
    }

    @Test
    void yamlRecipeWithUniquePreconditionAcrossDifferentYamlFiles() {
        // This test demonstrates that even when recipes are loaded from different YAML sources,
        // the Unique precondition ensures only the first instance makes changes.
        String childYaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.ChildWithUniquePrecondition
          displayName: Child recipe with Unique precondition
          description: This recipe should only run once when included multiple times.
          preconditions:
            - org.openrewrite.Unique
          recipeList:
            - org.openrewrite.text.AppendToTextFile:
                relativeFileName: test.txt
                content: once
                existingFileStrategy: Continue
          """;

        String parentYaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.ParentRecipe
          displayName: Parent recipe that includes child multiple times
          description: Includes the same child recipe twice to test that only the first instance makes changes.
          recipeList:
            - org.openrewrite.ChildWithUniquePrecondition
            - org.openrewrite.ChildWithUniquePrecondition
          """;

        Recipe recipe = Environment.builder()
          .load(new YamlResourceLoader(
            new ByteArrayInputStream(childYaml.getBytes(StandardCharsets.UTF_8)),
            URI.create("child.yml"),
            new Properties()))
          .load(new YamlResourceLoader(
            new ByteArrayInputStream(parentYaml.getBytes(StandardCharsets.UTF_8)),
            URI.create("parent.yml"),
            new Properties()))
          .build()
          .activateRecipes("org.openrewrite.ParentRecipe");

        rewriteRun(
          spec -> spec
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .recipe(recipe),
          text(
            "",
            """

              once
              """,
            spec -> spec.path("test.txt").noTrim()
          )
        );
    }
}