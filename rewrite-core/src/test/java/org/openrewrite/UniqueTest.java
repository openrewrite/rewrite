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
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.openrewrite.Unique.unique;
import static org.openrewrite.test.SourceSpecs.text;

class UniqueTest implements RewriteTest {

    /**
     * A simplified recipe that appends content to a text file.
     * Uses Unique to ensure only one instance with the same content parameter makes changes.
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    static class UniqueAppend extends Recipe {
        String content;

        @Override
        public String getDisplayName() {
            return "Unique append";
        }

        @Override
        public String getDescription() {
            return "Appends content to test.txt with Unique behavior";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return unique(this, new TreeVisitor<>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if (tree instanceof PlainText plainText) {
                        return plainText.withText(plainText.getText() + content);
                    }
                    return tree;
                }
            });
        }
    }

    @Test
    void onlyFirstRecipeWithSharedUniquePreconditionMakesChanges() {
        Recipe recipe = new Recipe() {
            @Override
            public String getDisplayName() {
                return "Test Unique with identical recipes";
            }

            @Override
            public String getDescription() {
                return "UniqueAppend uses Unique, so when included multiple times only the first makes changes";
            }

            @Override
            public List<Recipe> getRecipeList() {
                // Create two identical UniqueAppend instances
                // Because UniqueAppend uses Unique and equals() for comparison,
                // only the first will execute
                return List.of(
                  new UniqueAppend("first"),
                  new UniqueAppend("first")
                );
            }
        };

        rewriteRun(
          spec -> spec
            .recipe(recipe)
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .validateRecipeSerialization(false),
          text("", "first")
        );
    }

    @Test
    void differentRecipeInstancesCanBothMakeChanges() {
        Recipe recipe = new Recipe() {
            @Override
            public String getDisplayName() {
                return "Test Unique with different recipes";
            }

            @Override
            public String getDescription() {
                return "UniqueAppend instances with different arguments are not equal, so both execute";
            }

            @Override
            public List<Recipe> getRecipeList() {
                // Create two UniqueAppend instances with different content
                // Because they have different content, they are not equal,
                // so both will execute
                return List.of(
                  new UniqueAppend("first "),
                  new UniqueAppend("second")
                );
            }
        };

        rewriteRun(
          spec -> spec
            .recipe(recipe)
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .validateRecipeSerialization(false),
          text("", "first second")
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
        // For this to work Environment must ensure that declarative recipes with the same name are the same instance
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
