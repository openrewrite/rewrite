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
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Recipe;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ALL")
class ReassignableTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(markReassignables());
    }

    @DocumentExample
    @Test
    void nonFinalLocalIsReassignable() {
        rewriteRun(
          java(
            """
              class T {
                  void run() {
                      String local = "";
                      local = "x";
                  }
              }
              """,
            """
              class T {
                  void run() {
                      String local = "";
                      /*~~>*/local = "x";
                  }
              }
              """
          )
        );
    }

    @Test
    void finalLocalIsNotReassignable() {
        rewriteRun(
          java(
            """
              class T {
                  void run() {
                      final String local = "";
                      System.out.println(local);
                  }
              }
              """
          )
        );
    }

    @Test
    void methodParameterIsNotReassignable() {
        rewriteRun(
          java(
            """
              class T {
                  void run(String param) {
                      param = "x";
                  }
              }
              """
          )
        );
    }

    @Test
    void nonFinalThisFieldIsReassignable() {
        rewriteRun(
          java(
            """
              class T {
                  String field = "";
                  void run() {
                      this.field = "x";
                  }
              }
              """,
            """
              class T {
                  String field = "";
                  void run() {
                      /*~~>*/this.field = "x";
                  }
              }
              """
          )
        );
    }

    @Test
    void finalThisFieldIsNotReassignable() {
        rewriteRun(
          java(
            """
              class T {
                  final String field = "";
                  void run() {
                      System.out.println(this.field);
                  }
              }
              """
          )
        );
    }

    Recipe markReassignables() {
        return toRecipe(() -> new Reassignable.Matcher().asVisitor(r -> SearchResult.found(r.getTree())));
    }
}
