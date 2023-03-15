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
package org.openrewrite.java.trait.variable;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.trait.expr.VarAccess;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("UnusedAssignment")
public class VariableTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            private int variableIndex = 0;

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
                return Variable.viewOf(getCursor()).map(v -> {
                    int thisVariableIndex = variableIndex++;
                    doAfterVisit(new JavaIsoVisitor<>() {
                        @Override
                        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext executionContext) {
                            return VarAccess
                              .viewOf(getCursor()).map(var -> {
                                  if (v.getVarAccesses().contains(var)) {
                                      return SearchResult.foundMerging(
                                        identifier,
                                        v.getName() + ": " + thisVariableIndex
                                      );
                                  } else {
                                      return identifier;
                                  }
                              })
                              .orSuccess(identifier);
                        }
                    });

                    return SearchResult.found(variable, v.getName() + ": " + thisVariableIndex);
                }).orSuccess(variable);
            }
        })).cycles(1).expectedCyclesThatMakeChanges(1);
    }

    @Test
    void correctlyLabelsLocalVariables() {
        rewriteRun(
          java(
            """
                class Test {
                    void test(int a) {
                        int i = a;
                        i = 1;
                    }
                }
                """,
            """
                class Test {
                    void test(int /*~~(a: 0)~~>*/a) {
                        int /*~~(i: 1)~~>*/i = /*~~(a: 0)~~>*/a;
                        /*~~(i: 1)~~>*/i = 1;
                    }
                }
                """
          )
        );
    }

    @Test
    void correctlyLabelsLocalFields() {
        rewriteRun(
          java(
            """
                class Test {
                    int i;
                    void test(int a) {
                        i = a;
                    }
                }
                """,
            """
                class Test {
                    int /*~~(i: 0)~~>*/i;
                    void test(int /*~~(a: 1)~~>*/a) {
                        /*~~(i: 0)~~>*/i = /*~~(a: 1)~~>*/a;
                    }
                }
                """
          )
        );
    }

    @Test
    void correctlyLabelsWhenVariableRedefined() {
        rewriteRun(
          java(
            """
                class Test {
                    int i;
                    void test(int i) {
                        this.i = i;
                    }
                }
                """,
            """
                class Test {
                    int /*~~(i: 0)~~>*/i;
                    void test(int /*~~(i: 1)~~>*/i) {
                        this./*~~(i: 0)~~>*/i = /*~~(i: 1)~~>*/i;
                    }
                }
                """
          )
        );
    }



    @Test
    void correctlyLabelsSuperField() {
        rewriteRun(
          java(
            """
              class Parent {
                  int i = 0;
              }
                                                        
              class Child extends Parent {
              
                  void test() {
                      super.i = 1;
                  }
              }
              """,
            """
              class Parent {
                  int /*~~(i: 0)~~>*/i = 0;
              }
                            
              class Child extends Parent {
               
                  void test() {
                      super./*~~(i: 0)~~>*/i = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void correctlyLabelsWhenVariableReDefinedInParent() {
        rewriteRun(
          java(
            """
              class Parent {
                  int i = 0;
              }
                                                        
              class Child extends Parent {
                  int i = 1;              
              
                  void test(int i) {
                      System.out.println(i);
                      System.out.println(this.i);
                      System.out.println(super.i);
                  }
              }
              """,
            """
              class Parent {
                  int /*~~(i: 0)~~>*/i = 0;
              }
                            
              class Child extends Parent {
                  int /*~~(i: 1)~~>*/i = 1;
               
                  void test(int /*~~(i: 2)~~>*/i) {
                      System.out.println(/*~~(i: 2)~~>*/i);
                      System.out.println(this./*~~(i: 1)~~>*/i);
                      System.out.println(super./*~~(i: 0)~~>*/i);
                  }
              }
              """
          )
        );
    }

    @Test
    void correctlyLabelsWhenVariableRedefinedInThrow() {
        rewriteRun(
          java(
            """
              class Test {
                  int i = 1;
                  
                  void test() {
                      try {
                          System.out.println(i);
                      } catch (RuntimeException i) {
                          i.printStackTrace();
                          System.out.println(this.i);
                      }
                  }
              }
              """,
            """
              class Test {
                  int /*~~(i: 0)~~>*/i = 1;
              
                  void test() {
                      try {
                          System.out.println(/*~~(i: 0)~~>*/i);
                      } catch (RuntimeException /*~~(i: 1)~~>*/i) {
                          /*~~(i: 1)~~>*/i.printStackTrace();
                          System.out.println(this./*~~(i: 0)~~>*/i);
                      }
                  }
              }
              """
          )
        );
    }
}
