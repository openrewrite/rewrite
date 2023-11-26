/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceConstantWithAnotherConstantTest implements RewriteTest {

    @Test
    void replaceConstantInAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "com.constant.B.PATH_SEPARATOR")),
          java(
            """
              package com.constant;
              public class B {
                  public static final String PATH_SEPARATOR = ":";
              }
              """
          ),
          java(
            """
              import java.io.File;
              
              @SuppressWarnings(File.pathSeparator)
              class Test {
                  @SuppressWarnings(value = File.pathSeparator)
                  void foo() {
                      System.out.println("Annotation");
                  }
              }
              """,
            """
              import com.constant.B;
              
              @SuppressWarnings(B.PATH_SEPARATOR)
              class Test {
                  @SuppressWarnings(value = B.PATH_SEPARATOR)
                  void foo() {
                      System.out.println("Annotation");
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/pull/3448")
    void replaceConstantInCurlyBracesInAnnotation() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "java.io.File.separator"))
            .parser(JavaParser.fromJavaVersion().classpath("guava")),
          java(
            """
              import java.io.File;
              
              class Test {
                  @SuppressWarnings({File.pathSeparator})
                  private String bar;
              }
              """,
            """
              import java.io.File;
              
              class Test {
                  @SuppressWarnings({File.separator})
                  private String bar;
              }
              """
          )
        );
    }

    @Test
    void replaceConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "java.io.File.separator")),
          java(
            """
              import java.io.File;

              import static java.io.File.pathSeparator;

              class Test {
                  Object o = File.pathSeparator;
                  void foo() {
                      System.out.println(pathSeparator);
                      System.out.println(java.io.File.pathSeparator);
                  }
              }
              """,
            """
              import java.io.File;

              import static java.io.File.separator;

              class Test {
                  Object o = File.separator;
                  void foo() {
                      System.out.println(separator);
                      System.out.println(File.separator);
                  }
              }
              """
          )
        );
    }

    @Test
    void removeTopLevelClassImport() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo.Bar.Baz.QUX", "Test.FOO")),
          java(
            """
              package foo;

              public class Bar {
                  public static class Baz {
                      public static final String QUX = "QUX";
                  }
              }
              """
          ),
          java(
            """
              import foo.Bar;

              class Test {
                  static final String FOO = "foo";
                  Object o = Bar.Baz.QUX;
              }
              """,
            """
              class Test {
                  static final String FOO = "foo";
                  Object o = Test.FOO;
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3555")
    void replaceConstantForAnnotatedParameter() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("java.io.File.pathSeparator", "java.io.File.separator")),
          java(
            """
              import java.io.File;
              
              class Test {
                  void foo(@SuppressWarnings(value = File.pathSeparator) String param) {
                      System.out.println(param);
                  }
              }
              """,
            """
              import java.io.File;
              
              class Test {
                  void foo(@SuppressWarnings(value = File.separator) String param) {
                      System.out.println(param);
                  }
              }
              """
          )
        );
    }
}
