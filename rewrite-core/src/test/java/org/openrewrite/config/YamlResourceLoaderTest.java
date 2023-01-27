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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;

import static org.openrewrite.test.SourceSpecs.text;

public class YamlResourceLoaderTest implements RewriteTest {

    @Test
    void singleSourceApplicability() {
        rewriteRun(
          spec -> spec.recipe(
            new ByteArrayInputStream(
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
            ),
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
          spec -> spec.recipe(
            new ByteArrayInputStream(
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
                """.getBytes()
            ),
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

    // todo, kunli, how to test with expected exception?
    @Disabled
    @Test
    void anySourceAndSingleSourceApplicability() {
        rewriteRun(
          spec -> spec.recipe(
            new ByteArrayInputStream(
              //language=yml
              """
                type: specs.openrewrite.org/v1beta/recipe
                name: test.ChangeTextToHello
                displayName: Change text to hello
                applicability:
                    anySource:
                        - org.openrewrite.FindSourceFiles:
                            filePattern: '**/hello.txt'
                    singleSource:
                        - org.openrewrite.FindSourceFiles:
                            filePattern: '**/hello.txt'
                recipeList:
                    - org.openrewrite.text.ChangeText:
                        toText: Hello!
                """.getBytes()
            ),
            "test.ChangeTextToHello"
          ),
          text(
            "Hello, world!",
            spec -> spec.path("hello.txt")
          ),
          text(
            "Hello, world!",
            spec -> spec.path("goodbye.txt")
          )
        );
    }
}
