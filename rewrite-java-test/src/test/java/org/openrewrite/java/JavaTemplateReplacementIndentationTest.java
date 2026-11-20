/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Code a template puts in place of code that started its line starts that line as the replaced code did, however the
 * rest of the file is indented.
 */
class JavaTemplateReplacementIndentationTest implements RewriteTest {

    @DocumentExample
    @Test
    void replacedStatementKeepsItsTabInAFileIndentedWithSpaces() {
        rewriteRun(
          spec -> spec.recipe(replaceFooWith("bar()")),
          java(
            """
              class A {
                  void foo() {
                  }

                  void bar() {
                  }

                  void test() {
                      int i = 0;
              \t\tfoo();
                      i++;
                  }
              }
              """,
            """
              class A {
                  void foo() {
                  }

                  void bar() {
                  }

                  void test() {
                      int i = 0;
              \t\tbar();
                      i++;
                  }
              }
              """
          )
        );
    }

    @Test
    void replacedStatementKeepsTheCommentAndIndentationItStartedWith() {
        rewriteRun(
          spec -> spec.recipe(replaceFooWith("bar()")),
          java(
            """
              class A {
                  void foo() {
                  }

                  void bar() {
                  }

                  void test() {
                        // the call that changes
                        foo();
                  }
              }
              """,
            """
              class A {
                  void foo() {
                  }

                  void bar() {
                  }

                  void test() {
                        // the call that changes
                        bar();
                  }
              }
              """
          )
        );
    }

    @Test
    void replacedArgumentKeepsTheIndentationOfItsLine() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                  if (!"first".equals(literal.getValue())) {
                      return literal;
                  }
                  return JavaTemplate.apply("\"FIRST\"", getCursor(), literal.getCoordinates().replace());
              }
          })),
          java(
            """
              class A {
                  void log(String a, String b) {
                  }

                  void test() {
                      log(
                        "first",
                        "second");
                  }
              }
              """,
            """
              class A {
                  void log(String a, String b) {
                  }

                  void test() {
                      log(
                        "FIRST",
                        "second");
                  }
              }
              """
          )
        );
    }

    private static Recipe replaceFooWith(String code) {
        return toRecipe(() -> new JavaVisitor<>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (!"foo".equals(method.getSimpleName())) {
                    return method;
                }
                return JavaTemplate.builder(code).contextSensitive().build()
                  .apply(getCursor(), method.getCoordinates().replace());
            }
        });
    }
}
