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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateNamedTest implements RewriteTest {

    @Test
    void replaceSingleStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitAssert(J.Assert anAssert, ExecutionContext p) {
                  return JavaTemplate.builder(
                      """
                        if(#{name:any(int)} != #{}) {
                          #{name}++;
                        }"""
                    )
                    .build()
                    .apply(getCursor(), anAssert.getCoordinates().replace(),
                      ((J.Binary) anAssert.getCondition()).getLeft(), "1");
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      if (n != 1) {
                          n++;
                      }
                  }
              }
              """
          )
        );
    }
}
