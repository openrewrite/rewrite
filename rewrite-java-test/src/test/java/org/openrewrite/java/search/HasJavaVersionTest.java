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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.test.SourceSpecs.text;

class HasJavaVersionTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"[8,17)", "11", "11.x"})
    void matches(String version) {
        rewriteRun(
          spec -> spec.recipe(new HasJavaVersion(version, false)),
          version(
            java(
              """
                class Test {
                }
                """,
              """
                /*~~>*/class Test {
                }
                """
            ),
            11
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"[8,17)", "11"})
    void noMatch(String version) {
        rewriteRun(
          spec -> spec.recipe(new HasJavaVersion(version, false)),
          version(
            java(
              """
                class Test {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void declarativePrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.PreconditionTest
            preconditions:
              - org.openrewrite.java.search.HasJavaVersion:
                  version: 11
            recipeList:
              - org.openrewrite.text.ChangeText:
                 toText: 2
            """, "org.openrewrite.PreconditionTest"),
          text("1")
        );
    }

}
