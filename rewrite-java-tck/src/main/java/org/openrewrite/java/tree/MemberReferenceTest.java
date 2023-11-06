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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
class MemberReferenceTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/548")
    @Test
    void unknownDeclaringType() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().identifiers(false).methodInvocations(false).build()),
          java(
            """
              package com.company.pkg;

              import java.util.concurrent.CompletableFuture;
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import com.company.pkg.UnknownClass;

              public class ReproduceParserIssue {

                public static void main(String[] args) {
                  CompletableFuture.supplyAsync(
                      () -> {
                        return Stream.empty()
                            .map(UnknownClass::valueOf) 
                            .collect(Collectors.toList());
                      });
                }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/608")
    void memberReferenceWithTypeParameter() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.Map;
              import java.util.Set;
              import java.util.function.Function;
              import java.util.stream.Collector;
              import java.util.stream.Collectors;
                            
              public class Sample {
                            
                  private void foo(List<Criteria> request) {
                      Set<Id<Criteria>> allCriteria = request.stream()
                              .map(Id::<Criteria>of)
                              .collect(Collectors.toSet());
                      allCriteria = request.stream()
                              .map(Id/**yo**/::/**hola**/<Criteria>/**hello**/of)
                              .collect(Collectors.toSet());
                              
                      List<String> result = allCriteria.stream()
                              .map(Sample::idToString)
                              .collect(Collectors.toList());
                  }
                            
                  public static <T, K, V> Collector<T, ?, Map<K, V>> toImmutableMap(
                          Function<? super T, ? extends K> keyFunction,
                          Function<? super T, ? extends V> valueFunction) {
                      return null;
                  }
                  private static String idToString(Id id) {
                      return id.toString();
                  }
                  private class Id<T> {
                      static <Y> Id<Y> of(Y thing) {
                          return null;
                      }
                  }
                            
                  private class Criteria {
                      Long getId() {
                          return null;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void staticFunctionReference() {
        rewriteRun(
          java(
            """
              import java.util.stream.Stream;

              public class StaticLambdaRef {
                  void test() {
                      Stream.of("s").forEach(A :: func);
                  }
              }

              class A {
                  static void func(String s) {}
              }
              """
          )
        );
    }

    @Test
    void constructorMethodReference() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().methodInvocations(false).build()),
          java(
            """
              import java.util.HashSet;
              import java.util.Set;
              import java.util.stream.Stream;
              
              class Test {
                  Stream<Integer> n = Stream.of(1, 2);
                  Set<Integer> n2 = n.collect(HashSet < Integer > :: new, HashSet :: add);
              }
              """
          )
        );
    }
}
