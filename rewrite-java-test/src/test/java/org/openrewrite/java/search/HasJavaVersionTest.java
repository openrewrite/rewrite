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
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.test.SourceSpecs.text;

class HasJavaVersionTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"[8,17)", "11", "11.x"})
    void matches(String version) {
        rewriteRun(
          spec -> spec.recipe(new HasJavaVersion(version, false)),
          java(
            """
              class Test {
              }
              """,
            """
              /*~~>*/class Test {
              }
              """,
            spec -> spec.markers(javaVersion(11))
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"[8,17)", "11"})
    void noMatch(String version) {
        rewriteRun(
          spec -> spec.recipe(new HasJavaVersion(version, false)),
          java(
            """
              class Test {
              }
              """,
            spec -> spec.markers(javaVersion(17))
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

    @Test
    void declarativePreconditionMatch() {
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
          text("1", "2", spec -> spec.markers(javaVersion(11)))
        );
    }

    @Test
    void combinedWithFindMethod() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.CombinedWithFindMethod
            recipeList:
              - org.openrewrite.java.search.HasJavaVersion:
                  version: 11
              - org.openrewrite.java.search.FindMethods:
                  methodPattern: java.util.List add(..)
            """, "org.openrewrite.CombinedWithFindMethod"),
          java(
            """
              import java.util.List;
              import java.util.ArrayList;
              class Test {
                  void test() {
                      List<String> list = new ArrayList<>();
                      list.add("1");
                  }
              }
              """,
            """
              /*~~>*/import java.util.List;
              import java.util.ArrayList;
              class Test {
                  void test() {
                      List<String> list = new ArrayList<>();
                      /*~~>*/list.add("1");
                  }
              }
              """, spec -> spec.markers(javaVersion(11)))
        );
    }

    @Test
    void combinedWithFindMethodFirst() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.CombinedWithFindMethod
            recipeList:
              - org.openrewrite.java.search.FindMethods:
                  methodPattern: java.util.List add(..)
              - org.openrewrite.java.search.HasJavaVersion:
                  version: 11

            """, "org.openrewrite.CombinedWithFindMethod"),
          java(
            """
              import java.util.List;
              import java.util.ArrayList;
              class Test {
                  void test() {
                      List<String> list = new ArrayList<>();
                      list.add("1");
                  }
              }
              """,
            """
              /*~~>*/import java.util.List;
              import java.util.ArrayList;
              class Test {
                  void test() {
                      List<String> list = new ArrayList<>();
                      /*~~>*/list.add("1");
                  }
              }
              """, spec -> spec.markers(javaVersion(11)))
        );
    }

}
