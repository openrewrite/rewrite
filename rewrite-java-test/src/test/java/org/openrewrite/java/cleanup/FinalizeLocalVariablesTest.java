/*
 * Copyright 2020 the original author or authors.
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

@SuppressWarnings("ALL")
class FinalizeLocalVariablesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FinalizeLocalVariables());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1478")
    @Test
    void initializedInWhileLoop() {
        rewriteRun(
          java(
            """
              import java.io.BufferedReader;
              class T {
                  public void doSomething(StringBuilder sb, BufferedReader br) {
                      String line;
                      try {
                          while ((line = br.readLine()) != null) {
                              sb.append(line);
                          }
                      } catch (Exception e) {
                          error("Exception", e);
                      }
                  }
                  private static void error(String s, Exception e) {
                  
                  }
              }
              """
          )
        );
    }

    @Test
    void localVariablesAreMadeFinal() {
        rewriteRun(
          java(
            """
              class A {
                  public void test() {
                      int n = 1;
                      for(int i = 0; i < n; i++) {
                      }
                  }
              }
              """,
            """
              class A {
                  public void test() {
                      final int n = 1;
                      for(int i = 0; i < n; i++) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyReassignedLocalVariables() {
        rewriteRun(
          java(
            """
              class A {
                  public void test() {
                      int a = 0;
                      int b = 0;
                      int c = 10;
                      for(int i = 0; i < c; i++) {
                          a = i + c;
                          b++;
                      }
                  }
              }
              """,
            """
              class A {
                  public void test() {
                      int a = 0;
                      int b = 0;
                      final int c = 10;
                      for(int i = 0; i < c; i++) {
                          a = i + c;
                          b++;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void localVariableScopeAwareness() {
        rewriteRun(
          java(
            """
                  class Test {
                      public static void testA() {
                          int a = 0;
                          a = 1;
                      }

                      public static void testB() {
                          int a = 0;
                      }
                  }
              """,
            """
                  class Test {
                      public static void testA() {
                          int a = 0;
                          a = 1;
                      }

                      public static void testB() {
                          final int a = 0;
                      }
                  }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/549")
    void catchBlocksIgnored() {
        rewriteRun(
          java(
            """
                  import java.io.IOException;
                  
                  class Test {
                      static {
                          try {
                              throw new IOException();
                          } catch (RuntimeException | IOException e) {
                              System.out.println("oops");
                          }
                      }
                  }
              """
          )
        );
    }

    // aka "non-static-fields"
    @Test
    void instanceVariablesIgnored() {
        rewriteRun(
          java(
            """
              class Test {
                  int instanceVariableUninitialized;
                  int instanceVariableInitialized = 0;
              }
              """
          )
        );
    }

    // aka "static fields"
    @Test
    void classVariablesIgnored() {
        rewriteRun(
          java(
            """
              class Test {
                  static int classVariableInitialized = 0;
              }
              """
          )
        );
    }

    @Test
    void staticInitializer() {
        rewriteRun(
          java(
            """
                  class Test {
                      static {
                          int n = 1;
                          for(int i = 0; i < n; i++) {
                          }
                      }
                  }
              """,
            """
              class Test {
                  static {
                      final int n = 1;
                      for(int i = 0; i < n; i++) {
                      }
                  }
              }
            """
          )
        );
    }

    @Test
    void methodParameterVariablesIgnored() {
        rewriteRun(
          java(
            """
                  class Test {
                      private static int testMath(int x, int y) {
                          y = y + y;
                          return x + y;
                      }

                      public static void main(String[] args) {
                      }
                  }
              """
          )
        );
    }

    @Test
    void lambdaVariablesIgnored() {
        rewriteRun(
          java(
            """
                  import java.util.stream.Stream;
                  class A {
                      public boolean hasFoo(Stream<String> input) {
                          return input.anyMatch(word -> word.equalsIgnoreCase("foo"));
                      }
                  }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1357")
    @Test
    void forLoopVariablesIgnored() {
        rewriteRun(
          java(
            """
                  import java.util.concurrent.FutureTask;
                  
                  class A {
                      void f() {
                          for(FutureTask<?> future; (future = new FutureTask<>(() -> "hello world")) != null;) { }
                      }
                  }
              """
          )
        );
    }

    @Test
    void nonModifyingUnaryOperatorAwareness() {
        rewriteRun(
          java(
            """
                  class Test {
                      void test() {
                          int i = 1;
                          int j = -i;
                          int k = +j;
                          int l = ~k;
                      }
                  }
              """,
            """
                  class Test {
                      void test() {
                          final int i = 1;
                          final int j = -i;
                          final int k = +j;
                          final int l = ~k;
                      }
                  }
              """
          )
        );
    }

    @Test
    void localVariableInInitializerBlockMadeFinal() {
        rewriteRun(
          java(
            """
              class Person {
                  {
                      int n = 10;
                      name = "N1";
                      age = n;
                  }

                  private String name;
                  private int age;
              }
              """,
            """
              class Person {
                  {
                      final int n = 10;
                      name = "N1";
                      age = n;
                  }

                  private String name;
                  private int age;
              }
              """
          )
        );
    }
}
