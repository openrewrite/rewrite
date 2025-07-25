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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeMethodInvocationReturnTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeMethodInvocationReturnType("java.lang.Integer parseInt(String)", "long"));
    }

    @DocumentExample
    @Test
    void replaceVariableAssignment() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      int one = Integer.parseInt("1");
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      long one = Integer.parseInt("1");
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldOnlyChangeTargetMethodAssignments() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      int zero = Integer.valueOf("0");
                      int one = Integer.parseInt("1");
                      int two = Integer.valueOf("2");
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      int zero = Integer.valueOf("0");
                      long one = Integer.parseInt("1");
                      int two = Integer.valueOf("2");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVariableAssignmentFullyQualified() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodInvocationReturnType("bar.Bar bar()", "java.math.BigInteger"))
            .parser(JavaParser.fromJavaVersion()
              //language=java
              .dependsOn(
                """
                  package bar;
                  public class Bar {
                      public static Integer bar() {
                          return null;
                      }
                  }
                  """
              )
            ),
          //language=java
          java(
            """
              import bar.Bar;
              class Foo {
                  void foo() {
                      Integer one = Bar.bar();
                  }
              }
              """,
            """
              import bar.Bar;

              import java.math.BigInteger;

              class Foo {
                  void foo() {
                      BigInteger one = Bar.bar();
                  }
              }
              """
          )
        );
    }
}
