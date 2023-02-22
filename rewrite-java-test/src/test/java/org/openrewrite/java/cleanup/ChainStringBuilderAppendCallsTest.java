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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChainStringBuilderAppendCallsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChainStringBuilderAppendCalls());
    }

    @Test
    void objectsConcatenation() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      String op = "+";
                      sb.append("A" + op + "B");
                      sb.append(1 + op + 2);
                  }
              }
              """,
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      String op = "+";
                      sb.append("A").append(op).append("B");
                      sb.append(1).append(op).append(2);
                  }
              }
              """
          )
        );
    }

    @Test
    void literalConcatenationIgnored() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("A" + "B" + "C");
                  }
              }
              """
          )
        );
    }

    @Test
    void groupedObjectsConcatenation() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      String op = "+";
                      sb.append("A" + "B" + "C" + op + "D" + "E");
                  }
              }
              """,
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      String op = "+";
                      sb.append("A" + "B" + "C").append(op).append("D" + "E");
                  }
              }
              """
          )
        );
    }

    @Test
    void appendMethods() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      sb.append(str1() + str2() + str3());
                  }

                  String str1() { return "A"; }
                  String str2() { return "B"; }
                  String str3() { return "C"; }
              }
              """,
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      sb.append(str1()).append(str2()).append(str3());
                  }

                  String str1() { return "A"; }
                  String str2() { return "B"; }
                  String str3() { return "C"; }
              }
              """
          )
        );
    }

    @Test
    void ChainedAppendWithConstructor() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder().append("A" + operator() + "B");
                  }

                  String operator() { return "+"; }
              }
              """,
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder().append("A").append(operator()).append("B");
                  }

                  String operator() { return "+"; }
              }
              """
          )
        );
    }

    @Test
    void methodArgumentIgnored() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      String op = "+";
                      print(new StringBuilder().append("A" + op + "C").toString());
                  }

                  void print(String str) {
                  }
              }
              """
          )
        );
    }
}
