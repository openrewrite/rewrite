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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"CStyleArrayDeclaration", "InfiniteLoopStatement", "StatementWithEmptyBody", "ForLoopReplaceableByWhile"})
class MultipleVariableDeclarationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MultipleVariableDeclarations());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/812")
    @Test
    void arrayDimensionsBeforeName() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      int[] m, n;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      int[] m;
                      int[] n;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/812")
    @Test
    void arrayDimensionsBeforeName2() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      int[] m, n;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      int[] m;
                      int[] n;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceWithIndividualVariableDeclarations() {
        rewriteRun(
          java(
            """
              class Test {
                  int n = 0, m = 0;
                  int o = 0, p;
                  int s, t = 0;

                  public void method() {
                      for (int i = 0, j = 0; ; ) ;
                  }
              }
              """,
            """
              class Test {
                  int n = 0;
                  int m = 0;
                  int o = 0;
                  int p;
                  int s;
                  int t = 0;

                  public void method() {
                      for (int i = 0, j = 0; ; ) ;
                  }
              }
              """
          )
        );
    }

    @Test
    void arrayTypes() {
        rewriteRun(
          java(
            """
              class Test {
                  public void method() {
                      Integer[] q = {0}, r[] = {{0}};
                  }
              }
              """,
            """
              class Test {
                  public void method() {
                      Integer[] q = {0};
                      Integer[][] r = {{0}};
                  }
              }
              """
          )
        );
    }

    @Test
    void singleLineCommentPreserved() {
        rewriteRun(
          java(
            """
              class Test {
                  // a before-statement example
                  int a = 0, b = 0;
                  int n = 0, m = 0; // an end-of-line example
              }
              """,
            """
              class Test {
                  // a before-statement example
                  int a = 0;
                  int b = 0;
                  int n = 0;
                  int m = 0; // an end-of-line example
              }
              """
          )
        );
    }

    @Test
    void blockCommentPreserved() {
        rewriteRun(
          java(
            """
              class Test {
                  int a = 0;

                  /**
                   * An example minimum and maximum.
                   */
                  private int max = Integer.MAX_VALUE, min = Integer.MIN_VALUE;
              }
              """,
            """
              class Test {
                  int a = 0;

                  /**
                   * An example minimum and maximum.
                   */
                  private int max = Integer.MAX_VALUE;
                  private int min = Integer.MIN_VALUE;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2287")
    @Test
    void removeNewlinesFromMultivariablePrefix() {
        rewriteRun(
          java(
            """
              class A {
                  void test() {
                      int a = 1,
                          b = 2;
                  }
              }
              """,
            """
              class A {
                  void test() {
                      int a = 1;
                      int b = 2;
                  }
              }
              """
          )
        );
    }
}
