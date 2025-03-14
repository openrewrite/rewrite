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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindLiteralsTest implements RewriteTest {

    @DocumentExample
    @Test
    void string() {
        rewriteRun(
          spec -> spec.recipe(new FindLiterals("Hello.*")),
          java(
            """
              class Test {
                  String s = "Hello Jonathan";
              }
              """,
            """
              class Test {
                  String s = /*~~>*/"Hello Jonathan";
              }
              """
          )
        );
    }

    @Test
    void textBlock() {
        rewriteRun(
          spec -> spec.recipe(new FindLiterals("(?s)Hello.*")),
          java(
            """
              class Test {
                  String s = \"""
                      Hello Jonathan
                      \""";
              }
              """,
            """
              class Test {
                  String s = /*~~>*/\"""
                      Hello Jonathan
                      \""";
              }
              """
          )
        );
    }

    @Test
    void number() {
        rewriteRun(
          spec -> spec.recipe(new FindLiterals("1000")),
          java(
            """
              class Test {
                  int i1 = 1000;
                  int i2 = 1_000;
              }
              """,
            """
              class Test {
                  int i1 = /*~~>*/1000;
                  int i2 = /*~~>*/1_000;
              }
              """
          )
        );
    }
}
