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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openrewrite.test.SourceSpecs.text;

class YamlResourceLoaderTest implements RewriteTest {

    @BeforeAll
    static void beforeAll() {
        try {
            // Instantiate once to throw a ExceptionInInitializerError and subsequent
            // instantiations will throw a NoClassDefFoundError.
            new RecipeWithBadStaticInitializer();
        } catch (ExceptionInInitializerError ignored) {
        }
    }

    @DocumentExample
    @Test
    void recipeExamples() {
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.ChangeTextToHello
              displayName: Change text to hello
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/example
              recipeName: test.ChangeTextToHello
              examples:
                - description: "Change World to Hello in a text file"
                  sources:
                    - before: "World"
                      after: "Hello!"
                      path: "1.txt"
                      language: "text"
                    - before: "World 2"
                      after: "Hello 2!"
                      path: "2.txt"
                      language: "text"
                - description: "Change World to Hello in a java file"
                  parameters:
                    - arg0
                    - 1
                  sources:
                    - before: |
                        public class A {
                            void method() {
                                System.out.println("World");
                            }
                        }
                      after: |
                        public class A {
                            void method() {
                                System.out.println("Hello!");
                            }
                        }
                      language: java
              """.getBytes()
          ), URI.create("attribution/test.ChangeTextToHello.yml"), new Properties()))
          .build();

        Collection<Recipe> recipes = env.listRecipes();
        assertThat(recipes).singleElement().satisfies(r -> {
            assertThat(r.getExamples()).hasSize(2);
            assertThat(r.getExamples()).first().satisfies(e -> {
                assertThat(e.getDescription()).isEqualTo("Change World to Hello in a text file");
                assertThat(e.getSources()).hasSize(2);
                assertThat(e.getSources()).first().satisfies(s -> {
                      assertThat(s.getBefore()).isEqualTo("World");
                      assertThat(s.getAfter()).isEqualTo("Hello!");
                      assertThat(s.getPath()).isEqualTo("1.txt");
                      assertThat(s.getLanguage()).isEqualTo("text");
                  }
                );

                assertThat(e.getSources().get(1)).satisfies(s -> {
                      assertThat(s.getBefore()).isEqualTo("World 2");
                      assertThat(s.getAfter()).isEqualTo("Hello 2!");
                      assertThat(s.getPath()).isEqualTo("2.txt");
                      assertThat(s.getLanguage()).isEqualTo("text");
                  }
                );
            });
            assertThat(r.getExamples().get(1)).satisfies(e -> {
                assertThat(e.getDescription()).isEqualTo("Change World to Hello in a java file");

                assertThat(e.getParameters()).hasSize(2);
                assertThat(e.getParameters().getFirst()).isEqualTo("arg0");
                assertThat(e.getParameters().get(1)).isEqualTo("1");

                assertThat(e.getSources()).hasSize(1);
                assertThat(e.getSources()).first().satisfies(s -> {
                    //language=java
                    assertThat(s.getBefore()).isEqualTo("""
                      public class A {
                          void method() {
                              System.out.println("World");
                          }
                      }
                      """);
                    //language=java
                    assertThat(s.getAfter()).isEqualTo("""
                      public class A {
                          void method() {
                              System.out.println("Hello!");
                          }
                      }
                      """);
                    assertThat(s.getPath()).isNull();
                    assertThat(s.getLanguage()).isEqualTo("java");
                });
            });
        });

        Collection<RecipeDescriptor> recipeDescriptors = env.listRecipeDescriptors();
        assertThat(recipeDescriptors).singleElement().satisfies(descriptor -> {
            List<RecipeExample> descriptorExamples = descriptor.getExamples();
            assertThat(descriptorExamples).containsExactlyElementsOf(recipes.iterator().next().getExamples());
        });
    }

    @Test
    void dataTables() {
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.ChangeTextToHello
              displayName: Change text to hello
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        Collection<RecipeDescriptor> recipeDescriptors = env.listRecipeDescriptors();
        assertThat(recipeDescriptors).hasSize(1);
        assertThat(recipeDescriptors.iterator().next().getDataTables()).isNotEmpty();
    }

    @Test
    void maintainers() {
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.ChangeTextToHello
              displayName: Change text to hello
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              maintainers:
                  - maintainer: Sam
                    logo: https://sam.com/logo.svg
                  - maintainer: Jon
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        Collection<RecipeDescriptor> recipeDescriptors = env.listRecipeDescriptors();
        assertThat(recipeDescriptors).hasSize(1);
        RecipeDescriptor descriptor = recipeDescriptors.iterator().next();
        assertThat(descriptor.getDataTables()).isNotEmpty();
        assertThat(descriptor.getMaintainers()).hasSize(2);
        Maintainer sam = descriptor.getMaintainers().getFirst();
        assertThat(sam.getMaintainer()).isEqualTo("Sam");
        assertThat(sam.getLogo()).isNotNull();
        assertThat(sam.getLogo().toString()).isEqualTo("https://sam.com/logo.svg");
        Maintainer jon = descriptor.getMaintainers().get(1);
        assertThat(jon.getMaintainer()).isEqualTo("Jon");
        assertThat(jon.getLogo()).isNull();
    }

    @Test
    void recipeContributorAttribution() {
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.ChangeTextToHello
              displayName: Change text to hello
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/attribution
              recipeName: test.ChangeTextToHello
              contributors:
                - name: "Jonathan Schneider"
                  email: "jon@moderne.io"
                  lineCount: 5
                - name: "Sam Snyder"
                  email: "sam@moderne.io"
                  lineCount: 3
              """.getBytes()
          ), URI.create("attribution/test.ChangeTextToHello.yml"), new Properties()))
          .build();
        Collection<Recipe> recipes = env.listRecipes();
        assertThat(recipes).hasSize(1);
        Recipe recipe = recipes.iterator().next();
        List<Contributor> contributors = recipe.getContributors();
        assertThat(contributors).hasSize(2);
        Contributor jon = contributors.getFirst();
        assertThat(jon.getName()).isEqualTo("Jonathan Schneider");
        assertThat(jon.getEmail()).isEqualTo("jon@moderne.io");
        assertThat(jon.getLineCount()).isEqualTo(5);
        Contributor sam = contributors.get(1);
        assertThat(sam.getName()).isEqualTo("Sam Snyder");
        assertThat(sam.getEmail()).isEqualTo("sam@moderne.io");
        assertThat(sam.getLineCount()).isEqualTo(3);

        Collection<RecipeDescriptor> recipeDescriptors = env.listRecipeDescriptors();
        assertThat(recipeDescriptors).hasSize(1);
        RecipeDescriptor descriptor = recipeDescriptors.iterator().next();
        List<Contributor> descriptorContributors = descriptor.getContributors();
        assertThat(descriptorContributors).containsExactlyElementsOf(contributors);

        RecipeDescriptor recipeDescriptor = recipe.getDescriptor();
        assertThat(recipeDescriptor.getContributors()).containsExactlyElementsOf(contributors);
    }

    @Test
    void declarativeAttributionIncludesRecipeListContributors() {
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/attribution
              recipeName: org.openrewrite.text.ChangeText
              contributors:
                - name: "Jonathan Schneider"
                  email: "jon@moderne.io"
                  lineCount: 5
              """.getBytes()
          ), URI.create("attribution/org.openrewrite.text.ChangeText.yml"), new Properties()))
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.ChangeTextToHello
              displayName: Change text to hello
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();
        Collection<Recipe> recipes = env.listRecipes();
        assertThat(recipes).hasSize(1);
        Recipe recipe = recipes.iterator().next();
        Optional<Contributor> maybeJon = recipe.getContributors()
          .stream()
          .filter(c -> c.getName().equals("Jonathan Schneider"))
          .findFirst();
        assertThat(maybeJon).isPresent();
    }

    @Test
    void caseInsensitiveEnums() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            //language=yml
            """
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.gradle.testCaseInsensitiveEnumInYaml
              displayName: test Enum in yaml
              description: test Enum in yaml.
              recipeList:
                - org.openrewrite.text.AppendToTextFile:
                    relativeFileName: "file.txt"
                    content: " World!"
                    preamble: "preamble"
                    appendNewline : false
                    existingFileStrategy: "cOnTiNuE"
              """,
            "org.openrewrite.gradle.testCaseInsensitiveEnumInYaml"
          ),
          text("Hello", "Hello World!")
        );
    }

    @Test
    void loadRecipeWithRecipeDataStringThatThrowsNoClassDefFoundError() {
        assertRecipeWithRecipeDataThatThrowsNoClassDefFoundError(
          RecipeWithBadStaticInitializer.class.getName());
    }

    @Test
    void loadRecipeWithRecipeDataMapThatThrowsNoClassDefFoundError() {
        assertRecipeWithRecipeDataThatThrowsNoClassDefFoundError(
          Map.of(RecipeWithBadStaticInitializer.class.getName(), Map.of()));
    }

    private void assertRecipeWithRecipeDataThatThrowsNoClassDefFoundError(Object recipeData) {
        final List<Validated<Object>> invalidRecipes = new ArrayList<>();
        YamlResourceLoader resourceLoader = createYamlResourceLoader();

        resourceLoader.loadRecipe(
          "org.company.CustomRecipe",
          0,
          recipeData,
          recipe -> {
          },
          recipe -> {
          },
          invalidRecipes::add);

        assertEquals(1, invalidRecipes.size());
    }

    private YamlResourceLoader createYamlResourceLoader() {
        return new YamlResourceLoader(
          new ByteArrayInputStream("type: specs.openrewrite.org/v1beta/recipe".getBytes()),
          URI.create("rewrite.yml"),
          new Properties());
    }

    private static class RecipeWithBadStaticInitializer extends Recipe {
        // Explicitly fail static initialization
        static final int val = 1 / 0;

        @Override
        public String getDisplayName() {
            return "";
        }

        @Override
        public String getDescription() {
            return "";
        }
    }
}
