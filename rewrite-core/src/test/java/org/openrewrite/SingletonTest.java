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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Singleton.singleton;
import static org.openrewrite.test.SourceSpecs.text;

class SingletonTest implements RewriteTest {

    /**
     * A simplified recipe that appends content to a text file.
     * Uses Singleton to ensure only one instance with the same content parameter makes changes.
     */
    @EqualsAndHashCode(callSuper = false)
    @Value
    static class SingletonAppend extends Recipe {
        String content;

        @Override
        public String getDisplayName() {
            return "Singleton append";
        }

        @Override
        public String getDescription() {
            return "Appends content to test.txt with Singleton behavior";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return singleton(this, new TreeVisitor<>() {
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
    void onlyFirstRecipeWithSharedSingletonPreconditionMakesChanges() {
        Recipe recipe = new Recipe() {
            @Override
            public String getDisplayName() {
                return "Test Singleton with identical recipes";
            }

            @Override
            public String getDescription() {
                return "SingletonAppend uses Singleton, so when included multiple times only the first makes changes";
            }

            @Override
            public List<Recipe> getRecipeList() {
                // Create two identical SingletonAppend instances
                // Because SingletonAppend uses Singleton and equals() for comparison,
                // only the first will execute
                return List.of(
                  new SingletonAppend("first"),
                  new SingletonAppend("first")
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
                return "Test Singleton with different recipes";
            }

            @Override
            public String getDescription() {
                return "SingletonAppend instances with different arguments are not equal, so both execute";
            }

            @Override
            public List<Recipe> getRecipeList() {
                // Create two SingletonAppend instances with different content
                // Because they have different content, they are not equal,
                // so both will execute
                return List.of(
                  new SingletonAppend("first "),
                  new SingletonAppend("second")
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
    void yamlRecipeWithSingletonPreconditionIncludedMultipleTimes() {
        // This test demonstrates that when a YAML recipe with org.openrewrite.Singleton precondition.
        // is included multiple times the singleton instance is shared only the first gets to make changes.
        rewriteRun(
          spec -> spec
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .recipeFromYaml(
            """
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.ChildWithSingletonPrecondition
              displayName: Child recipe with Singleton precondition
              description: This recipe should only run once when included multiple times.
              preconditions:
                - org.openrewrite.Singleton
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
                - org.openrewrite.ChildWithSingletonPrecondition
                - org.openrewrite.ChildWithSingletonPrecondition
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
    void yamlRecipeWithSingletonPreconditionAcrossDifferentYamlFiles() {
        // This test demonstrates that even when recipes are loaded from different YAML sources,
        // the Singleton precondition ensures only the first instance makes changes.
        // For this to work Environment must ensure that declarative recipes with the same name are the same instance
        String childYaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.ChildWithSingletonPrecondition
          displayName: Child recipe with Singleton precondition
          description: This recipe should only run once when included multiple times.
          preconditions:
            - org.openrewrite.Singleton
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
            - org.openrewrite.ChildWithSingletonPrecondition
            - org.openrewrite.ChildWithSingletonPrecondition
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

    /**
     * A scanning recipe whose scanner increments a static counter for each visited file.
     * Used to prove that when it is used behind a Singleton precondition, the scanner
     * runs once per file for the first recipe instance and is skipped entirely for later
     * equivalent instances.
     */
    @EqualsAndHashCode(callSuper = false)
    @Value
    static class CountingScanRecipe extends ScanningRecipe<AtomicInteger> {
        static final AtomicInteger SCAN_COUNT = new AtomicInteger();

        @Override
        public String getDisplayName() {
            return "Counting scan recipe";
        }

        @Override
        public String getDescription() {
            return "Tracks how many files its scanner visits via a shared static counter.";
        }

        @Override
        public AtomicInteger getInitialValue(ExecutionContext ctx) {
            return SCAN_COUNT;
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(AtomicInteger acc) {
            return new TreeVisitor<>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    SCAN_COUNT.incrementAndGet();
                    return tree;
                }
            };
        }
    }

    @Test
    void singletonPreconditionDeduplicatesAcrossNestingLevels() {
        CountingScanRecipe.SCAN_COUNT.set(0);

        // ChildWithCountingScanner appears twice: once at the top level of ParentRecipe, and once
        // nested inside NestedWrapper. Recursive deduplication should keep the first occurrence
        // and drop the nested one.
        String childYaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.ChildWithCountingScanner
          displayName: Child recipe with counting scanner behind Singleton
          description: Uses Singleton precondition so scanning is skipped for duplicate instances.
          preconditions:
            - org.openrewrite.Singleton
          recipeList:
            - org.openrewrite.SingletonTest$CountingScanRecipe
          """;

        String nestedWrapperYaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.NestedWrapper
          displayName: Wrapper that also pulls in the counting child
          description: Embeds the counting child under a layer of nesting.
          recipeList:
            - org.openrewrite.ChildWithCountingScanner
          """;

        String parentYaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.ParentRecipe
          displayName: Parent recipe that references the counting child at two different depths
          description: Forces the Singleton-gated child to appear at different levels of the tree.
          recipeList:
            - org.openrewrite.ChildWithCountingScanner
            - org.openrewrite.NestedWrapper
          """;

        Recipe recipe = Environment.builder()
          .load(new YamlResourceLoader(
            new ByteArrayInputStream(childYaml.getBytes(StandardCharsets.UTF_8)),
            URI.create("child.yml"),
            new Properties()))
          .load(new YamlResourceLoader(
            new ByteArrayInputStream(nestedWrapperYaml.getBytes(StandardCharsets.UTF_8)),
            URI.create("nested-wrapper.yml"),
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
            .expectedCyclesThatMakeChanges(0)
            .recipe(recipe)
            .validateRecipeSerialization(false)
            .afterRecipe(run -> assertThat(CountingScanRecipe.SCAN_COUNT.get())
              .as("scanner should run once per file exactly once across the whole tree; nested occurrence must be filtered by recursive singleton deduplication")
              .isEqualTo(2)),
          text("one", spec -> spec.path("a.txt")),
          text("two", spec -> spec.path("b.txt"))
        );
    }
}
