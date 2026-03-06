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

@SuppressWarnings("NullableProblems")
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
          .hasSize(2)
          .flatExtracting(RecipeDescriptor::getOptions)
          .hasSize(2)
          .extracting(OptionDescriptor::getName)
          .containsOnly("toText");
    }

    @Test
    void preconditionDescriptorsIncludedInDescriptor() {
        DeclarativeRecipe dr = new DeclarativeRecipe("test", "test", "test", emptySet(),
          null, URI.create("dummy"), true, emptyList());
        dr.addPrecondition(new Find("precondition-marker", null, null, null, null, null, null, null));
        dr.addUninitialized(new ChangeText("2"));
        dr.initialize(List.of());

        RecipeDescriptor descriptor = dr.getDescriptor();
        assertThat(descriptor.getPreconditions())
          .hasSize(1)
          .first()
          .satisfies(p -> assertThat(p.getName()).isEqualTo("org.openrewrite.text.Find"));
        assertThat(descriptor.getRecipeList())
          .hasSize(1)
          .first()
          .satisfies(r -> assertThat(r.getName()).isEqualTo("org.openrewrite.text.ChangeText"));
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

        String displayName = "Executes recipes multiple times";

        String description = "Executes recipes multiple times.";
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

        String displayName = "Repeated find and replace";

        String description = "Find and replace repeatedly.";

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new TreeVisitor<>() {
                @Override
                public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    var text = ((PlainText) tree);
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

    @Issue("https://github.com/openrewrite/rewrite/issues/6698")
    @Test
    void nestedScanningRecipeInOrPrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.AddJacksonAnnotations
              description: Test.
              preconditions:
                - org.sample.ProjectUsesJackson
              recipeList:
                - org.openrewrite.text.ChangeText:
                   toText: changed
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.ProjectUsesJackson
              description: OR precondition - matches if ANY condition is true
              recipeList:
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/jackson-config.json"
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/jackson.properties"
              """,
            "org.sample.AddJacksonAnnotations"
          ),
          // jackson-config.json triggers the precondition, so all files get changed
          text("config", "changed", spec -> spec.path("jackson-config.json")),
          text("original", "changed")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6698")
    @Test
    void nestedScanningRecipeInOrPreconditionNotMet() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.AddJacksonAnnotations
              description: Test.
              preconditions:
                - org.sample.ProjectUsesJackson
              recipeList:
                - org.openrewrite.text.ChangeText:
                   toText: changed
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.ProjectUsesJackson
              description: OR precondition - matches if ANY condition is true
              recipeList:
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/jackson-config.json"
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/jackson.properties"
              """,
            "org.sample.AddJacksonAnnotations"
          ),
          text("config", spec -> spec.path("some-other-file.txt")),
          text("original")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6698")
    @Test
    void sameScanningRecipeWithDifferentParametersInOrPrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.MultiFileCheck
              description: Test.
              preconditions:
                - org.sample.HasAnyConfigFile
              recipeList:
                - org.openrewrite.text.ChangeText:
                   toText: changed
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.HasAnyConfigFile
              description: OR precondition with same recipe type but different params
              recipeList:
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/config-a.json"
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/config-b.json"
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/config-c.json"
              """,
            "org.sample.MultiFileCheck"
          ),
          // Only config-b.json exists, so should still match due to OR logic
          // When matched, all files are changed
          text("config b", "changed", spec -> spec.path("config-b.json")),
          text("original", "changed")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6698")
    @Test
    void deeplyNestedOrPreconditionsWithScanningRecipe() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.TopLevel
              description: Test.
              preconditions:
                - org.sample.MiddleLevel
              recipeList:
                - org.openrewrite.text.ChangeText:
                   toText: changed
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.MiddleLevel
              description: Middle level precondition
              recipeList:
                - org.sample.BottomLevel
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/middle.json"
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.BottomLevel
              description: Bottom level with scanning recipes
              recipeList:
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/bottom-a.json"
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/bottom-b.json"
              """,
            "org.sample.TopLevel"
          ),
          // Only bottom-b.json exists, which should match through the nested OR chain
          // When matched, all files are changed
          text("bottom b config", "changed", spec -> spec.path("bottom-b.json")),
          text("original", "changed")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6698")
    @Test
    void multipleNestedOrPreconditionsWithSameScanningRecipe() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.TopLevel
              description: Test.
              preconditions:
                - org.sample.BranchA
                - org.sample.BranchB
              recipeList:
                - org.openrewrite.text.ChangeText:
                   toText: changed
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.BranchA
              description: First branch
              recipeList:
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/branch-a-1.json"
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/shared.json"
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.sample.BranchB
              description: Second branch
              recipeList:
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/branch-b-1.json"
                - org.openrewrite.search.RepositoryContainsFile:
                    filePattern: "**/shared.json"
              """,
            "org.sample.TopLevel"
          ),
          // shared.json exists, which should satisfy both branches (AND of two ORs)
          // When matched, all files are changed
          text("shared config", "changed", spec -> spec.path("shared.json")),
          text("original", "changed")
        );
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

    @Test
    void programmaticInlinePrecondition() {
        rewriteRun(
          spec -> {
              spec.validateRecipeSerialization(false);
              // Create a nested DeclarativeRecipe with inline precondition
              DeclarativeRecipe nested = new DeclarativeRecipe("nested", "nested", "nested", emptySet(),
                null, URI.create("test"), false, emptyList());
              nested.addPrecondition(new FindSourceFiles("**/target.txt"));
              nested.addUninitialized(new ChangeText("changed"));
              nested.initialize(List.of());

              // Verify preconditions were initialized
              assertThat(nested.getPreconditions()).hasSize(1);

              // Create parent DeclarativeRecipe that contains the nested one
              DeclarativeRecipe dr = new DeclarativeRecipe("test", "test", "test", emptySet(),
                null, URI.create("null"), false, emptyList());
              dr.addUninitialized(nested);
              dr.initialize(List.of());
              spec.recipe(dr);
          },
          text("original", "changed", spec -> spec.path("target.txt")),
          text("original", spec -> spec.path("other.txt"))
        );
    }

    @Test
    void yamlInlinePrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.InlinePreconditionTest
            description: Test inline preconditions.
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: changed
                  preconditions:
                    - org.openrewrite.FindSourceFiles:
                        filePattern: "**/target.txt"
            """, "org.openrewrite.InlinePreconditionTest"),
          text("original", "changed", spec -> spec.path("target.txt")),
          text("original", spec -> spec.path("other.txt"))
        );
    }

    @Test
    void yamlMultipleInlinePreconditions() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.MultipleInlinePreconditionsTest
            description: Test multiple recipes with different inline preconditions.
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: xml-changed
                  preconditions:
                    - org.openrewrite.FindSourceFiles:
                        filePattern: "**/*.xml"
              - org.openrewrite.text.ChangeText:
                  toText: json-changed
                  preconditions:
                    - org.openrewrite.FindSourceFiles:
                        filePattern: "**/*.json"
            """, "org.openrewrite.MultipleInlinePreconditionsTest"),
          text("original", "xml-changed", spec -> spec.path("config.xml")),
          text("original", "json-changed", spec -> spec.path("data.json")),
          text("original", spec -> spec.path("readme.txt"))
        );
    }

    @Test
    void yamlTopLevelPreconditionWithWrapperRecipe() {
        // This tests that top-level preconditions work correctly
        // when there are no inline preconditions on the recipe.
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.TopLevelWithWrapperTest
            description: Test top-level preconditions with wrapper.
            preconditions:
              - org.openrewrite.text.Find:
                  find: needle
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: changed
            """, "org.openrewrite.TopLevelWithWrapperTest"),
          text("needle", "changed"),  // Has needle -> top-level passes -> changed
          text("haystack")  // No needle -> top-level fails -> NOT changed
        );
    }

    @Test
    void yamlInlineAndTopLevelPreconditions() {
        // Test that both top-level AND inline preconditions must be satisfied
        // Top-level: Find "needle"
        // Inline: FindSourceFiles "**/target.txt"
        // Only the first file (has needle, at target.txt) should be changed
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.CombinedPreconditionsTest
            description: Test combination of top-level and inline preconditions.
            preconditions:
              - org.openrewrite.text.Find:
                  find: needle
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: changed
                  preconditions:
                    - org.openrewrite.FindSourceFiles:
                        filePattern: "**/target.txt"
            """, "org.openrewrite.CombinedPreconditionsTest"),
          // Has needle, at target.txt -> BOTH preconditions pass -> changed
          text("needle", "changed", spec -> spec.path("target.txt")),
          // Has needle, NOT at target.txt -> top-level passes, inline fails -> NOT changed
          text("needle", spec -> spec.path("other.txt")),
          // NO needle, at target.txt -> top-level fails -> NOT changed (regardless of inline)
          text("haystack", spec -> spec.path("also-target.txt"))
        );
    }

    @Test
    void yamlInlinePreconditionWithScanningRecipe() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.InlineScanningPreconditionTest
            description: Test inline preconditions with scanning recipe.
            recipeList:
              - org.openrewrite.text.FindAndReplace:
                  find: foo
                  replace: bar
                  preconditions:
                    - org.openrewrite.search.RepositoryContainsFile:
                        filePattern: pom.xml
            """, "org.openrewrite.InlineScanningPreconditionTest"),
          text("pom", spec -> spec.path("pom.xml")),
          text("foo", "bar")
        );
    }

    @Test
    void yamlTwoInlinePreconditionsOnSingleRecipe() {
        // Both inline preconditions must be satisfied (AND logic)
        // Precondition 1: file must match "**/target.txt"
        // Precondition 2: file content must contain "needle"
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.TwoInlinePreconditionsTest
            description: Test two inline preconditions on a single recipe.
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: changed
                  preconditions:
                    - org.openrewrite.FindSourceFiles:
                        filePattern: "**/target.txt"
                    - org.openrewrite.text.Find:
                        find: needle
            """, "org.openrewrite.TwoInlinePreconditionsTest"),
          // Matches both preconditions -> changed
          text("needle", "changed", spec -> spec.path("target.txt")),
          // Matches file pattern but NOT content -> NOT changed
          text("haystack", spec -> spec.path("target.txt")),
          // Matches content but NOT file pattern -> NOT changed
          text("needle", spec -> spec.path("other.txt")),
          // Matches neither -> NOT changed
          text("haystack", spec -> spec.path("other.txt"))
        );
    }

    @Test
    void yamlInlinePreconditionNotMet() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.InlinePreconditionNotMetTest
            description: Test inline preconditions when not met.
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: changed
                  preconditions:
                    - org.openrewrite.FindSourceFiles:
                        filePattern: "**/nonexistent.txt"
            """, "org.openrewrite.InlinePreconditionNotMetTest"),
          text("original")
        );
    }

    @Test
    void yamlMixedRecipesWithAndWithoutPreconditions() {
        // Test that recipes without preconditions run unconditionally,
        // while recipes with preconditions only run when their preconditions are met
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.MixedPreconditionsTest
            description: Test recipes with and without inline preconditions.
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: conditional-changed
                  preconditions:
                    - org.openrewrite.FindSourceFiles:
                        filePattern: "**/target.txt"
              - org.openrewrite.text.FindAndReplace:
                  find: unconditional
                  replace: replaced
            """, "org.openrewrite.MixedPreconditionsTest"),
          text("unconditional", "replaced", spec -> spec.path("any.txt")),
          text("unconditional", "conditional-changed", spec -> spec.path("target.txt"))
        );
    }

    @Test
    void yamlInlinePreconditionWithDeclarativeRecipe() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.InlinePreconditionWithDeclarativeTest
            description: Test inline preconditions with declarative recipe reference.
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: changed
                  preconditions:
                    - org.openrewrite.FindAnyTarget
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.FindAnyTarget
            recipeList:
              - org.openrewrite.FindSourceFiles:
                  filePattern: "**/target1.txt"
              - org.openrewrite.FindSourceFiles:
                  filePattern: "**/target2.txt"
            """, "org.openrewrite.InlinePreconditionWithDeclarativeTest"),
          text("original", "changed", spec -> spec.path("target1.txt")),
          text("original", "changed", spec -> spec.path("target2.txt")),
          text("original", spec -> spec.path("other.txt"))
        );
    }
}
