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
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class EnvironmentTest implements RewriteTest {
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

        var results = recipe.run(List.of(new PlainText(Tree.randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "hello"))).getResults();
        assertThat(results).hasSize(1);
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
                  recipeList:
                      - test.ChangeTextToHello
                  ---
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.ChangeTextToHello
                  displayName: Change text to hello
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

        var results = recipe.run(List.of(new PlainText(Tree.randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "hello"))).getResults();
        assertThat(results).hasSize(1);
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

        var results = recipe.run(List.of(new PlainText(Tree.randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "hello"))).getResults();
        assertThat(results).hasSize(1);
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
                  displayName: License header.
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
        var env = Environment.builder().scanRuntimeClasspath().build();

        assertThat(env.listRecipes()).hasSizeGreaterThanOrEqualTo(2)
          .extracting("name")
          .contains("org.openrewrite.text.ChangeTextToJon", "org.openrewrite.HelloJon");

        assertThat(env.listStyles()).hasSizeGreaterThanOrEqualTo(1)
          .extracting("name")
          .contains("org.openrewrite.SampleStyle");
    }

    @Test
    void listRecipeDescriptors() {
        var env = Environment.builder().scanRuntimeClasspath().build();
        var recipeDescriptors = env.listRecipeDescriptors();
        assertThat(recipeDescriptors).isNotNull().isNotEmpty();
        var changeTextDescriptor = recipeDescriptors.stream().filter(rd -> rd.getName().equals("org.openrewrite.text.ChangeText"))
          .findAny().orElse(null);
        assertThat(changeTextDescriptor).isNotNull();
        assertThat(changeTextDescriptor.getOptions()).hasSize(1);
        assertThat(changeTextDescriptor.getOptions().get(0).getName()).isEqualTo("toText");
        assertThat(changeTextDescriptor.getOptions().get(0).getType()).isEqualTo("String");
    }

    @Test
    void listStyles() {
        var env = Environment.builder().scanRuntimeClasspath().build();
        var styles = env.listStyles();
        assertThat(styles).isNotNull().isNotEmpty();
        var sampleStyle = styles.stream().filter(s -> s.getName().equals("org.openrewrite.SampleStyle"))
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
        var helloJon2 = recipeDescriptors.stream().filter(rd -> rd.getName().equals("org.openrewrite.HelloJon2"))
          .findAny().orElseThrow();
        assertThat(helloJon2.getRecipeList()).hasSize(1);
        assertThat(helloJon2.getRecipeList().get(0).getName()).isEqualTo("org.openrewrite.HelloJon");
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
        var recipeList = env.activateRecipes("test.OrderPreserved").getRecipeList().get(0).getRecipeList();
        assertThat(recipeList.get(0).getName()).isEqualTo("org.openrewrite.config.RecipeNoParameters");
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
