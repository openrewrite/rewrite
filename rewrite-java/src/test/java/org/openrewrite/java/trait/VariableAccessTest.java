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
package org.openrewrite.java.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ALL")
class VariableAccessTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(markVariableAccesses(new VariableAccess.Matcher()));
    }

    @Test
    void variableAccesses() {
        rewriteRun(
          java(
            """
              class Test {
                 int test(int p) {
                   int a = p;
                   p++;
                   p = p+1;
                   return a+1;
                 }
              }
              """,
            """
              class Test {
                 int test(int p) {
                   int a = /*~~(read p)~~>*/p;
                   /*~~(write p)~~>*/p++;
                   /*~~(write p)~~>*/p = /*~~(read p)~~>*/p+1;
                   return /*~~(read a)~~>*/a+1;
                 }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(new VariableAccess.Matcher().lower(cu)).hasSize(5);
            })
          )
        );
    }

    Recipe markVariableAccesses(VariableAccess.Matcher matcher) {
        return toRecipe(() -> matcher.asVisitor(va -> {
            String op = va.isReadAccess() ? "read" : "write";
            if (!(va.isReadAccess() ^ va.isWriteAccess())) {
                op = "neither";
            }
            return SearchResult.found(va.getTree(),
              op + " " + va.getTree().getSimpleName());
        }));
    }
}
