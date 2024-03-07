/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class UsesFieldTest implements RewriteTest {

    @Test
    void staticFieldAccess() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesField<>("java.util.Collections", "EMPTY_LIST"))),
          java(
            """
              import java.util.Collections;
              
              class T {
                  Object o = Collections.EMPTY_LIST;
              }
              """,
            """
              /*~~>*/import java.util.Collections;
              
              class T {
                  Object o = Collections.EMPTY_LIST;
              }
              """
          )
        );
    }

    @Test
    void staticImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesField<>("java.util.Collections", "EMPTY_LIST"))),
          java(
            """
              import static java.util.Collections.EMPTY_LIST;
              
              class T {
                  Object o = EMPTY_LIST;
              }
              """,
            """
              /*~~>*/import static java.util.Collections.EMPTY_LIST;
              
              class T {
                  Object o = EMPTY_LIST;
              }
              """
          )
        );
    }

    @Test
    void noImportStaticField() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new UsesField<>("java.util.Collections", "EMPTY_LIST"))),
          java(
            """
              class T {
                  Object o = java.util.Collections.EMPTY_LIST;
              }
              """,
            """
              /*~~>*/class T {
                  Object o = java.util.Collections.EMPTY_LIST;
              }
              """
          )
        );
    }
}
