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
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.trait.Traits.methodAccess;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ALL")
class MethodAccessTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(markMethodAccesses(methodAccess(
          new MethodMatcher("java.util.List add(..)", true))));
    }

    @Test
    void methodAccesses() {
        rewriteRun(
          java(
            """
              import java.util.List;
              class Test {
                 void test(List<String> names) {
                   names.add("Alice");
                 }
              }
              """,
            """
              import java.util.List;
              class Test {
                 void test(List<String> names) {
                   /*~~>*/names.add("Alice");
                 }
              }
              """
          )
        );
    }

    Recipe markMethodAccesses(MethodAccess.Matcher matcher) {
        return toRecipe(() -> matcher.asVisitor(ma -> SearchResult.found(ma.getTree())));
    }
}
