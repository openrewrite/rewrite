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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceConstantWithAnotherConstantTest implements RewriteTest {

    @DocumentExample
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

    @Issue("https://github.com/openrewrite/rewrite/pull/3448")
    @Test
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

    @Issue("https://github.com/openrewrite/rewrite/issues/3555")
    @Test
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

    @Issue("https://github.com/openrewrite/rewrite/issues/5174")
    @Test
    void shouldUpdateWithinMethod() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo.Bar.QUX1", "foo.Bar.QUX2")),
          java(
            """
              package foo;

              public class Bar {
                  public static final String QUX1 = "QUX1";
                  public static final String QUX2 = "QUX2";
              }
              """
          ),
          java(
            """
              import static foo.Bar.QUX1;

              class Test {
                  String out = QUX1;
                  void a() {
                      String in = QUX1;
                  }
              }
              """,
            """
              import static foo.Bar.QUX2;

              class Test {
                  String out = QUX2;
                  void a() {
                      String in = QUX2;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5174")
    @Test
    void shouldUpdateWithinMethod2() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo.Bar.QUX1", "foo.Bar.QUX2")),
          java(
            """
              package foo;

              public class Bar {
                  public static final String QUX1 = "QUX1";
                  public static final String QUX2 = "QUX2";
              }
              """
          ),
          java(
            """
              import foo.Bar;
              
              import static foo.Bar.QUX2;

              class Test {
                  public static final String QUX1 = "QUX111"; // the same name as Bar.QUX1

                  void a() {
                      String in = Bar.QUX1;
                      String in2 = QUX2;
                  }
              }
              """,
            """
              import foo.Bar;
              
              import static foo.Bar.QUX2;

              class Test {
                  public static final String QUX1 = "QUX111"; // the same name as Bar.QUX1

                  void a() {
                      String in = Bar.QUX2;
                      String in2 = QUX2;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5224")
    @Test
    void shouldFullyQualifyWhenNewTypeIsAmbiguous() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo1.Bar.QUX1", "foo2.Bar.QUX1")),
          java(
            """
              package foo1;

              public class Bar {
                  public static final String QUX1 = "QUX1_FROM_FOO1";
                  public static final String QUX2 = "QUX1_FROM_FOO2";
              }
              """
          ),
          java(
            """
              package foo2;

              public class Bar {
                  public static final String QUX1 = "QUX1_FROM_FOO1";
              }
              """
          ),
          java(
            """
              import foo1.Bar;

              class Test {
                  void a() {
                      System.out.println(Bar.QUX1);
                      System.out.println(Bar.QUX2);
                  }
              }
              """,
            """
              import foo1.Bar;

              class Test {
                  void a() {
                      System.out.println(foo2.Bar.QUX1);
                      System.out.println(Bar.QUX2);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5224")
    @Test
    void shouldFullyQualifyWhenNewTypeIsAmbiguous2() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo1.Bar.QUX1", "foo3.Bar.QUX2")),
          java(
            """
              package foo1;

              public class Bar {
                  public static final String QUX1 = "QUX_FROM_FOO1";
              }
              """
          ),
          java(
            """
              package foo2;

              public class Bar {
                  public static final String QUX2 = "QUX_FROM_FOO2";
              }
              """
          ),
          java(
            """
              package foo3;

              public class Bar {
                  public static final String QUX2 = "QUX_FROM_FOO3";
              }
              """
          ),
          java(
            """
              import static foo1.Bar.QUX1;
              import static foo2.Bar.QUX2;

              class Test {
                  void a() {
                      System.out.println(QUX1);
                      System.out.println(QUX2);
                  }
              }
              """,
            """
              import static foo2.Bar.QUX2;

              class Test {
                  void a() {
                      System.out.println(foo3.Bar.QUX2);
                      System.out.println(QUX2);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5224")
    @Test
    void shouldFullyQualifyWhenNewTypeIsAmbiguous3() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceConstantWithAnotherConstant("foo1.Bar.QUX1", "foo2.Bar.QUX1")),
          java(
            """
              package foo1;

              public class Bar {
                  public static final String QUX1 = "QUX_FROM_FOO1";
              }
              """
          ),
          java(
            """
              package foo2;

              public class Bar {
                  public static final String QUX1 = "QUX_FROM_FOO2";
              }
              """
          ),
          java(
            """
              import static foo1.Bar.QUX1;

              class Test {
                  void a() {
                      System.out.println(QUX1);
                  }
              }
              """,
            """
              import static foo2.Bar.QUX1;

              class Test {
                  void a() {
                      System.out.println(QUX1);
                  }
              }
              """
          )
        );
    }
}
