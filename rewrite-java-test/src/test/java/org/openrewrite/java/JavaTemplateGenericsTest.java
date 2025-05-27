/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateGenericsTest implements RewriteTest {

    @DocumentExample
    @Test
    void genericTypes() {
        JavaTemplate invalidPrintf = JavaTemplate.builder("System.out.printf(#{any(T)})")
          .genericTypes("T")
          .build();
        JavaTemplate invalidSort = JavaTemplate.builder("java.util.Collections.sort(#{any(java.util.List<T>)}, #{any(C)})")
          .genericTypes("T", "C extends java.util.Comparator<?>")
          .build();
        JavaTemplate validPrintf = JavaTemplate.builder("System.out.printf(#{any(T)})")
          .genericTypes("T extends String")
          .build();
        JavaTemplate validSort = JavaTemplate.builder("java.util.Collections.sort(#{any(java.util.List<T>)}, #{any(C)})")
          .genericTypes("T", "C extends java.util.Comparator<? super T>")
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                  J.VariableDeclarations.NamedVariable variable = multiVariable.getVariables().getFirst();
                  if ("o".equals(variable.getSimpleName())) {
                      Expression exp = Objects.requireNonNull(variable.getInitializer());
                      J.MethodInvocation res1 = invalidPrintf.apply(getCursor(), multiVariable.getCoordinates().replace(), exp);
                      assertThat(res1.getMethodType()).isNull();
                      J.MethodInvocation res2 = invalidSort.apply(getCursor(), multiVariable.getCoordinates().replace(), exp, exp);
                      assertThat(res2.getMethodType()).isNull();
                      J.MethodInvocation res3 = validPrintf.apply(getCursor(), multiVariable.getCoordinates().replace(), exp);
                      assertThat(res3.getMethodType()).isNotNull();
                      J.MethodInvocation res4 = validSort.apply(getCursor(), multiVariable.getCoordinates().replace(), exp, exp);
                      assertThat(res4.getMethodType()).isNotNull();
                      return res3;
                  }
                  return super.visitVariableDeclarations(multiVariable, executionContext);
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      Object o = any();
                  }
                  static native <T> T any();
              }
              """,
            """
              class Test {
                  void test() {
                      System.out.printf(any());
                  }
                  static native <T> T any();
              }
              """
          )
        );
    }

    @Test
    void expressionTest() {
        var template = JavaTemplate.builder("!#{iterable:any(java.lang.Iterable<T>)}.iterator().hasNext()")
          .genericTypes("T")
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Unary visitUnary(J.Unary unary, ExecutionContext executionContext) {
                  return template.matches(getCursor()) ? SearchResult.found(unary) : super.visitUnary(unary, executionContext);
              }
          })),
          java(
            """
              import java.util.List;
              
              class Test {
                  boolean test() {
                      return !List.of("1").iterator().hasNext();
                  }
              }
              """,
            """
              import java.util.List;
              
              class Test {
                  boolean test() {
                      return /*~~>*/!List.of("1").iterator().hasNext();
                  }
              }
              """
          )
        );
    }

    @Test
    void recursiveType() {
        var template = JavaTemplate.builder("#{enumAssert:any(org.assertj.core.api.AbstractIterableAssert<?, ?, E, ?>)}.size().isLessThan(#{size:any(int)}).returnToIterable()")
          .genericTypes("E")
          .javaParser(JavaParser.fromJavaVersion().classpath("assertj-core"))
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  return template.matches(getCursor()) ? SearchResult.found(method) : super.visitMethodInvocation(method, executionContext);
              }
          })),
          java(
            """
              import com.google.common.collect.ImmutableSet;
              import org.assertj.core.api.AbstractAssert;
              import org.assertj.core.api.Assertions;
              
              class Test {
                  void test() {
                      Assertions.assertThat(ImmutableSet.of(1)).size().isLessThan(2).returnToIterable();
                  }
              }
              """,
            """
              import com.google.common.collect.ImmutableSet;
              import org.assertj.core.api.AbstractAssert;
              import org.assertj.core.api.Assertions;
              
              class Test {
                  void test() {
                      /*~~>*/Assertions.assertThat(ImmutableSet.of(1)).size().isLessThan(2).returnToIterable();
                  }
              }
              """
          )
        );
    }

    @Test
    void methodModifiersMismatch() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate template = JavaTemplate.builder("#{stream:any(java.util.stream.Stream<T>)}.filter(#{map:any(java.util.Map<K, V>)}::containsKey).map(#{map}::get)")
                .genericTypes("T", "K", "V")
                .build();

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  return template.matches(getCursor()) ? SearchResult.found(method) : super.visitMethodInvocation(method, executionContext);
              }
          })),
          java(
            """
              import com.google.common.collect.ImmutableMap;
              
              import java.util.function.Function;
              import java.util.function.Predicate;
              import java.util.stream.Stream;
              
              class Test {
                  Stream<Integer> test() {
                      return Stream.of("foo").filter(ImmutableMap.of(1, 2)::containsKey).map(ImmutableMap.of(1, 2)::get);
                  }
              }
              """,
            """
              import com.google.common.collect.ImmutableMap;
              
              import java.util.function.Function;
              import java.util.function.Predicate;
              import java.util.stream.Stream;
              
              class Test {
                  Stream<Integer> test() {
                      return /*~~>*/Stream.of("foo").filter(ImmutableMap.of(1, 2)::containsKey).map(ImmutableMap.of(1, 2)::get);
                  }
              }
              """
          )
        );
    }
}
