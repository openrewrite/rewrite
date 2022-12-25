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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class LineCounterTest implements RewriteTest {

    @SuppressWarnings("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    void countLines() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final LineCounter lineCount = new LineCounter();

              @Override
              public Space visitSpace(Space space, Space.Location loc, ExecutionContext p) {
                  lineCount.count(space);
                  return super.visitSpace(space, loc, p);
              }

              @Override
              public J preVisit(J tree, ExecutionContext p) {
                  if (lineCount.getLine() == 3) {
                      return SearchResult.found(tree);
                  }
                  return super.preVisit(tree, p);
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      int n = 0;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      /*~~>*/int /*~~>*//*~~>*/n = /*~~>*/0;
                  }
              }
              """
          )
        );
    }
}
