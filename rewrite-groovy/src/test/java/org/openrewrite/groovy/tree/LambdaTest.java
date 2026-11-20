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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings("GroovyUnusedAssignment")
class LambdaTest implements RewriteTest {

    @Test
    void lambdaExpression() {
        rewriteRun(
          groovy(
                """
            def lambda = a -> a
            """
          )
        );
    }

    @Test
    void lambdaExpressionWithCurlyBraces() {
        rewriteRun(
          groovy(
            """
              def lambda = a -> { a }
              """
          )
        );
    }

    @Test
    void lambdaExpressionNoArguments() {
        rewriteRun(
          groovy(
                """
            ( ) -> arg
            """
          )
        );
    }

    @Test
    void lambdaExpressionWithArgument() {
        rewriteRun(
          groovy(
                """
            ( String arg ) -> arg
            """
          )
        );
    }

    @Test
    void closureReturningLambda() {
        rewriteRun(
          groovy(
                """
            def foo(Closure cl) {}
            foo { String a ->
                ( _ ) -> a
            }
            """
          )
        );
    }

    @Test
    void closureParameterWithType() {
        rewriteRun(
          groovy(
                """
            class A {}
            def foo(Closure cl) {}
            foo { A a ->
                a
                a
            }
            """
          )
        );
    }

    @Test
    void closureParameterWithDefaultValue() {
        rewriteRun(
          groovy(
            """
              javadoc {
                  options {
                      group = 'WDK Language' -> 'com.symphony.bdk.workflow*'
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/298")
    @Test
    void compileStaticLambdaArgumentWithoutParameters() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.CompileStatic

              @CompileStatic
              class Main {
                  static void main(String[] args) {
                      Optional<String> myOptional = Optional.ofNullable("test")
                      String myStr = myOptional.orElseGet(() -> "alternative")
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2168")
    @Test
    void closureNoArguments() {
        rewriteRun(
          groovy(
            """
              def f1 = { -> 1 }
              def f2 = { 1 }
              def f3 = { -> }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8879")
    @Test
    void lambdaArgumentWithoutParentheses() {
        rewriteRun(
          groovy(
            """
              def f(List<String> boxes) {
                  boxes.forEach (String box) -> {
                      println box
                  }
                  boxes.forEach (box) -> println(box)
                  boxes.forEach(box) -> {
                      println box
                  }
                  boxes.forEach (a) -> a
                  boxes.forEach ((String box) -> {
                      println box
                  })
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(new GroovyIsoVisitor<List<Boolean>>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<Boolean> omitsParentheses) {
                    assertThat(method.getArguments()).singleElement().isInstanceOfSatisfying(J.Lambda.class,
                      lambda -> {
                          assertThat(lambda.getParameters().isParenthesized()).isTrue();
                          omitsParentheses.add(lambda.getMarkers().findFirst(OmitParentheses.class).isPresent());
                      });
                    return method;
                }
            }.reduce(cu, new ArrayList<>())).containsExactly(true, true, true, true, false))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8879")
    @Test
    void lambdaArgumentWithEmptyParameterListWithoutParentheses() {
        rewriteRun(
          groovy(
            """
              submit () -> {
                  println "x"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8879")
    @Test
    void multipleLambdaArgumentsWithoutParenthesesInClosures() {
        rewriteRun(
          groovy(
            """
              class A {
                  void mock(Map<String, String> parents) {
                      when(a).thenAnswer {
                          List<String> reqBoxes = it.getArgument(0)
                          reqBoxes.forEach (String box) -> {
                              if (parents.containsKey(box)) {
                                  println box
                              }
                          }
                          return Request.builder()
                                  .boxes(reqBoxes)
                                  .build()
                      }
                      when(b).thenAnswer {
                          List<String> reqBoxes = it.getArgument(0)
                          reqBoxes.forEach (String box) -> {
                              println box
                          }
                          return Response.builder()
                                  .boxes(reqBoxes)
                                  .build()
                      }
                  }
              }
              """
          )
        );
    }
}
