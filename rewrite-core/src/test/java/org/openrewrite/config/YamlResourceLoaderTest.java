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

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;

import static java.util.Collections.emptyList;
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

    @Test
    void exampleDocumentsAreIgnored() {
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
              ---
              type: specs.openrewrite.org/v1beta/example
              recipeName: test.ChangeTextToHello
              examples:
                - description: "Change World to Hello"
                  sources:
                    - before: "World"
                      after: "Hello!"
                      language: "text"
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        assertThat(env.listRecipes()).singleElement()
          .satisfies(recipe -> assertThat(recipe.getExamples()).isEmpty());
        assertThat(env.listRecipeDescriptors()).singleElement()
          .satisfies(descriptor -> assertThat(descriptor.getExamples()).isEmpty());
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
    void nonStringTagsAreReportedAsValidationErrorsInsteadOfThrowing() {
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.ChangeTextToHello
              displayName: Change text to hello
              tags:
                - fine
                - not_string_tag: boom
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        Recipe recipe = env.listRecipes().iterator().next();

        // Only valid String tags are retained, so iterating getTags() does not throw a ClassCastException
        assertThat(recipe.getTags()).containsExactly("fine");
        recipe.getTags().forEach(tag -> assertThat(tag).isInstanceOf(String.class));

        // The non-string tag surfaces as a validation error rather than crashing at runtime
        assertThat(recipe.validate().isValid()).isFalse();
        assertThat(recipe.validate().failures())
          .anySatisfy(failure -> {
              assertThat(failure.getProperty()).isEqualTo("test.ChangeTextToHello.tags");
              assertThat(failure.getMessage()).contains("tags must be a list of strings");
          });
    }

    @Test
    void unknownOptionIsReportedAsValidationError() {
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
                      toTxt: Hello?
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        Recipe recipe = env.activateRecipes("test.ChangeTextToHello");

        assertThat(recipe.validateAll())
          .flatMap(Validated::failures)
          .singleElement()
          .satisfies(failure -> {
              assertThat(failure.getProperty()).isEqualTo("org.openrewrite.text.ChangeText.toTxt");
              assertThat(failure.getMessage()).isEqualTo("is not an option of this recipe; valid options are: toText");
          });
    }

    @Test
    void optionFieldThatJacksonCannotSetIsNotReportedAsUnknown() {
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.FieldOption
              displayName: Field option
              recipeList:
                  - org.openrewrite.config.YamlResourceLoaderTest$RecipeWithFieldOption:
                      fieldOption: value
              """.getBytes()
          ), URI.create("rewrite.yml"), new Properties()))
          .build();

        assertThat(env.activateRecipes("test.FieldOption").validateAll())
          .flatMap(Validated::failures)
          .isEmpty();
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

    @Test
    void loadRecipeWhoseStaticInitializerHasNotYetFailed() {
        // Unlike RecipeWithBadStaticInitializer, this class is never pre-loaded, so the first
        // load throws ExceptionInInitializerError rather than NoClassDefFoundError.
        final List<Validated<Object>> invalidRecipes = new ArrayList<>();

        createYamlResourceLoader().loadRecipe(
          "org.company.CustomRecipe",
          0,
          RecipeFailingOnFirstLoad.class.getName(),
          recipe -> {
          },
          recipe -> {
          },
          invalidRecipes::add);

        assertEquals(1, invalidRecipes.size());
        Validated.Invalid<Object> invalid = (Validated.Invalid<Object>) invalidRecipes.get(0);
        assertThat(invalid.getException()).isInstanceOf(ExceptionInInitializerError.class);
        assertThat(invalid.getMessage()).contains("ExceptionInInitializerError");
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
        Validated.Invalid<Object> invalid = (Validated.Invalid<Object>) invalidRecipes.get(0);
        assertThat(invalid.getMessage())
          .as("must report why the class could not be loaded, not merely that it could not be")
          .contains("Could not initialize class");
        assertThat(invalid.getException())
          .as("the originating error must be retained for callers that can surface it")
          .isInstanceOf(NoClassDefFoundError.class);
    }

    private YamlResourceLoader createYamlResourceLoader() {
        return new YamlResourceLoader(
          new ByteArrayInputStream("type: specs.openrewrite.org/v1beta/recipe".getBytes()),
          URI.create("rewrite.yml"),
          new Properties());
    }

    @Test
    void nullPropertiesSkipsPlaceholderResolution() {
        String yaml = //language=yml
          """
            type: specs.openrewrite.org/v1beta/recipe
            name: test.ChangeTextWithPlaceholder
            displayName: Change text with placeholder
            description: Test that placeholder resolution respects null properties.
            recipeList:
                - org.openrewrite.text.ChangeText:
                    toText: "${java_home.jdk21:/bin/java}"
            """;

        // With null properties, placeholder with default value should be preserved as-is
        rewriteRun(
          spec -> spec.recipe(Environment.builder(null)
            .load(new YamlResourceLoader(new ByteArrayInputStream(yaml.getBytes()),
              URI.create("rewrite.yml"), null))
            .build().listRecipes().iterator().next()),
          text("hello", "${java_home.jdk21:/bin/java}")
        );

        // With empty properties, the default value gets resolved
        rewriteRun(
          spec -> spec.recipe(Environment.builder(new Properties())
            .load(new YamlResourceLoader(new ByteArrayInputStream(yaml.getBytes()),
              URI.create("rewrite.yml"), new Properties()))
            .build().listRecipes().iterator().next()),
          text("hello", "/bin/java")
        );
    }

    @Test
    void loadRecipeReturnsNullWhenMarketplaceDoesNotListTheRecipe() {
        YamlResourceLoader userYaml = marketplaceBackedLoader(
          //language=yml
          """
            type: specs.openrewrite.org/v1beta/recipe
            name: user.BestPractices
            displayName: User best practices
            description: Test.
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: Hello!
            """);

        assertThat(userYaml.loadRecipe("user.BestPractices"))
          .as("a recipe this loader defines is returned")
          .isNotNull();
        assertThat(userYaml.loadRecipe("org.example.NotInThisYaml"))
          .as("a recipe neither defined here nor listed in the marketplace is reported as absent")
          .isNull();
    }

    @Test
    void marketplaceBackedLoaderDoesNotAbortResolutionOfATransitiveRecipe() {
        // Loader order is load bearing: the marketplace-backed loader, which can supply neither
        // recipe, must be consulted between the loader defining Outer and the one defining Inner.
        Environment env = Environment.builder()
          .load(declarativeLoader("org.example.Outer", "org.example.Inner", "outer.yml"))
          .load(marketplaceBackedLoader(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: user.Unrelated
              displayName: Unrelated
              description: Test.
              recipeList:
                - org.openrewrite.text.ChangeText:
                    toText: Hello!
              """))
          .load(declarativeLoader("org.example.Inner", "org.openrewrite.text.ChangeText:\n        toText: Hello!", "inner.yml"))
          .build();

        Recipe outer = env.activateRecipes("org.example.Outer");

        assertThat(outer.getRecipeList()).singleElement()
          .extracting(Recipe::getName).isEqualTo("org.example.Inner");
        assertThat(outer.validate().isValid()).isTrue();
    }

    @Test
    void unresolvableTransitiveRecipeIsReportedAsValidationErrorRatherThanThrowing() {
        Environment env = Environment.builder()
          .load(declarativeLoader("org.example.Outer", "org.example.Missing", "outer.yml"))
          .load(marketplaceBackedLoader(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: user.Unrelated
              displayName: Unrelated
              description: Test.
              recipeList:
                - org.openrewrite.text.ChangeText:
                    toText: Hello!
              """))
          .build();

        Recipe outer = env.activateRecipes("org.example.Outer");

        assertThat(outer.validate().failures())
          .anySatisfy(f -> {
              assertThat(f.getProperty()).startsWith("org.example.Outer.recipeList[0]");
              assertThat(f.getInvalidValue()).isEqualTo("org.example.Missing");
              assertThat(f.getMessage()).isEqualTo("refers to a recipe that doesn't exist.");
          });
    }

    private static YamlResourceLoader declarativeLoader(String name, @Language("yml") String subRecipe, String source) {
        return new YamlResourceLoader(new ByteArrayInputStream((
          //language=yml
          """
            type: specs.openrewrite.org/v1beta/recipe
            name: %s
            displayName: %s
            description: Test.
            recipeList:
              - %s
            """.formatted(name, name, subRecipe)).getBytes()), URI.create(source), new Properties());
    }

    private static YamlResourceLoader marketplaceBackedLoader(@Language("yml") String yaml) {
        return new YamlResourceLoader(new ByteArrayInputStream(yaml.getBytes()),
          URI.create("file:///user.yaml"), new Properties(), new RecipeMarketplace(), emptyList());
    }

    public static class RecipeWithFieldOption extends Recipe {
        @Option(displayName = "Field option", description = "Has no constructor parameter or setter.")
        @Nullable
        String fieldOption;

        @Override
        public String getDisplayName() {
            return "Recipe with field option";
        }

        @Override
        public String getDescription() {
            return "Recipe with field option.";
        }
    }

    private static class RecipeFailingOnFirstLoad extends Recipe {
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
