/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"RedundantCast", "SimplifyStreamApiCallChains", "Convert2MethodRef", "CodeBlock2Expr", "RedundantOperationOnEmptyContainer", "ResultOfMethodCallIgnored"})
class ReplaceLambdaWithMethodReferenceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceLambdaWithMethodReference());
    }

    @Test
    void dontSelectCastFromTypeVariable() {
        rewriteRun(
          java(
            """
              import java.util.function.Supplier;
              class Test<T> {
                  Supplier<T> test() {
                        return () -> (T) this;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1926")
    @Test
    void multipleMethodInvocations() {
        rewriteRun(
          java(
            """
              import java.nio.file.Path;
              import java.nio.file.Paths;
              import java.util.List;import java.util.stream.Collectors;
                            
              class Test {
                  Path path = Paths.get("");
                  List<String> method(List<String> l) {
                      return l.stream()
                          .filter(s -> path.getFileName().toString().equals(s))
                          .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Test
    void containsMultipleStatements() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.stream.Collectors;

              class Test {
                  List<Integer> even(List<Integer> l) {
                      return l.stream().map(n -> {
                          if (n % 2 == 0) return n;
                          return n * 2;
                      }).collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1772")
    @Test
    void typeCastOnMethodInvocationReturnType() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.stream.Collectors;
              import java.util.stream.Stream;

              class Test {
                  public void foo() {
                      List<String> bar = Stream.of("A", "b")
                              .map(s -> (String) s.toLowerCase())
                              .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Test
    void instanceOf() {
        rewriteRun(
          java(
            """
              package org.test;
              public class CheckType {
              }
              """
          ),
          java(
            """
              import java.util.List;
              import java.util.stream.Collectors;

              import org.test.CheckType;

              class Test {
                  List<Object> method(List<Object> input) {
                      return input.stream().filter(n -> n instanceof CheckType).collect(Collectors.toList());
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.stream.Collectors;

              import org.test.CheckType;

              class Test {
                  List<Object> method(List<Object> input) {
                      return input.stream().filter(CheckType.class::isInstance).collect(Collectors.toList());
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MemberReference visitMemberReference(J.MemberReference memberRef, Object o) {
                    assertThat(TypeUtils.isOfClassType(((J.FieldAccess) memberRef.getContaining()).getTarget().getType(),
                      "org.test.CheckType")).isTrue();
                    return memberRef;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void functionMultiParamReference() {
        rewriteRun(
          java(
            """
              public interface ObservableValue<T> {
              }
              """
          ),
          java(
            """
              @FunctionalInterface
              public interface ChangeListener<T> {
                  void changed(ObservableValue<? extends T> observable, T oldValue, T newValue);
              }
              """
          ),
          java(
            """
              import java.util.function.Function;
              class Test {
                            
                  ChangeListener listener = (o, oldVal, newVal) -> {
                      onChange(o, oldVal, newVal);
                  };
                  
                  protected void onChange(ObservableValue<?> o, Object oldVal, Object newVal) {
                      String strVal = newVal.toString();
                      System.out.println(strVal);
                  }
              }
              """,
            """
              import java.util.function.Function;
              class Test {
                            
                  ChangeListener listener = this::onChange;
                  
                  protected void onChange(ObservableValue<?> o, Object oldVal, Object newVal) {
                      String strVal = newVal.toString();
                      System.out.println(strVal);
                  }
              }
              """
          )
        );
    }

    @Test
    void nonStaticMethods() {
        rewriteRun(
          java(
            """
              import java.util.Collections;
              class Test {
                  Runnable r = () -> run();
                  public void run() {
                      Collections.singletonList(1).forEach(n -> run());
                  }
              }
                            
              class Test2 {
                  Test t = new Test();
                  Runnable r = () -> t.run();
              }
              """,
            """
              import java.util.Collections;
              class Test {
                  Runnable r = this::run;
                  public void run() {
                      Collections.singletonList(1).forEach(n -> run());
                  }
              }
                            
              class Test2 {
                  Test t = new Test();
                  Runnable r = t::run;
              }
              """
          )
        );
    }

    @Test
    void staticMethods() {
        rewriteRun(
          java(
            """
              import java.util.Collections;
              class Test {
                  Runnable r = () -> run();
                  public static void run() {
                      Collections.singletonList(1).forEach(n -> run());
                  }
              }
                            
              class Test2 {
                  Runnable r = () -> Test.run();
              }
              """,
            """
              import java.util.Collections;
              class Test {
                  Runnable r = Test::run;
                  public static void run() {
                      Collections.singletonList(1).forEach(n -> run());
                  }
              }
                            
              class Test2 {
                  Runnable r = Test::run;
              }
              """
          )
        );
    }

    @Test
    void systemOutPrint() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class Test {
                  void method(List<Integer> input) {
                      input.forEach(x -> System.out.println(x));
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  void method(List<Integer> input) {
                      input.forEach(System.out::println);
                  }
              }
              """
          )
        );
    }

    @Test
    void systemOutPrintInBlock() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class Test {
                  void method(List<Integer> input) {
                      input.forEach(x -> { System.out.println(x); });
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  void method(List<Integer> input) {
                      input.forEach(System.out::println);
                  }
              }
              """
          )
        );
    }

    @Test
    void castType() {
        rewriteRun(
          java(
            """
              package org.test;
              public class CheckType {
              }
              """
          ),
          java(
            """
              import java.util.List;
              import java.util.stream.Collectors;

              import org.test.CheckType;

              class Test {
                  List<Object> filter(List<Object> l) {
                      return l.stream()
                          .filter(CheckType.class::isInstance)
                          .map(o -> (CheckType) o)
                          .collect(Collectors.toList());
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.stream.Collectors;

              import org.test.CheckType;

              class Test {
                  List<Object> filter(List<Object> l) {
                      return l.stream()
                          .filter(CheckType.class::isInstance)
                          .map(CheckType.class::cast)
                          .collect(Collectors.toList());
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MemberReference visitMemberReference(J.MemberReference memberRef, Object o) {
                    assertThat(TypeUtils.isOfClassType(((J.FieldAccess) memberRef.getContaining()).getTarget().getType(),
                      "org.test.CheckType")).isTrue();
                    return memberRef;
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void notEqualToNull() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.stream.Collectors;

              class Test {
                  List<Object> filter(List<Object> l) {
                      return l.stream()
                          .filter(o -> o != null)
                          .collect(Collectors.toList());
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.Objects;
              import java.util.stream.Collectors;

              class Test {
                  List<Object> filter(List<Object> l) {
                      return l.stream()
                          .filter(Objects::nonNull)
                          .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("Convert2MethodRef")
    @Test
    void isEqualToNull() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.stream.Collectors;

              class Test {
                  boolean containsNull(List<Object> l) {
                      return l.stream()
                          .anyMatch(o -> o == null);
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.Objects;
              import java.util.stream.Collectors;

              class Test {
                  boolean containsNull(List<Object> l) {
                      return l.stream()
                          .anyMatch(Objects::isNull);
                  }
              }
              """
          )
        );
    }

    @Test
    void voidMethodReference() {
        rewriteRun(
          java(
            """
                  class Test {
                      Runnable r = () -> {
                          this.execute();
                      };

                      void execute() {}
                  }
              """,
            """
                  class Test {
                      Runnable r = this::execute;

                      void execute() {}
                  }
              """
          )
        );
    }

    @Test
    void functionReference() {
        rewriteRun(
          java(
            """
              import java.util.function.Function;

              class Test {
                  Function<Integer, String> f = (i) -> {
                      return this.execute(i);
                  };
                  
                  String execute(Integer i) {
                      return i.toString();
                  }
              }
              """,
            """
              import java.util.function.Function;

              class Test {
                  Function<Integer, String> f = this::execute;
                  
                  String execute(Integer i) {
                      return i.toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void returnExpressionIsNotAMethodInvocation() {
        rewriteRun(
          java(
            """
              class T {
                  public void killBatchJob() {
                      return deleteSparkBatchRequest()
                              .map(resp -> {
                                  return this;
                              })
                              .defaultIfEmpty(this);
                  }
              }
              """
          )
        );
    }

    @Test
    void lambdaReturnsFunctionalInterface() {
        rewriteRun(
          java(
            """
              package abc;
              @FunctionalInterface
              public interface MyFunction {
                  String get();
              }
              """
          ),
          java(
            """
              package abc;
                            
              class M {
                  MyFunction getFunction(String fcn) {
                      return () -> fcn;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2178")
    @Test
    void doNotReplaceInvocationWhichAcceptsArgument() {
        rewriteRun(
          java(
            """
              import java.util.*;

              class A {
                  void foo() {
                      new ArrayList<List<Integer>>().stream()
                              .map(it -> it.addAll(singletonList(1, 2, 3)));
                  }
              }
              """
          )
        );
    }
}
