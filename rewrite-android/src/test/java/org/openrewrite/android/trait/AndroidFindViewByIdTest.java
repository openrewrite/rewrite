/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.android.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class AndroidFindViewByIdTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new AndroidFindViewById.Matcher().asVisitor(
                (trait, ctx) -> SearchResult.found(trait.getTree(),
                        trait.getResourceIdExpression().printTrimmed(trait.getCursor()))
        )));
    }

    @Test
    void javaFindViewById() {
        rewriteRun(
          java(
            """
              class Test {
                  Object findViewById(int id) { return null; }
                  Object f() { return findViewById(42); }
              }
              """,
            """
              class Test {
                  Object findViewById(int id) { return null; }
                  Object f() { return /*~~(42)~~>*/findViewById(42); }
              }
              """
          )
        );
    }

    @Test
    void kotlinFindViewById() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun findViewById(id: Int): Any? = null
                  fun f(): Any? = findViewById(42)
              }
              """,
            """
              class Test {
                  fun findViewById(id: Int): Any? = null
                  fun f(): Any? = /*~~(42)~~>*/findViewById(42)
              }
              """
          )
        );
    }

    @Test
    void doesNotMatchUnrelatedMethod() {
        rewriteRun(
          java(
            """
              class Test {
                  int findOtherById(int id) { return id; }
                  int f() { return findOtherById(42); }
              }
              """
          )
        );
    }

    @Test
    void doesNotMatchTwoArgFindViewById() {
        rewriteRun(
          java(
            """
              class Test {
                  Object findViewById(int id, String tag) { return null; }
                  Object f() { return findViewById(42, "x"); }
              }
              """
          )
        );
    }
}
