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
package org.openrewrite.java.trait.expr;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ALL")
public class VarAccessCreationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J preVisit(J tree, ExecutionContext executionContext) {
                return VarAccess.viewOf(getCursor())
                  .map(var -> {
                      assertNotNull(var.getVariable(), "VarAccess.getVariable() is null");
                      assertTrue(var.getVariable().getVarAccesses().contains(var), "VarAccess.getVariable().getVarAccesses() does not contain this VarAccess");
                      return SearchResult.foundMerging(tree, var.getName());
                  })
                  .orSuccess(tree);
            }
        })).cycles(1).expectedCyclesThatMakeChanges(1);
    }

    @Test
    void searchResultsForVarAccessUnary() {
        rewriteRun(
          java("""
              class Test {
                  void test() {
                      int i = 0;
                      i++;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      int i = 0;
                      /*~~(i)~~>*/i++;
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForBinaryExpression() {
        rewriteRun(
          java("""
              class Test {
                  int test(int a, int b) {
                      return a + b;
                  }
              }
              """,
            """
              class Test {
                  int test(int a, int b) {
                      return /*~~(a)~~>*/a + /*~~(b)~~>*/b;
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForSubjectMethodInvocation() {
        rewriteRun(
          java("""
              import java.util.function.Supplier;
              class Test {
                   void test(Supplier<Object> s) {
                      s.get();
                   }               
              }
              """,
            """
              import java.util.function.Supplier;
              class Test {
                   void test(Supplier<Object> s) {
                      /*~~(s)~~>*/s.get();
                   }               
              }
              """
          )
        );
    }

    @Test
    void searchResultsForNewClass() {
        rewriteRun(
          java("""
              class Test {
                  void test(Object o) {
                      Another a = new Another(o);
                      Another.Inner i = a.new Inner(o);
                  }
              }
              class Another {
                  Another(Object o) {}
                  
                  class Inner {
                      Inner(Object o) {}
                  }
                            
              }
              """,
            """
              class Test {
                  void test(Object o) {
                      Another a = new Another(/*~~(o)~~>*/o);
                      Another.Inner i = /*~~(a)~~>*/a.new Inner(/*~~(o)~~>*/o);
                  }
              }
              class Another {
                  Another(Object o) {}
                  
                  class Inner {
                      Inner(Object o) {}
                  }
                            
              }
              """
          )
        );
    }

    @Test
    void searchResultsForArgumentsMethodInvocation() {
        rewriteRun(
          java("""
              import java.util.function.Consumer;
              class Test {
                  void test(Consumer<Object> c, Object value) {
                      c.accept(value);
                  }               
              }
              """,
            """
                  import java.util.function.Consumer;
                  class Test {
                      void test(Consumer<Object> c, Object value) {
                          /*~~(c)~~>*/c.accept(/*~~(value)~~>*/value);
                      }               
                  }
              """)
        );
    }

    @Test
    void searchResultsForParenthesesWrappedMethodInvocation() {
        rewriteRun(
          java("""
              import java.util.function.Consumer;
              class Test {
                  void test(Consumer<Object> c, Object value) {
                      (c).accept((value));
                  }               
              }
              """,
            """
                  import java.util.function.Consumer;
                  class Test {
                      void test(Consumer<Object> c, Object value) {
                          (/*~~(c)~~>*/c).accept((/*~~(value)~~>*/value));
                      }               
                  }
              """)
        );
    }

    @Test
    void searchResultsForTeraryExpression() {
        rewriteRun(
          java("""
              class Test {
                  void test(Object any, Object other) {
                      Object o = any == null ? other : any;
                  }
              }
              """,
            """
              class Test {
                  void test(Object any, Object other) {
                      Object o = /*~~(any)~~>*/any == null ? /*~~(other)~~>*/other : /*~~(any)~~>*/any;
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForNamedVariableCreation() {
        rewriteRun(
          java("""
              class Test {
                  void test(Object any) {
                      Object o = any;
                  }
              }
              """,
            """
              class Test {
                  void test(Object any) {
                      Object o = /*~~(any)~~>*/any;
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForVariableReassignment() {
        rewriteRun(
          java("""
              class Test {
                  void test(Object any) {
                      Object o = new Object();
                      o = any;
                  }
              }
              """,
            """
              class Test {
                  void test(Object any) {
                      Object o = new Object();
                      /*~~(o)~~>*/o = /*~~(any)~~>*/any;
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForCast() {
        rewriteRun(
          java("""
              class Test {
                  void test(Object any) {
                      Integer o = (Integer) any;
                  }
              }
              """,
            """
              class Test {
                  void test(Object any) {
                      Integer o = (Integer) /*~~(any)~~>*/any;
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultForForEachLoop() {
        rewriteRun(
          java("""
              import java.util.List;
              class Test {
                  void test(List<Object> any) {
                      for(Object o : any) {
                          System.out.println(o);
                      }
                  }
              }
              """,
            """
              import java.util.List;
              class Test {
                  void test(List<Object> any) {
                      for(Object o : /*~~(any)~~>*/any) {
                          System./*~~(out)~~>*/out.println(/*~~(o)~~>*/o);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForForLoop() {
        rewriteRun(
          java("""
              class Test {
                  void test(int initial, boolean condition, Object any) {
                      for(int i = initial; condition; i++) {
                          System.out.println(any);
                      }
                  }
              }
              """,
            """
              class Test {
                  void test(int initial, boolean condition, Object any) {
                      for(int i = /*~~(initial)~~>*/initial; /*~~(condition)~~>*/condition; /*~~(i)~~>*/i++) {
                          System./*~~(out)~~>*/out.println(/*~~(any)~~>*/any);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForControlParentheses() {
        rewriteRun(
          java("""
              class Test {
                  void test(boolean condition, Object any) {
                      while(condition) {
                          System.out.println(any);
                      }
                  }
              }
              """,
            """
              class Test {
                  void test(boolean condition, Object any) {
                      while(/*~~(condition)~~>*/condition) {
                          System./*~~(out)~~>*/out.println(/*~~(any)~~>*/any);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForNewArray() {
        rewriteRun(
          java("""
              class Test {
                  void test(Object first, Object second, int size) {
                      Object[] o = new Object[] { first, second };
                      Object[] o2 = new Object[size];
                  }
              }
              """,
            """
              class Test {
                  void test(Object first, Object second, int size) {
                      Object[] o = new Object[] { /*~~(first)~~>*/first, /*~~(second)~~>*/second };
                      Object[] o2 = new Object[/*~~(size)~~>*/size];
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForArrayAccess() {
        rewriteRun(
          java("""
              class Test {
                  void test(Object[] any, int index) {
                      Object o = any[index];
                  }
              }
              """,
            """
              class Test {
                  void test(Object[] any, int index) {
                      Object o = /*~~(any)~~>*/any[/*~~(index)~~>*/index];
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForAnnotations() {
        rewriteRun(
          java("""
              class Test {
                  private static final String FOO = "foo";
                  @SuppressWarnings(FOO)
                  void test() {
                  }
              }
              """,
            """
              class Test {
                  private static final String FOO = "foo";
                  @SuppressWarnings(/*~~(FOO)~~>*/FOO)
                  void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForAnnotationsExlicitArgument() {
        rewriteRun(
          java("""
              class Test {
                  private static final String FOO = "foo";
                  @SuppressWarnings(value = FOO)
                  void test() {
                  }
              }
              """,
            """
              class Test {
                  private static final String FOO = "foo";
                  @SuppressWarnings(value = /*~~(FOO)~~>*/FOO)
                  void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void searchResultsForQualifiedClassLocalVariableAccess() {
        rewriteRun(
          java(
            """
              class Test {
                  Object field;
                  void test() {
                      Object o = this.field;
                  }
              }
              """,
            """
                class Test {
                    Object field;
                    void test() {
                        Object o = this./*~~(field)~~>*/field;
                    }
                }
                """
          )
        );
    }

    @Test
    void searchResultsDoNotInclude() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(0),
          java("""
            package org.openrewrite;
            import java.util.function.Supplier;
            class Test {
                void test(Object any) {
                    new Object();
                    String.format("foo");
                }
            }
            """
          )
        );
    }

}
