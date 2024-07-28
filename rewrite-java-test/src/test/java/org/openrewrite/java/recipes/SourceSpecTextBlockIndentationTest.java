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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SourceSpecTextBlockIndentationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SourceSpecTextBlockIndentation())
          .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @DocumentExample
    @Test
    void minimalIndentation() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              import org.openrewrite.test.RewriteTest;
              import static org.openrewrite.test.SourceSpecs.text;

              class MyRecipeTest implements RewriteTest {
                  void test() {
                    rewriteRun(
                       text(
                         \"""
                             class Test {
              \s
                                \s
                                 void test() {
                                     System.out.println("Hello, world!");
                                 }
                             }
                           \"""
                       )
                    );
                  }
              }
              """,
            """
              import org.openrewrite.test.RewriteTest;
              import static org.openrewrite.test.SourceSpecs.text;

              class MyRecipeTest implements RewriteTest {
                  void test() {
                    rewriteRun(
                       text(
                         \"""
                           class Test {
              \s
                              \s
                               void test() {
                                   System.out.println("Hello, world!");
                               }
                           }
                           \"""
                       )
                    );
                  }
              }
              """
          )
        );
    }

    @Test
    void startsOnNewline() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              import org.openrewrite.test.RewriteTest;
              import static org.openrewrite.test.SourceSpecs.text;

              class MyRecipeTest implements RewriteTest {
                  void test() {
                    rewriteRun(
                       text(\"""
                           class Test {
              \s
                              \s
                               void test() {
                                   System.out.println("Hello, world!");
                               }
                           }
                           \"""
                       )
                    );
                  }
              }
              """,

            """
              import org.openrewrite.test.RewriteTest;
              import static org.openrewrite.test.SourceSpecs.text;

              class MyRecipeTest implements RewriteTest {
                  void test() {
                    rewriteRun(
                       text(
                            \"""
                           class Test {
              \s
                              \s
                               void test() {
                                   System.out.println("Hello, world!");
                               }
                           }
                           \"""
                       )
                    );
                  }
              }
              """
          )
        );
    }
}
