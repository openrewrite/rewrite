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

@SuppressWarnings({
  "ComparatorCombinators",
  "Convert2MethodRef",
  "ResultOfMethodCallIgnored",
  "CodeBlock2Expr"
})
class ExplicitLambdaArgumentTypesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ExplicitLambdaArgumentTypes());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1459")
    @Test
    void unknownArgumentType() {
        rewriteRun(
          java(
            """
              import java.util.function.Predicate;

              class Test {
                  static void run(Predicate<WillyWonka> c) {
                  }

                  static void method() {
                      run(a -> {
                          return a.isEmpty();
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void oneArgumentExistingExplicitType() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.function.Consumer;

              class Test {
                  static void run(Consumer<List<String>> c) {
                  }

                  static void method() {
                      run(a -> a.size());
                  }
              }
              """
          )
        );
    }

    @Test
    void oneArgumentNoBlock() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;

              class Test {
                  static void run(Consumer<String> c) {
                  }

                  static void method() {
                      run(q -> q.length());
                  }
              }
              """
          )
        );
    }

    @Test
    void twoArgumentsExistingExplicitType() {
        rewriteRun(
          java(
            """
              import java.util.function.BiConsumer;

              class Test {
                  static void run(BiConsumer<String, Object> bc) {
                  }

                  static void method() {
                      run((String a, Object b) -> a.length());
                  }
              }
              """
          )
        );
    }

    @Test
    void twoArgumentsNoBlock() {
        rewriteRun(
          java(
            """
              import java.util.function.BiConsumer;

              class Test {
                  static void run(BiConsumer<String, Object> bc) {
                  }

                  static void method() {
                      run((a, b) -> a.length());
                  }
              }
              """
          )
        );
    }

    @Test
    void twoArgumentsWithBlock() {
        rewriteRun(
          java(
            """
              import java.util.function.BiPredicate;

              class Test {
                  static void run(BiPredicate<String, Object> bc) {
                  }

                  static void method() {
                      run((a, b) -> {
                          return a.isEmpty();
                      });
                  }
              }
              """,
            """
              import java.util.function.BiPredicate;

              class Test {
                  static void run(BiPredicate<String, Object> bc) {
                  }

                  static void method() {
                      run((String a, Object b) -> {
                          return a.isEmpty();
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void handlePrimitiveArrays() {
        rewriteRun(
          java(
            """
              import java.util.function.BiFunction;

              class Test {
                  private final BiFunction<Integer, byte[], byte[]> func = (a, b) -> {
                      return null;
                  };
              }
              """,
            """
              import java.util.function.BiFunction;

              class Test {
                  private final BiFunction<Integer, byte[], byte[]> func = (Integer a, byte[] b) -> {
                      return null;
                  };
              }
              """
          )
        );
    }

    @Test
    void handleMultiDimensionalPrimitiveArrays() {
        rewriteRun(
          java(
            """
              import java.util.function.BiFunction;

              class Test {
                  private final BiFunction<Integer, byte[][], byte[][]> func = (a, b) -> {
                      return null;
                  };
              }
              """,
            """
              import java.util.function.BiFunction;

              class Test {
                  private final BiFunction<Integer, byte[][], byte[][]> func = (Integer a, byte[][] b) -> {
                      return null;
                  };
              }
              """
          )
        );
    }

    @Test
    void handleMultiDimensionalFullyQualifiedArrays() {
        rewriteRun(
          java(
            """
              import java.util.function.BiFunction;

              class Test {
                  private final BiFunction<Integer, Integer[][], Integer[][]> func = (a, b) -> {
                      return null;
                  };
              }
              """,
            """
              import java.util.function.BiFunction;

              class Test {
                  private final BiFunction<Integer, Integer[][], Integer[][]> func = (Integer a, Integer[][] b) -> {
                      return null;
                  };
              }
              """
          )
        );
    }

    @Test
    void oneArgumentWithBlock() {
        rewriteRun(
          java(
            """
              import java.util.function.Predicate;

              class Test {
                  static void run(Predicate<String> c) {
                  }

                  static void method() {
                      run(a -> {
                          return a.isEmpty();
                      });
                  }
              }
              """,
            """
              import java.util.function.Predicate;

              class Test {
                  static void run(Predicate<String> c) {
                  }

                  static void method() {
                      run((String a) -> {
                          return a.isEmpty();
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void threeArgumentsNoBlock() {
        rewriteRun(
          java(
            """
              class Test {
                  static void run(TriConsumer tc) {
                  }

                  static void method() {
                      run((a, b, c) -> a.toUpperCase());
                  }

                  private interface TriConsumer {
                      String method(String a, String b, String c);
                  }
              }
              """,
            """
              class Test {
                  static void run(TriConsumer tc) {
                  }

                  static void method() {
                      run((String a, String b, String c) -> a.toUpperCase());
                  }

                  private interface TriConsumer {
                      String method(String a, String b, String c);
                  }
              }
              """
          )
        );
    }

    @Test
    void threeArgumentsWithBlock() {
        rewriteRun(
          java(
            """
              class Test {
                  static void run(TriConsumer tc) {
                  }

                  static void method() {
                      run((a, b, c) -> {
                          return a.toUpperCase();
                      });
                  }

                  private interface TriConsumer {
                      String method(String a, String b, String c);
                  }
              }
              """,
            """
              class Test {
                  static void run(TriConsumer tc) {
                  }

                  static void method() {
                      run((String a, String b, String c) -> {
                          return a.toUpperCase();
                      });
                  }

                  private interface TriConsumer {
                      String method(String a, String b, String c);
                  }
              }
              """
          )
        );
    }

    @Test
    void threeArgumentsWithBlockPrimitive() {
        rewriteRun(
          java(
            """
                  class Test {
                      static void run(TriConsumer tc) {
                      }

                      static void method() {
                          run((a, b, c) -> {
                              return a + b - c;
                          });
                      }

                      private interface TriConsumer {
                          int method(int a, int b, int c);
                      }
                  }
              """,
            """
              class Test {
                  static void run(TriConsumer tc) {
                  }

                  static void method() {
                      run((int a, int b, int c) -> {
                          return a + b - c;
                      });
                  }

                  private interface TriConsumer {
                      int method(int a, int b, int c);
                  }
              }
              """
          )
        );
    }

    @Test
    void threeArgumentsWithBlockGeneric() {
        rewriteRun(
          java(
            """
              class Test {
                  static <T> void run(TriConsumer<T> tc) {
                  }

                  static void method() {
                      run((a, b, c) -> {
                          return a.toString();
                      });
                  }

                  private interface TriConsumer<T> {
                      T method(T a, T b, T c);
                  }
              }
              """,
            """
              class Test {
                  static <T> void run(TriConsumer<T> tc) {
                  }

                  static void method() {
                      run((Object a, Object b, Object c) -> {
                          return a.toString();
                      });
                  }

                  private interface TriConsumer<T> {
                      T method(T a, T b, T c);
                  }
              }
              """
          )
        );
    }

    @Test
    void noArguments() {
        rewriteRun(
          java(
            """
              import java.util.function.Supplier;

              class Test {
                  static void run(Supplier<String> s) {
                  }

                  static void method() {
                      run(() -> {
                          return "example";
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void arraysSortExample() {
        rewriteRun(
          java(
            """
              import java.util.Arrays;

              class Test {
                  static void method(String[] arr) {
                      Arrays.sort(arr, (a, b) -> {
                          return a.length() - b.length();
                      });
                  }
              }
              """,
            """
              import java.util.Arrays;

              class Test {
                  static void method(String[] arr) {
                      Arrays.sort(arr, (String a, String b) -> {
                          return a.length() - b.length();
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void arraysSortExampleWithGeneric() {
        rewriteRun(
          java(
            """
              import java.util.Arrays;

              class Test {
                  static <T> void method(T[] arr) {
                      Arrays.sort(arr, (a, b) -> {
                          return a.toString().length() - b.toString().length();
                      });
                  }
              }
              """,
            """
              import java.util.Arrays;

              class Test {
                  static <T> void method(T[] arr) {
                      Arrays.sort(arr, (T a, T b) -> {
                          return a.toString().length() - b.toString().length();
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void parameterizedTypes() {
        rewriteRun(
          java(
            """
              package javafx.beans.value;

              public interface ObservableValue<T> {
                  void addListener(ChangeListener<? super T> listener);
              }
              """
          ),
          java(
            """
              package javafx.beans.value;
              
              @FunctionalInterface
              public interface ChangeListener<T> {
                  void changed(ObservableValue<? extends T> observable, T oldValue, T newValue);
              }
              """
          ),
          java(
            """
              package example;
                            
              import javafx.beans.value.ObservableValue;
              import javafx.beans.value.ChangeListener;

              public class Fred implements ObservableValue<String>{
                  public void addListener(ChangeListener<? super String> listener) {
                  }
              }
              """
          ),
          java(
            """
              import javafx.beans.value.ObservableValue;
              import example.Fred;
                            
              class Test {
                  void foo() {
                      Fred fred = new Fred();
                      fred.addListener((ov, oldState, newState) -> {
                      });
                  }
              }
              """,
            """
              import javafx.beans.value.ObservableValue;
              import example.Fred;
                            
              class Test {
                  void foo() {
                      Fred fred = new Fred();
                      fred.addListener((ObservableValue<? extends String> ov, String oldState, String newState) -> {
                      });
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2177")
    @Test
    void extendsConstraint() {
        rewriteRun(
          java(
            """
              package com.test;
                            
              import java.util.List;
                            
              class A {
                  void foo(List<? extends A> a) {
                      a.forEach(it -> { });
                  }
              }
              """,
            """
              package com.test;
                            
              import java.util.List;
                            
              class A {
                  void foo(List<? extends A> a) {
                      a.forEach((A it) -> { });
                  }
              }
              """
          )
        );
    }
}
