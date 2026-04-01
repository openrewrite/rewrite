/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.kotlin.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class RemoveTrailingWhitespaceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new RemoveTrailingWhitespaceVisitor<>()));
    }

    @DocumentExample
    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    @Test
    void removeTrailing() {
        rewriteRun(
          kotlin(
            """
              class Test {

                  fun method(t: Test) {\s\s
                  }\s\s
              }\s\s
              """,
            """
              class Test {

                  fun method(t: Test) {
                  }
              }
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1053")
    @Test
    void doNotRemoveTrailingComma() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  val integerArray: Array<Int> = arrayOf(
                      1,
                      2,
                      4
                  )
              }
              """
          )
        );
    }
}
