/*
 * Copyright 2022 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.openrewrite.test.SourceSpecs.text;

class EnvironmentTest implements RewriteTest {

    @DocumentExample
    @Test
    void declarativeRecipesDontAddToEstimatedTimeFixed() {
        rewriteRun(
          spec -> spec
            .recipeFromYaml(
              """
                type: specs.openrewrite.org/v1beta/recipe
                name: test.ChangeTextToHello
                displayName: Change text to hello
                description: Test.
                recipeList:
                    - org.openrewrite.text.ChangeText:
                        toText: Hello
                """,
              "test.ChangeTextToHello"
            )
            .afterRecipe(run -> {
                for (Result result : run.getChangeset().getAllResults()) {
                    assertThat(result.getTimeSavings()).isEqualTo(Duration.ofMinutes(5));
                }
            }),
          text(
            "Hi",
            "Hello"
          )
        );
    }

    @Test
    void listRecipes() {
        var env = Environment.builder()
          .load(
            new YamlResourceLoader(
              //language=yml
              new ByteArrayInputStream(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.ChangeTextToHello
                  displayName: Change text to hello
                  description: Test.
                  recipeList:
                      - org.openrewrite.text.ChangeText:
                          toText: Hello
                  """.getBytes()
              ),
              URI.create("rewrite.yml"),
              new Properties()
            )
          )
          .build();

        assertThat(env.listRecipes().stream()
          .map(Recipe::getName)).containsExactly("test.ChangeTextToHello");

        var recipe = env.activateRecipes("test.ChangeTextToHello");
        assertThat(recipe.validateAll()).allMatch(Validated::isValid);

        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(
            PlainText.builder()
              .sourcePath(Path.of("test.txt"))
              .text("hello")
              .build())), new InMemoryExecutionContext())
          .getChangeset()
          .getAllResults();
        assertThat(changes).hasSize(1);
    }

    @Test
    void activeRecipeNotFoundSuggestions() {
        var env = Environment.builder()
          .load(
            new YamlResourceLoader(
              //language=yml
              new ByteArrayInputStream(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.ChangeTextToHello
                  displayName: Change text to hello
                  description: Test.
                  recipeList:
                    - org.openrewrite.text.ChangeText:
                        toText: Hello
                  ---
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.ChangeTextToHelloWorld
                  displayName: Change text to hello world
                  description: Test.
                  recipeList:
                    - org.openrewrite.text.ChangeText:
                        toText: Hello
                  """.getBytes()
              ),
              URI.create("rewrite.yml"),
              new Properties()
            )
          )
          .build();

        assertThatExceptionOfType(RecipeException.class)
          .isThrownBy(() -> env.activateRecipes("foo.ChangeTextToHelloWorld"))
          .withMessageContaining("foo.ChangeTextToHelloWorld")
          .withMessageContaining("test.ChangeTextToHelloWorld");
    }

    @Test
    void recipeWithoutRequiredConfiguration() {
        var env = Environment.builder()
          .load(
            new YamlResourceLoader(
              //language=yml
              new ByteArrayInputStream(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.ChangeTextToHello
                  displayName: Change text to hello
                  description: Test.
                  recipeList:
                      - org.openrewrite.text.ChangeText
                  """.getBytes()
              ),
              URI.create("rewrite.yml"),
              new Properties()
            )
          )
          .build();

        var recipe = env.activateRecipes("test.ChangeTextToHello");
        assertThat(recipe.validateAll()).anyMatch(Validated::isInvalid);
    }

    @Test
    void recipeDependsOnOtherDeclarativeRecipe() {
        var env = Environment.builder()
          .load(
            new YamlResourceLoader(
              //language=yml
              new ByteArrayInputStream(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.TextMigration
                  displayName: Text migration
                  description: Test.
                  recipeList:
                      - test.ChangeTextToHello
                  ---
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.ChangeTextToHello
                  displayName: Change text to hello
                  description: Test.
                  recipeList:
                      - org.openrewrite.text.ChangeText:
                          toText: Hello
                  """.getBytes()
              ),
              URI.create("rewrite.yml"),
              new Properties()
            )
          )
          .build();

        var recipe = env.activateRecipes("test.TextMigration");
        assertThat(recipe.validateAll()).allMatch(Validated::isValid);

        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(
            PlainText.builder()
              .sourcePath(Path.of("test.txt"))
              .text("hello")
              .build())), new InMemoryExecutionContext())
          .getChangeset()
          .getAllResults();
        assertThat(changes).hasSize(1);
    }

    @Test
    void recipeDependsOnOtherDeclarativeRecipeSpecifiedInAnotherFile() {
        var env = Environment.builder()
          .load(
            new YamlResourceLoader(
              //language=yml
              new ByteArrayInputStream(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.TextMigration
                  displayName: Text migration
                  description: Test.
                  recipeList:
                      - test.ChangeTextToHello
                  """.getBytes()
              ),
              URI.create("rewrite.yml"),
              new Properties()
            )
          )
          .load(
            new YamlResourceLoader(
              new ByteArrayInputStream("""
                    type: specs.openrewrite.org/v1beta/recipe
                    name: test.ChangeTextToHello
                    displayName: Change text to hello
                    recipeList:
                        - org.openrewrite.text.ChangeText:
                            toText: Hello
                """.getBytes()
              ),
              URI.create("text.yml"),
              new Properties()
            )
          )
          .build();

        var recipe = env.activateRecipes("test.TextMigration");
        assertThat(recipe.validateAll()).allMatch(Validated::isValid);

        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(
            PlainText.builder()
              .sourcePath(Path.of("test.txt"))
              .text("hello")
              .build())), new InMemoryExecutionContext())
          .getChangeset()
          .getAllResults();
        assertThat(changes).hasSize(1);
    }

    @Test
    void recipeDependsOnNonExistentRecipe() {
        var env = Environment.builder()
          .load(
            new YamlResourceLoader(
              //language=yml
              new ByteArrayInputStream(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.TextMigration
                  displayName: Text migration
                  description: Test.
                  recipeList:
                      - test.DoesNotExist
                  """.getBytes()
              ),
              URI.create("rewrite.yml"),
              new Properties()
            )
          )
          .build();

        var recipe = env.activateRecipes("test.TextMigration");
        assertThat(recipe.validateAll()).anyMatch(Validated::isInvalid);
    }

    @Test
    void declarativeRecipeListClassCastException() {
        var env = Environment.builder()
          .load(
            new YamlResourceLoader(
              //language=yml
              new ByteArrayInputStream(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.LicenseHeader
                  displayName: License header
                  description: Test.
                  recipeList:
                    - org.openrewrite.java.AddLicenseHeader: |-
                        LicenseHeader
                  """.getBytes()
              ),
              URI.create("rewrite.yml"),
              new Properties()
            )
          ).build();

        var recipe = env.activateRecipes("test.LicenseHeader");
        assertThat(recipe.validateAll()).anyMatch(Validated::isInvalid);
    }

    @Test
    void declarativeRecipeWrongPackage() {
        var env = Environment.builder()
          .load(
            new YamlResourceLoader(
              //language=yml
              new ByteArrayInputStream(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.ResultOfFileMkdirsIgnored
                  displayName: Test
                  description: Test.
                  recipeList:
                    - org.openrewrite.java.ResultOfMethodCallIgnored:
                          methodPattern: 'java.io.File mkdir*()'
                  """.getBytes()
              ),
              URI.create("rewrite.yml"),
              new Properties()
            )
          ).build();

        var recipe = env.activateRecipes("test.ResultOfFileMkdirsIgnored");
        var validateAll = recipe.validateAll();
        assertThat(validateAll).anyMatch(Validated::isInvalid);
    }

    @Test
    void listRecipeDescriptors() {
        var env = Environment.builder().scanRuntimeClasspath().build();
        var recipeDescriptors = env.listRecipeDescriptors();
        assertThat(recipeDescriptors).isNotEmpty();
        var changeTextDescriptor = recipeDescriptors.stream().filter(rd -> "org.openrewrite.text.ChangeText".equals(rd.getName()))
          .findAny().orElse(null);
        assertThat(changeTextDescriptor).isNotNull();
        assertThat(changeTextDescriptor.getOptions()).hasSize(1);
        assertThat(changeTextDescriptor.getOptions().getFirst().getName()).isEqualTo("toText");
        assertThat(changeTextDescriptor.getOptions().getFirst().getType()).isEqualTo("String");
    }

    @Test
    void listStyles() {
        var env = Environment.builder().scanRuntimeClasspath().build();
        var styles = env.listStyles();
        assertThat(styles).isNotEmpty();
        var sampleStyle = styles.stream().filter(s -> "org.openrewrite.SampleStyle".equals(s.getName()))
          .findAny().orElse(null);
        assertThat(sampleStyle).isNotNull();
        assertThat(sampleStyle.getDisplayName()).isEqualTo("Sample style");
        assertThat(sampleStyle.getDescription()).isEqualTo("Sample test style.");
        assertThat(sampleStyle.getTags()).containsExactly("testing");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/343")
    @Test
    void environmentActivatedRecipeUsableInTests() {
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.text.ChangeTextToJon")),
          text(
            "some text that isn't jon",
            "Hello Jon!"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/543")
    @Test
    void recipeDescriptorsFromCrossResources() {
        var env = Environment.builder().scanRuntimeClasspath().build();
        var recipeDescriptors = env.listRecipeDescriptors();
        assertThat(recipeDescriptors).isNotEmpty();
        var helloJon2 = recipeDescriptors.stream().filter(rd -> "org.openrewrite.HelloJon2".equals(rd.getName()))
          .findAny().orElseThrow();
        assertThat(helloJon2.getRecipeList()).hasSize(1);
        assertThat(helloJon2.getRecipeList().getFirst().getName()).isEqualTo("org.openrewrite.HelloJon");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1789")
    @Test
    void preserveRecipeListOrder() {
        var env = Environment.builder()
          .load(new YamlResourceLoader(
            //language=yaml
            new ByteArrayInputStream(
              """
                type: specs.openrewrite.org/v1beta/recipe
                name: test.FooOne
                displayName: Test
                description: Test.
                recipeList:
                  - org.openrewrite.config.RecipeAcceptingParameters:
                        foo: "foo"
                        bar: 1
                """.getBytes()
            ),
            URI.create("rewrite.yml"),
            new Properties()
          ))
          .load(new YamlResourceLoader(
            //language=yaml
            new ByteArrayInputStream(
              """
                type: specs.openrewrite.org/v1beta/recipe
                name: test.BarTwo
                displayName: Test
                recipeList:
                  - org.openrewrite.config.RecipeAcceptingParameters:
                        foo: "bar"
                        bar: 2
                """.getBytes()
            ),
            URI.create("rewrite.yml"),
            new Properties()
          ))
          .load(new YamlResourceLoader(
            //language=yaml
            new ByteArrayInputStream(
              """
                type: specs.openrewrite.org/v1beta/recipe
                name: test.OrderPreserved
                displayName: Test
                description: Test.
                recipeList:
                  - org.openrewrite.config.RecipeNoParameters
                  - test.FooOne
                  - org.openrewrite.config.RecipeAcceptingParameters:
                      foo: "bar"
                      bar: 2
                  - org.openrewrite.config.RecipeNoParameters
                  - test.BarTwo
                  - org.openrewrite.config.RecipeNoParameters
                """.getBytes()
            ),
            URI.create("rewrite.yml"),
            new Properties()
          ))
          .build();
        var recipeList = env.activateRecipes("test.OrderPreserved").getRecipeList();
        assertThat(recipeList.getFirst().getName()).isEqualTo("org.openrewrite.config.RecipeNoParameters");
        assertThat(recipeList.get(1).getName()).isEqualTo("test.FooOne");
        assertThat(recipeList.get(2).getName()).isEqualTo("org.openrewrite.config.RecipeAcceptingParameters");
        assertThat(recipeList.get(3).getName()).isEqualTo("org.openrewrite.config.RecipeNoParameters");
        assertThat(recipeList.get(4).getName()).isEqualTo("test.BarTwo");
        assertThat(recipeList.get(5).getName()).isEqualTo("org.openrewrite.config.RecipeNoParameters");
    }

    @Test
    void canCauseAnotherCycle() {
        var env = Environment.builder()
          .load(new YamlResourceLoader(
            //language=yaml
            new ByteArrayInputStream(
              """
                type: specs.openrewrite.org/v1beta/recipe
                name: test.Foo
                displayName: Test
                description: Test.
                causesAnotherCycle: true
                recipeList:
                  - org.openrewrite.config.RecipeNoParameters

                """.getBytes()
            ),
            URI.create("rewrite.yml"),
            new Properties()
          )).build();
        var recipe = env.activateRecipes("test.Foo");
        assertThat(recipe.causesAnotherCycle()).isTrue();
    }

    @Test
    void willBeValidIfIncludesRecipesFromDependencies() {
        var env = Environment.builder()
          .load(new YamlResourceLoader(
              //language=yaml
              new ByteArrayInputStream(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.Foo
                  displayName: Test
                  description: Test.
                  recipeList:
                    - org.openrewrite.config.RecipeNoParameters
                  """.getBytes()
              ),
              URI.create("rewrite.yml"),
              new Properties()
            ),
            List.of(new YamlResourceLoader(
              //language=yaml
              new ByteArrayInputStream(
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: org.openrewrite.config.RecipeNoParameters
                  displayName: Test
                  description: Test.
                  recipeList:
                    - org.openrewrite.config.RecipeSomeParameters
                  """.getBytes()
              ),
              URI.create("rewrite.yml"),
              new Properties()
            ))).build();
        var recipe = env.activateRecipes("test.Foo");
        assertThat(recipe.validate().isValid()).isTrue();
    }

    @Test
    void recipeReferencedMultipleTimesShouldNotBeReInitialized() {
        // This test reproduces the issue where a child recipe referenced by multiple parents
        // gets re-initialized each time, losing its recipe list

        String childYaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.ChildRecipe
          displayName: Child recipe
          description: A child recipe.
          recipeList:
            - org.openrewrite.text.ChangeText:
                toText: child-text
          """;

        String parent1Yaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.Parent1
          displayName: Parent 1
          description: First parent.
          recipeList:
            - org.openrewrite.ChildRecipe
          """;

        String parent2Yaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.Parent2
          displayName: Parent 2
          description: Second parent.
          recipeList:
            - org.openrewrite.ChildRecipe
          """;

        String grandparentYaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.Grandparent
          displayName: Grandparent
          description: Top-level recipe that references both parents.
          recipeList:
            - org.openrewrite.Parent1
            - org.openrewrite.Parent2
          """;

        Environment env = Environment.builder()
          .load(new YamlResourceLoader(
            new ByteArrayInputStream(childYaml.getBytes()),
            URI.create("child.yml"),
            new Properties()))
          .load(new YamlResourceLoader(
            new ByteArrayInputStream(parent1Yaml.getBytes()),
            URI.create("parent1.yml"),
            new Properties()))
          .load(new YamlResourceLoader(
            new ByteArrayInputStream(parent2Yaml.getBytes()),
            URI.create("parent2.yml"),
            new Properties()))
          .load(new YamlResourceLoader(
            new ByteArrayInputStream(grandparentYaml.getBytes()),
            URI.create("grandparent.yml"),
            new Properties()))
          .build();

        // Load the grandparent recipe - this will load both parents, which both reference the child
        Recipe grandparent = env.activateRecipes("org.openrewrite.Grandparent");

        assertThat(grandparent.getRecipeList())
          .as("Grandparent should have 2 parent recipes")
          .hasSize(2);

        Recipe parent1 = grandparent.getRecipeList().getFirst();
        assertThat(parent1.getRecipeList())
          .as("Parent1 should have the child recipe")
          .hasSize(1);

        Recipe childFromParent1 = parent1.getRecipeList().getFirst();
        assertThat(childFromParent1.getRecipeList())
          .as("Child recipe (from Parent1) should have its ChangeText recipe")
          .hasSize(1);

        Recipe parent2 = grandparent.getRecipeList().get(1);
        assertThat(parent2.getRecipeList())
          .as("Parent2 should have the child recipe")
          .hasSize(1);

        Recipe childFromParent2 = parent2.getRecipeList().getFirst();
        assertThat(childFromParent2.getRecipeList())
          .as("Child recipe (from Parent2) should still have its ChangeText recipe")
          .hasSize(1);

        // If both child instances are the same object, verify it's properly initialized
        if (childFromParent1 == childFromParent2) {
            assertThat(childFromParent1.getRecipeList())
              .as("Shared child recipe instance should have its ChangeText recipe")
              .hasSize(1);
        }
    }

    @Test
    void recipeLoadedMultipleTimesViaActivateRecipesShouldRemainInitialized() {
        // This test simulates what happens when activateRecipes() is called multiple times
        // for the same recipe name, which is common in test scenarios

        String recipeYaml = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.TestRecipe
          displayName: Test recipe
          description: A test recipe.
          recipeList:
            - org.openrewrite.text.ChangeText:
                toText: test-text
          """;

        Environment env = Environment.builder()
          .load(new YamlResourceLoader(
            new ByteArrayInputStream(recipeYaml.getBytes()),
            URI.create("test.yml"),
            new Properties()))
          .build();

        // Load the recipe the first time
        Recipe recipe1 = env.activateRecipes("org.openrewrite.TestRecipe");
        int initialSize1 = recipe1.getRecipeList().size();
        assertThat(initialSize1)
          .as("Recipe should have recipes after first activation")
          .isGreaterThan(0);

        // Load the same recipe a second time (simulating what might happen across different tests)
        Recipe recipe2 = env.activateRecipes("org.openrewrite.TestRecipe");
        assertThat(recipe2.getRecipeList())
          .as("Recipe should still have recipes after second activation")
          .hasSize(initialSize1);

        // Load it a third time to be sure
        Recipe recipe3 = env.activateRecipes("org.openrewrite.TestRecipe");
        assertThat(recipe3.getRecipeList())
          .as("Recipe should still have recipes after third activation")
          .hasSize(initialSize1);
    }
}
