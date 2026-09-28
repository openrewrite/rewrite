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
import org.junit.jupiter.api.Timeout;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.tree.ParseError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD;
import static org.openrewrite.java.Assertions.java;

class JavaParserTypeAttributionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().typeAttribution(false))
          .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void declarationsAreTypedButExpressionsAreNot() {
        rewriteRun(
          java(
            """
              class A {
                  String s = "a".trim();
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.ClassDeclaration a = cu.getClasses().get(0);
                J.VariableDeclarations s = (J.VariableDeclarations) a.getBody().getStatements().get(0);
                assertThat(a.getType()).isNotNull();
                assertThat(((J.MethodInvocation) s.getVariables().get(0).getInitializer()).getMethodType()).isNull();
            })
          )
        );
    }

    @Test
    @Timeout(value = 30, threadMode = SEPARATE_THREAD)
    void deeplyNestedCallsToAMissingMethod() {
        // javac's error recovery re-attributes every argument, multiplying the work at each level
        StringBuilder call = new StringBuilder("x");
        for (int i = 0; i < 20; i++) {
            call.insert(0, "add(").append(", f(x))");
        }
        rewriteRun(
          java(String.format("class A extends Missing { Object m(Object x) { return %s; } }", call))
        );
    }

    @Test
    void stackOverflowIsReportedOnce() {
        String concat = String.join(" + ", Collections.nCopies(50_000, "\"a\""));
        List<Throwable> errors = new ArrayList<>();
        List<SourceFile> parsed = JavaParser.fromJavaVersion().typeAttribution(false).build()
          .parse(new InMemoryExecutionContext(errors::add), String.format("class A { String s = %s; }", concat))
          .collect(toList());

        assertThat(parsed.get(0)).isInstanceOf(ParseError.class);
        assertThat(errors).hasSize(1);
    }

    @Test
    void lambdaParameterWithoutAType() {
        rewriteRun(
          java(
            """
              import java.util.function.Function;
              class A {
                  Function<Object, Object> f = x -> x;
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void localRecordWithCompactConstructor() {
        rewriteRun(
          java(
            """
              class A {
                  void m() {
                      record R(int x) {
                          R {
                              var y = x;
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @MinimumJava21
    @Test
    void varInRecordPattern() {
        rewriteRun(
          java(
            """
              class A {
                  record Pair(Object a, Object b) {
                  }

                  void m(Object o) {
                      if (o instanceof Pair(var a, var b)) {
                      }
                  }
              }
              """
          )
        );
    }
}
