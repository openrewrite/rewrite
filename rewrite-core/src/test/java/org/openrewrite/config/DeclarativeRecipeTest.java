/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.ChangeText;
import org.openrewrite.text.Find;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class DeclarativeRecipeTest implements RewriteTest {

    @DocumentExample
    @Test
    void precondition() {
        rewriteRun(
          spec -> {
              spec.validateRecipeSerialization(false);
              DeclarativeRecipe dr = new DeclarativeRecipe("test", "test", "test", emptySet(),
                null, URI.create("null"), true, emptyList());
              dr.addPrecondition(
                toRecipe(() -> new PlainTextVisitor<>() {
                    @Override
                    public PlainText visitText(PlainText text, ExecutionContext ctx) {
                        if ("1".equals(text.getText())) {
                            return SearchResult.found(text);
                        }
                        return text;
                    }
                })
              );
              dr.addUninitialized(
                new ChangeText("2")
              );
              dr.addUninitialized(
                new ChangeText("3")
              );
              dr.initialize(List.of());
              spec.recipe(dr);
          },
          text("1", "3"),
          text("2")
        );
    }

    @Test
    void addingPreconditionsWithOptions() {
        DeclarativeRecipe dr = new DeclarativeRecipe("test", "test", "test", emptySet(),
          null, URI.create("dummy"), true, emptyList());
        dr.addPrecondition(
          toRecipe(() -> new PlainTextVisitor<>() {
              @Override
              public PlainText visitText(PlainText text, ExecutionContext ctx) {
                  if ("1".equals(text.getText())) {
                      return SearchResult.found(text);
                  }
                  return text;
              }
          })
        );
        dr.addUninitialized(
          new ChangeText("2")
        );
        dr.addUninitialized(
          new ChangeText("3")
        );
        dr.initialize(List.of());
        assertThat(dr.getDescriptor().getRecipeList())
          .hasSize(3) // precondition + 2 recipes with options
          .flatExtracting(RecipeDescriptor::getOptions)
          .hasSize(2)
          .extracting(OptionDescriptor::getName)
          .containsOnly("toText");
    }

    @Test
    void uninitializedFailsValidation() {
        DeclarativeRecipe dr = new DeclarativeRecipe("test", "test", "test", emptySet(),
          null, URI.create("dummy"), true, emptyList());
        dr.addUninitializedPrecondition(
          toRecipe(() -> new PlainTextVisitor<>() {
              @Override
              public PlainText visitText(PlainText text, ExecutionContext ctx) {
                  if ("1".equals(text.getText())) {
                      return SearchResult.found(text);
                  }
                  return text;
              }
          })
        );
        dr.addUninitialized(
          new ChangeText("2")
        );
        dr.addUninitialized(
          new ChangeText("3")
        );
        Validated<Object> validation = dr.validate();
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.failures().size()).isEqualTo(2);
        assertThat(validation.failures().getFirst().getProperty()).isEqualTo("initialization");
    }

    @Test
    void uninitializedWithInitializedRecipesPassesValidation() {
        DeclarativeRecipe dr = new DeclarativeRecipe("test", "test", "test", emptySet(),
          null, URI.create("dummy"), true, emptyList());
        dr.setPreconditions(
          List.of(
            toRecipe(() -> new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    if ("1".equals(text.getText())) {
                        return SearchResult.found(text);
                    }
                    return text;
                }
            }))
        );
        dr.setRecipeList(List.of(
          new ChangeText("2"),
          new ChangeText("3")
        ));
        Validated<Object> validation = dr.validate();
        assertThat(validation.isValid()).isTrue();
    }

    @Test
    void yamlPrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.PreconditionTest
            description: Test.
            preconditions:
              - org.openrewrite.text.Find:
                  find: 1
            recipeList:
              - org.openrewrite.text.ChangeText:
                 toText: 2
              - org.openrewrite.text.ChangeText:
                 toText: 3
            """, "org.openrewrite.PreconditionTest"),
          text("1", "3"),
          text("2")
        );
    }

    @Test
    void yamlDeclarativeRecipeAsPrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.PreconditionTest
              description: Test.
              preconditions:
                - org.openrewrite.DeclarativePrecondition
              recipeList:
                - org.openrewrite.text.ChangeText:
                   toText: 3
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.DeclarativePrecondition
              recipeList:
                - org.openrewrite.text.Find:
                    find: 1
              """,
            "org.openrewrite.PreconditionTest"
          ),
          text("1", "3"),
          text("2")
        );
    }

    @Test
    void orPreconditions() {
        // As documented https://docs.openrewrite.org/reference/yaml-format-reference#creating-or-preconditions-instead-of-and
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.DoSomething
              description: Test.
              preconditions:
                - org.sample.FindAnyJson
              recipeList:
                - org.openrewrite.text.ChangeText:
                   toText: 2
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.FindAnyJson
              recipeList:
                - org.openrewrite.FindSourceFiles:
                    filePattern: "**/my.json"
                - org.openrewrite.FindSourceFiles:
                    filePattern: "**/your.json"
                - org.openrewrite.FindSourceFiles:
                    filePattern: "**/our.json"
              """,
            "org.sample.DoSomething"
          ),
          text("1", "2", spec -> spec.path("a/my.json")),
          text("a", spec -> spec.path("a/not-my.json"))
        );
    }

    @Test
    void yamlPreconditionWithScanningRecipe() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.PreconditionTest
              description: Test.
              preconditions:
                - org.openrewrite.text.Find:
                    find: 1
              recipeList:
                - org.openrewrite.text.CreateTextFile:
                   relativeFileName: test.txt
                   fileContents: "test"
              """, "org.openrewrite.PreconditionTest")
            .afterRecipe(run -> assertThat(run.getChangeset().getAllResults()).anySatisfy(
              s -> {
                  //noinspection DataFlowIssue
                  assertThat(s.getAfter()).isNotNull();
                  assertThat(s.getAfter().getSourcePath()).isEqualTo(Path.of("test.txt"));
              }
            ))
            .expectedCyclesThatMakeChanges(1),
          text("1")
        );
    }

    @Test
    void scanningPreconditionMet() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.ScanningPreconditionTest
            description: Test.
            preconditions:
              - org.openrewrite.search.RepositoryContainsFile:
                  filePattern: sam.txt
            recipeList:
              - org.openrewrite.text.FindAndReplace:
                  find: foo
                  replace: bar
            """, "org.openrewrite.ScanningPreconditionTest"),
          text("sam", spec -> spec.path("sam.txt")),
          text("foo", "bar")
        );
    }

    @Test
    void scanningPreconditionNotMet() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.ScanningPreconditionTest
            description: Test.
            preconditions:
              - org.openrewrite.search.RepositoryContainsFile:
                  filePattern: sam.txt
            recipeList:
              - org.openrewrite.text.FindAndReplace:
                  find: foo
                  replace: bar
            """, "org.openrewrite.ScanningPreconditionTest"),
          text("foo")
        );
    }

    @Test
    void preconditionOnNestedDeclarative() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.PreconditionOnDeclarative
            description: Test.
            preconditions:
              - org.openrewrite.text.Find:
                  find: foo
            recipeList:
              - org.openrewrite.DeclarativeExample
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.DeclarativeExample
            description: Test.
            recipeList:
              - org.openrewrite.text.FindAndReplace:
                  find: foo
                  replace: bar
            """, "org.openrewrite.PreconditionOnDeclarative"),
          text("foo", "bar")
        );
    }

    @Test
    void exposesUnderlyingDataTables() {
        DeclarativeRecipe dr = new DeclarativeRecipe("org.openrewrite.DeclarativeDataTable", "declarative with data table",
          "test", emptySet(), null, URI.create("dummy"), true, emptyList());
        dr.addUninitialized(new Find("sam", null, null, null, null, null, null, null));
        dr.initialize(List.of());
        assertThat(dr.getDataTableDescriptors()).anyMatch(it -> "org.openrewrite.table.TextMatches".equals(it.getName()));
    }

    @Test
    void maxCycles() {
        rewriteRun(
          spec -> spec.recipe(new RepeatedFindAndReplace(".+", "$0+1", 1)),
          text("1", "1+1")
        );
        rewriteRun(
          spec -> spec.recipe(new RepeatedFindAndReplace(".+", "$0+1", 2)).expectedCyclesThatMakeChanges(2),
          text("1", "1+1+1")
        );
    }

    @Test
    void maxCyclesNested() {
        AtomicInteger cycleCount = new AtomicInteger();
        Recipe root = new MaxCycles(
          100,
          List.of(new MaxCycles(
              2,
              List.of(new RepeatedFindAndReplace(".+", "$0+1", 100))
            ),
            toRecipe(() -> new TreeVisitor<>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    cycleCount.incrementAndGet();
                    return tree;
                }
            })
          )
        );
        rewriteRun(
          spec -> spec.recipe(root).cycles(10).cycles(3).expectedCyclesThatMakeChanges(2),
          text("1", "1+1+1")
        );
        assertThat(cycleCount).hasValue(3);
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    static class MaxCycles extends Recipe {
        int maxCycles;
        List<Recipe> recipeList;

        @Override
        public int maxCycles() {
            return maxCycles;
        }

        @Override
        public String getDisplayName() {
            return "Executes recipes multiple times";
        }

        @Override
        public String getDescription() {
            return "Executes recipes multiple times.";
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    static class RepeatedFindAndReplace extends Recipe {
        String find;
        String replace;
        int maxCycles;

        @Override
        public int maxCycles() {
            return maxCycles;
        }

        @Override
        public String getDisplayName() {
            return "Repeated find and replace";
        }

        @Override
        public String getDescription() {
            return "Find and replace repeatedly.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new TreeVisitor<>() {
                @Override
                public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    PlainText text = ((PlainText) tree);
                    assert text != null;
                    return text.withText(text.getText().replaceAll(find, replace));
                }
            };
        }
    }

    @Test
    void selfReferencingRecipeDetectedAsCycle() {
        // Test that a recipe referencing itself is detected as a cycle
        DeclarativeRecipe selfReferencing = new DeclarativeRecipe(
            "org.openrewrite.SelfReferencing",
            "Self Referencing Recipe",
            "A recipe that references itself",
            emptySet(),
            null,
            URI.create("test"),
            false,
            emptyList()
        );

        // Add itself as a sub-recipe
        selfReferencing.addUninitialized("org.openrewrite.SelfReferencing");

        // Initialize should throw RecipeIntrospectionException when cycle is detected
        assertThatThrownBy(() -> selfReferencing.initialize(List.of(selfReferencing)))
            .isInstanceOf(RecipeIntrospectionException.class)
            .hasMessageContaining("creates a cycle")
            .hasMessageContaining("org.openrewrite.SelfReferencing -> org.openrewrite.SelfReferencing");
    }

    @Test
    void mutuallyRecursiveRecipesDetectedAsCycle() {
        // Test that mutually recursive recipes are detected as a cycle
        DeclarativeRecipe recipeA = new DeclarativeRecipe(
            "org.openrewrite.RecipeA",
            "Recipe A",
            "Recipe A that references Recipe B",
            emptySet(),
            null,
            URI.create("test"),
            false,
            emptyList()
        );

        DeclarativeRecipe recipeB = new DeclarativeRecipe(
            "org.openrewrite.RecipeB",
            "Recipe B",
            "Recipe B that references Recipe A",
            emptySet(),
            null,
            URI.create("test"),
            false,
            emptyList()
        );

        // A references B
        recipeA.addUninitialized("org.openrewrite.RecipeB");

        // B references A
        recipeB.addUninitialized("org.openrewrite.RecipeA");

        // Initialize should throw RecipeIntrospectionException when cycle is detected
        assertThatThrownBy(() -> recipeA.initialize(List.of(recipeA, recipeB)))
            .isInstanceOf(RecipeIntrospectionException.class)
            .hasMessageContaining("creates a cycle")
            .hasMessageContaining("RecipeA")
            .hasMessageContaining("RecipeB");
    }

    @Test
    void deeperCyclicReferencesDetectedAsCycle() {
        // Test that deeper cyclic references (A -> B -> C -> A) are detected as a cycle
        DeclarativeRecipe recipeA = new DeclarativeRecipe(
            "org.openrewrite.RecipeA",
            "Recipe A",
            "Recipe A that references Recipe B",
            emptySet(),
            null,
            URI.create("test"),
            false,
            emptyList()
        );

        DeclarativeRecipe recipeB = new DeclarativeRecipe(
            "org.openrewrite.RecipeB",
            "Recipe B",
            "Recipe B that references Recipe C",
            emptySet(),
            null,
            URI.create("test"),
            false,
            emptyList()
        );

        DeclarativeRecipe recipeC = new DeclarativeRecipe(
            "org.openrewrite.RecipeC",
            "Recipe C",
            "Recipe C that references Recipe A",
            emptySet(),
            null,
            URI.create("test"),
            false,
            emptyList()
        );

        // A references B
        recipeA.addUninitialized("org.openrewrite.RecipeB");

        // B references C
        recipeB.addUninitialized("org.openrewrite.RecipeC");

        // C references A (completing the cycle)
        recipeC.addUninitialized("org.openrewrite.RecipeA");

        // Initialize should throw RecipeIntrospectionException when cycle is detected
        assertThatThrownBy(() -> recipeA.initialize(List.of(recipeA, recipeB, recipeC)))
            .isInstanceOf(RecipeIntrospectionException.class)
            .hasMessageContaining("creates a cycle")
            // The cycle path should show A -> B -> C -> A
            .hasMessageContaining("RecipeA")
            .hasMessageContaining("RecipeB")
            .hasMessageContaining("RecipeC");
    }
}
