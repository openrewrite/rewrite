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
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"ClassInitializerMayBeStatic", "ResultOfMethodCallIgnored"})
class UsesAllMethodsTest implements RewriteTest {

    @Test
    void usesBothMethods() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new UsesAllMethods<>(
            new MethodMatcher("java.util.Collections emptyList()"),
            new MethodMatcher("java.util.Collections emptySet()")
          ))),
          java(
            """
              import java.util.Collections;
              class Test {
                  {
                      Collections.emptyList();
                      Collections.emptySet();
                  }
              }
              """,
            """
              /*~~>*/import java.util.Collections;
              class Test {
                  {
                      Collections.emptyList();
                      Collections.emptySet();
                  }
              }
              """
          )
        );
    }

    @Test
    void usesNeitherMethods() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new UsesAllMethods<>(
            new MethodMatcher("java.util.Collections emptyList()"),
            new MethodMatcher("java.util.Collections emptySet()")
          ))),
          java(
            """
              import java.util.Collections;
              class Test {
              }
              """
          )
        );
    }

    @Test
    void usesOneMethod() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new UsesAllMethods<>(
            new MethodMatcher("java.util.Collections emptyList()"),
            new MethodMatcher("java.util.Collections emptySet()")
          ))),
          java(
            """
              import java.util.Collections;
              class Test {
                  {
                      Collections.emptyList();
                  }
              }
              """
          )
        );
    }

    @Test
    void usesBothExitEarlyMethods() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new UsesAllMethods<>(
            new MethodMatcher("java.util.Collections emptyList()"),
            new MethodMatcher("java.util.Collections emptySet()")
          ))),
          java(
            """
              import java.util.Collections;
              class Test {
                  {
                      Collections.emptyList();
                      Collections.emptySet();
                      System.out.println("Hello");
                  }
              }
              """,
            """
              /*~~>*/import java.util.Collections;
              class Test {
                  {
                      Collections.emptyList();
                      Collections.emptySet();
                      System.out.println("Hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void matchesMultipleMethods() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new UsesAllMethods<>(
            new MethodMatcher("java.util.Collections emptyList()"),
            new MethodMatcher("java.util.Collections *()"),
            new MethodMatcher("java.util.Collections emptySet()")
          ))),
          java(
            """
              import java.util.Collections;
              class Test {
                  {
                      Collections.emptyList();
                      Collections.emptySet();
                  }
              }
              """,
            """
              /*~~>*/import java.util.Collections;
              class Test {
                  {
                      Collections.emptyList();
                      Collections.emptySet();
                  }
              }
              """
          )
        );
    }
}
