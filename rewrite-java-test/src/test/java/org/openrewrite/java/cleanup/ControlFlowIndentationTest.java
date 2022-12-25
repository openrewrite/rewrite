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

@SuppressWarnings("ConstantConditions")
class ControlFlowIndentationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ControlFlowIndentation());
    }

    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    @Issue("https://github.com/openrewrite/rewrite/issues/2277")
    @Test
    void removesIndentationFromStatementAroundIf() {
        rewriteRun(
          java(
            """
              class A {
                  void test() {
                          foo(); // This should be left alone because it does not come after control flow
                      if(true)
                      foo();
                          foo();
                  }
                  
                  static void foo() {
                  // There's no control flow in this method body, so its indentation should remain untouched
                              int a = 0;
                          }
              }
              """,
            """
              class A {
                  void test() {
                          foo(); // This should be left alone because it does not come after control flow
                      if(true)
                          foo();
                      foo();
                  }
                  
                  static void foo() {
                  // There's no control flow in this method body, so its indentation should remain untouched
                              int a = 0;
                          }
              }
              """
          )
        );
    }

    @SuppressWarnings("DuplicateCondition")
    @Test
    void leavesIndentationAloneWhenBlocksAreExplicit() {
        rewriteRun(
          java(
            """
              class A {
                  void test() {
                      if(true) {
                              foo();
                      } else if(true) {
                              foo();
                      } else {
                              foo();
                      }
                              foo();
                  }
                  
                  static void foo(){}
              }
              """
          )
        );
    }

    @SuppressWarnings({"SuspiciousIndentAfterControlStatement", "IfStatementWithIdenticalBranches"})
    @Test
    void removesIndentationFromStatementAfterIfElse() {
        rewriteRun(
          java(
            """
              class A {
                  void test() {
                      if(true) {
                          foo();
                      } else
                          foo();
                          foo();
                  }
                  
                  static void foo(){}
              }
              """,
            """
              class A {
                  void test() {
                      if(true) {
                          foo();
                      } else
                          foo();
                      foo();
                  }
                  
                  static void foo(){}
              }
              """
          )
        );
    }

    @Test
    void elseIf() {
        rewriteRun(
          java(
            """
              class A {
                  void test() {
                      if(true){
                          foo();
                      } else if(false)
                      foo();
                      else {
                          foo();
                      }
                  }
                  static void foo(){}
              }
              """,
            """
              class A {
                  void test() {
                      if(true){
                          foo();
                      } else if(false)
                          foo();
                      else {
                          foo();
                      }
                  }
                  static void foo(){}
              }
              """
          )
        );
    }

    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    @Test
    void removesIndentationFromStatementAfterLoop() {
        rewriteRun(
          java(
            """
              class A {
                  void test() {
                      while(false)
                          foo();
                          foo();
                  }
                  
                  static void foo(){}
              }
              """,
            """
              class A {
                  void test() {
                      while(false)
                          foo();
                      foo();
                  }
                  
                  static void foo(){}
              }
              """
          )
        );
    }
}
