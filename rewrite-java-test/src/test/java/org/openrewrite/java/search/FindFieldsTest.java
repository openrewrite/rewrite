/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindFieldsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindFields("java.nio.charset.StandardCharsets", null,"UTF_8"));
    }

    @Test
    void fieldMatch() {
        rewriteRun(
          spec -> spec.recipe(new FindFields("java.nio..*", true, "*")),
          java(
            """
              class Test {
                  Object o = java.nio.charset.StandardCharsets.UTF_8;
              }
              """,
            """
              class Test {
                  Object o = /*~~>*/java.nio.charset.StandardCharsets.UTF_8;
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void findFullyQualifiedFields() {
        rewriteRun(
          java(
            """
              class Test {
                  Object o = java.nio.charset.StandardCharsets.UTF_8;
              }
              """,
            """
              class Test {
                  Object o = /*~~>*/java.nio.charset.StandardCharsets.UTF_8;
              }
              """
          )
        );
    }

    @Test
    void findImported() {
        rewriteRun(
          java(
            """
              import java.nio.charset.StandardCharsets;
              class Test {
                  Object o = StandardCharsets.UTF_8;
              }
              """,
            """
              import java.nio.charset.StandardCharsets;
              class Test {
                  Object o = /*~~>*/StandardCharsets.UTF_8;
              }
              """
          )
        );
    }

    @Test
    void findStaticallyImported() {
        rewriteRun(
          java(
            """
              import static java.nio.charset.StandardCharsets.*;
              class Test {
                  Object o = UTF_8;
              }
              """,
            """
              import static java.nio.charset.StandardCharsets.*;
              class Test {
                  Object o = /*~~>*/UTF_8;
              }
              """
          )
        );
    }
}
