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
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
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
              .sourcePath(Paths.get("test.txt"))
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
              .sourcePath(Paths.get("test.txt"))
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
              .sourcePath(Paths.get("test.txt"))
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
    void scanClasspath() {
        var env = Environment.builder()
          .scanRuntimeClasspath()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/attribution
              recipeName: org.openrewrite.text.ChangeTextToJon
              contributors:
                - name: "Jonathan Schneider"
                  email: "jon@moderne.io"
                  lineCount: 5
              """.getBytes()
          ), URI.create("attribution/test.ChangeTextToHello.yml"), new Properties()))
          .build();

        Collection<Recipe> recipes = env.listRecipes();
        assertThat(recipes).hasSizeGreaterThanOrEqualTo(2)
          .extracting("name")
          .contains("org.openrewrite.text.ChangeTextToJon", "org.openrewrite.HelloJon");

        assertThat(env.listStyles()).hasSizeGreaterThanOrEqualTo(1)
          .extracting("name")
          .contains("org.openrewrite.SampleStyle");

        //noinspection OptionalGetWithoutIsPresent
        Recipe cttj = recipes.stream()
          .filter(it -> "org.openrewrite.text.ChangeTextToJon".equals(it.getName()))
          .findAny()
          .get();

        assertThat(cttj.getContributors())
          .isNotEmpty();
    }

    @Test
    void listRecipeDescriptors() {
        var env = Environment.builder().scanRuntimeClasspath().build();
        var recipeDescriptors = env.listRecipeDescriptors();
        assertThat(recipeDescriptors).isNotNull().isNotEmpty();
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
        assertThat(styles).isNotNull().isNotEmpty();
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
        assertThat(recipeDescriptors).isNotNull().isNotEmpty();
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
}
