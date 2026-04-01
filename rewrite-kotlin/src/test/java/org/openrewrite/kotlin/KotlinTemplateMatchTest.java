/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class KotlinTemplateMatchTest implements RewriteTest {

    @DocumentExample
    @Test
    void matchBinary() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new KotlinVisitor<>() {
                    @Override
                    public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                        return KotlinTemplate.matches("1 == #{any(int)}", getCursor()) ?
                                SearchResult.found(binary) : super.visitBinary(binary, ctx);
                    }
                })),
                kotlin(
                        """
                          class Test {
                              val b1 = 1 == 2
                              val b2 = 1 == 3

                              val b3 = 2 == 1
                              val b4 = 2 == 2 + 3 /* no match because type is `kotlin.Int` */
                          }
                          """,
                        """
                          class Test {
                              val b1 = /*~~>*/1 == 2
                              val b2 = /*~~>*/1 == 3

                              val b3 = 2 == 1
                              val b4 = 2 == 2 + 3 /* no match because type is `kotlin.Int` */
                          }
                          """
                ));
    }
}
