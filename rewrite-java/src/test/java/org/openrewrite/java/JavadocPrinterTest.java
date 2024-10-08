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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavadocPrinterTest implements RewriteTest {

    @DocumentExample
    @Test
    void findInJavadoc() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                  return space.withComments(ListUtils.map(space.getComments(), comment -> {
                      if (comment instanceof Javadoc.DocComment) {
                          return comment.withMarkers(comment.getMarkers().
                            computeByType(new SearchResult(randomId(), null), (s1, s2) -> s1 == null ? s2 : s1));
                      }
                      return comment;
                  }));
              }
          })),
          java(
            """
              /** this is a doc comment*/
              class Test {
              }
              """,
            """
              /*~~>*//** this is a doc comment*/
              class Test {
              }
              """
          )
        );
    }
}
