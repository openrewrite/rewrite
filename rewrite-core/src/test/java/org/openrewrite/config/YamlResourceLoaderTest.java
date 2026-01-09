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
    void duplicateNonDeclarativeRecipesAreNotDeduplicated() {
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.DuplicateRecipes
              displayName: Recipe with duplicates
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        Collection<Recipe> recipes = env.listRecipes();
        // Non-declarative recipes are not deduplicated since they may have different configurations
        assertThat(recipes).singleElement().satisfies(r -> {
            assertThat(r.getRecipeList()).hasSize(3);
        });
    }

    @Test
    void duplicateDeclarativeRecipesAreDeduplicated() {
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: test.SubRecipe
              displayName: Sub recipe
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: test.DuplicateDeclarativeRecipes
              displayName: Recipe with duplicate declarative recipes
              recipeList:
                  - test.SubRecipe
                  - test.SubRecipe
                  - test.SubRecipe
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        Collection<Recipe> recipes = env.listRecipes();
        assertThat(recipes).hasSize(2);

        Recipe parentRecipe = recipes.stream()
          .filter(r -> r.getName().equals("test.DuplicateDeclarativeRecipes"))
          .findFirst()
          .orElseThrow();

        assertThat(parentRecipe.getRecipeList()).hasSize(1);
        assertThat(parentRecipe.getRecipeList().getFirst().getName()).isEqualTo("test.SubRecipe");
    }

    @Test
    void duplicateDeclarativeRecipesAreDeduplicatedAcrossHierarchy() {
        // Recipe A includes B and C, both B and C include D
        // D should only be loaded once across the entire hierarchy
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: test.D
              displayName: Recipe D
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello from D!
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: test.B
              displayName: Recipe B
              recipeList:
                  - test.D
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: test.C
              displayName: Recipe C
              recipeList:
                  - test.D
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: test.A
              displayName: Recipe A
              recipeList:
                  - test.B
                  - test.C
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        Collection<Recipe> recipes = env.listRecipes();
        assertThat(recipes).hasSize(4);

        Recipe recipeA = recipes.stream()
          .filter(r -> r.getName().equals("test.A"))
          .findFirst()
          .orElseThrow();

        // A should have B and C
        assertThat(recipeA.getRecipeList()).hasSize(2);

        Recipe recipeB = recipeA.getRecipeList().stream()
          .filter(r -> r.getName().equals("test.B"))
          .findFirst()
          .orElseThrow();

        Recipe recipeC = recipeA.getRecipeList().stream()
          .filter(r -> r.getName().equals("test.C"))
          .findFirst()
          .orElseThrow();

        // B should have D
        assertThat(recipeB.getRecipeList()).hasSize(1);
        assertThat(recipeB.getRecipeList().getFirst().getName()).isEqualTo("test.D");

        // C should NOT have D since it was already seen when initializing B
        assertThat(recipeC.getRecipeList()).isEmpty();
    }

    @Test
    void duplicateDeclarativeRecipesAreDeduplicatedAcrossMultipleYamlResourceLoaders() {
        // Recipe A includes B and C, both B and C include D
        // Each recipe comes from a different YamlResourceLoader
        // D should only be loaded once across the entire hierarchy
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.D
              displayName: Recipe D
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello from D!
              """.getBytes()
          ), URI.create("d.yml"), new Properties()))
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.B
              displayName: Recipe B
              recipeList:
                  - test.D
              """.getBytes()
          ), URI.create("b.yml"), new Properties()))
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.C
              displayName: Recipe C
              recipeList:
                  - test.D
              """.getBytes()
          ), URI.create("c.yml"), new Properties()))
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.A
              displayName: Recipe A
              recipeList:
                  - test.B
                  - test.C
              """.getBytes()
          ), URI.create("a.yml"), new Properties()))
          .build();

        Collection<Recipe> recipes = env.listRecipes();
        assertThat(recipes).hasSize(4);

        Recipe recipeA = recipes.stream()
          .filter(r -> r.getName().equals("test.A"))
          .findFirst()
          .orElseThrow();

        // A should have B and C
        assertThat(recipeA.getRecipeList()).hasSize(2);

        Recipe recipeB = recipeA.getRecipeList().stream()
          .filter(r -> r.getName().equals("test.B"))
          .findFirst()
          .orElseThrow();

        Recipe recipeC = recipeA.getRecipeList().stream()
          .filter(r -> r.getName().equals("test.C"))
          .findFirst()
          .orElseThrow();

        // B should have D
        assertThat(recipeB.getRecipeList()).hasSize(1);
        assertThat(recipeB.getRecipeList().getFirst().getName()).isEqualTo("test.D");

        // C should NOT have D since it was already seen when initializing B
        assertThat(recipeC.getRecipeList()).isEmpty();
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
