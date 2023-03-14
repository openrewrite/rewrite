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
import org.openrewrite.Maintainer;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collection;
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
        assertThat(descriptor.getMaintainers().size()).isEqualTo(2);
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
}
