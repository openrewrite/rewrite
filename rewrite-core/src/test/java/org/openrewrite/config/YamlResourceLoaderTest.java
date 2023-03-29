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
import org.openrewrite.Contributor;
import org.openrewrite.Maintainer;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class YamlResourceLoaderTest implements RewriteTest {

    @Test
    void dataTables() {
        Environment env = Environment.builder()
          .load(new YamlResourceLoader(new ByteArrayInputStream(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.ChangeTextToHello
              displayName: Change text to hello
              applicability:
                  singleSource:
                      - org.openrewrite.FindSourceFiles:
                          filePattern: '**/hello.txt'
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
        Maintainer sam = descriptor.getMaintainers().get(0);
        assertThat(sam.getMaintainer()).isEqualTo("Sam");
        assertThat(sam.getLogo()).isNotNull();
        assertThat(sam.getLogo().toString()).isEqualTo("https://sam.com/logo.svg");
        Maintainer jon = descriptor.getMaintainers().get(1);
        assertThat(jon.getMaintainer()).isEqualTo("Jon");
        assertThat(jon.getLogo()).isNull();
    }

    @Test
    void singleSourceApplicability() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.ChangeTextToHello
              displayName: Change text to hello
              applicability:
                  singleSource:
                      - org.openrewrite.FindSourceFiles:
                          filePattern: '**/hello.txt'
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              """
            ,
            "test.ChangeTextToHello"
          ),
          text(
            "Hello, world!",
            "Hello!",
            spec -> spec.path("hello.txt")
          ),
          text(
            "Hello, world!",
            spec -> spec.path("ignore.txt")
          )
        );
    }

    @Test
    void anySourceApplicability() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.ChangeTextToHello
              displayName: Change text to hello
              applicability:
                  anySource:
                      - org.openrewrite.FindSourceFiles:
                          filePattern: '**/hello.txt'
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              """,
            "test.ChangeTextToHello"
          ),
          text(
            "Hello, world!",
            "Hello!",
            spec -> spec.path("hello.txt")
          ),
          text(
            "Hello, world!",
            "Hello!",
            spec -> spec.path("goodbye.txt")
          )
        );
    }

    @Test
    void bothAnySourceAndSingleSourceApplicability() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            //language=yml
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: test.ChangeTextToHello
              displayName: Change text to hello
              applicability:
                  anySource:
                      - org.openrewrite.FindSourceFiles:
                          filePattern: '**/day.txt'
                  singleSource:
                      - org.openrewrite.FindSourceFiles:
                          filePattern: '**/night.txt'
              recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: Hello!
              """,
            "test.ChangeTextToHello"
          ),
          text(
            "Good morning!",
            spec -> spec.path("day.txt")
          ),
          text(
            "Good night!",
            "Hello!",
            spec -> spec.path("night.txt")
          )
        );
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
        Contributor jon = contributors.get(0);
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
        Optional<Contributor> maybeJon = recipe.getContributors().stream().filter(c -> c.getName().equals("Jonathan Schneider")).findFirst();
        assertThat(maybeJon).isPresent();
    }
}
