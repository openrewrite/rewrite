/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.MinimumJava25;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@MinimumJava25
class UnnamedVariableTest implements RewriteTest {

    // Based on https://docs.oracle.com/en/java/javase/21/language/unnamed-variables-and-patterns.html

    @Test
    void unnamedVariableInForLoop() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class Test {
                  void test(List<String> list) {
                      int total = 0;
                      for (var _ : list) {
                          total++;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void unnamedVariableInCatchBlock() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      try {
                          int x = Integer.parseInt("123");
                      } catch (NumberFormatException _) {
                          System.out.println("Bad number format");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void unnamedVariableInTryWithResources() {
        rewriteRun(
          java(
            """
              import java.io.FileReader;

              class Test {
                  void test(String path) {
                      try (var _ = new FileReader(path)) {
                          System.out.println("Critical section");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void unnamedVariableInLambda() {
        rewriteRun(
          java(
            """
              import java.util.function.BiFunction;

              class Test {
                  void test() {
                      BiFunction<Integer, Integer, Integer> func = (x, _) -> x * 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleUnnamedVariablesInLambda() {
        rewriteRun(
          java(
            """
              interface TriConsumer<T, U, V> {
                  void accept(T t, U u, V v);
              }

              class Test {
                  void test() {
                      TriConsumer<String, String, String> consumer = (_, _, third) ->
                          System.out.println(third);
                  }
              }
              """,
            after -> after.afterRecipe(cu -> {
                J.MethodDeclaration md = (J.MethodDeclaration) cu.getClasses().get(1).getBody().getStatements().getFirst();
                J.VariableDeclarations vd = (J.VariableDeclarations) md.getBody().getStatements().getFirst();
                J.Lambda lambda = (J.Lambda) vd.getVariables().getFirst().getInitializer();
                List<J> lambdaParams = lambda.getParameters().getParameters();
                assertThat(lambdaParams.getFirst()).isNotEqualTo(lambdaParams.get(1));
                // They are semantically equal but not registered by the compiler as
                // a variable in scope, so they cannot be read or written to.
                assertThat(SemanticallyEqual.areEqual(lambdaParams.getFirst(), lambdaParams.get(1))).isTrue();
            })
          )
        );
    }

    @Test
    void unnamedPatternInInstanceof() {
        rewriteRun(
          java(
            """
              class Test {
                  void test(Object obj) {
                      if (obj instanceof String _) {
                          System.out.println("It's a String");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void unnamedPatternInSwitch() {
        rewriteRun(
          java(
            """
              class Test {
                  String test(Object obj) {
                      return switch (obj) {
                          case Integer _ -> "It's an Integer";
                          case Long _ -> "It's a Long";
                          case Double _ -> "It's a Double";
                          default -> "It's something else";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void unnamedPatternWithGuard() {
        rewriteRun(
          java(
            """
              class Test {
                  String test(Object obj) {
                      return switch (obj) {
                          case String _ when obj.toString().length() > 5 -> "Long string";
                          case String _ -> "Short string";
                          default -> "Not a string";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void unnamedVariableInRecordPattern() {
        rewriteRun(
          java(
            """
              record Point(int x, int y) {}

              class Test {
                  void test(Object obj) {
                      if (obj instanceof Point(int x, _)) {
                          System.out.println("x = " + x);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedUnnamedPatterns() {
        rewriteRun(
          java(
            """
              record Box<T>(T content) {}

              class Test {
                  void test(Object obj) {
                      if (obj instanceof Box(Box(_))) {
                          System.out.println("Nested box");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void unnamedVariableInLocalDeclaration() {
        rewriteRun(
          java(
            """
              import java.util.Queue;
              import java.util.LinkedList;

              class Test {
                  void test() {
                      Queue<String> queue = new LinkedList<>();
                      queue.offer("first");
                      queue.offer("second");

                      var _ = queue.poll();
                      var second = queue.poll();
                      System.out.println(second);
                  }
              }
              """
          )
        );
    }

    @Test
    void unnamedVariableWithSideEffects() {
        rewriteRun(
          java(
            """
              class Test {
                  int counter = 0;

                  int increment() {
                      return ++counter;
                  }

                  void test() {
                      var _ = increment();
                      var _ = increment();
                      System.out.println("Counter incremented twice");
                  }
              }
              """
          )
        );
    }

    @Test
    void unnamedPatternInSwitchWithMultipleLabels() {
        rewriteRun(
          java(
            """
              sealed interface Shape permits Circle, Rectangle, Square {}
              record Circle(double radius) implements Shape {}
              record Rectangle(double width, double height) implements Shape {}
              record Square(double side) implements Shape {}

              class Test {
                  String test(Shape shape) {
                      return switch (shape) {
                          case Circle _, Square _ -> "Round or square";
                          case Rectangle _ -> "Rectangular";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void unnamedVariableInMethodParameter() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;

              class Test {
                  void process(Consumer<String> _) {
                      System.out.println("Processing consumer");
                  }

                  void test() {
                      process(s -> System.out.println(s));
                  }
              }
              """
          )
        );
    }
}
