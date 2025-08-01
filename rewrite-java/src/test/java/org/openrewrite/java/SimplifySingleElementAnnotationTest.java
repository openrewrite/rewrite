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

/**
 * Examples for some tests taken from: <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.7.3">Single-Element Annotations</a>.
 */
class SimplifySingleElementAnnotationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SimplifySingleElementAnnotation())
          .parser(JavaParser.fromJavaVersion()
            .classpath("jakarta.validation-api")
          );
    }

    /**
     * Here is an example of a single-element annotation.
     */
    @DocumentExample
    @Test
    void simpleExample() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  @interface Copyright {
                      String value();
                  }
                  """
              )
            ),
          java(
            """
              @Copyright(value = "2002 Yoyodyne Propulsion Systems, Inc.")
              class OscillationOverthruster {
              }
              """,
            """
              @Copyright("2002 Yoyodyne Propulsion Systems, Inc.")
              class OscillationOverthruster {
              }
              """
          )
        );
    }

    /**
     * Here is an example of an array-valued single-element annotation.
     */
    @Test
    void simpleExampleWithArray() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  @interface Endorsers {
                      String[] value();
                  }
                  """
              )
            ),
          java(
            """
              @Endorsers(value = {"Children", "Unscrupulous dentists"})
              public class Lollipop {
              }
              """,
            """
              @Endorsers({"Children", "Unscrupulous dentists"})
              public class Lollipop {
              }
              """
          )
        );
    }

    /**
     * Here is an example of a single-element array-valued single-element annotation
     * (note that the curly braces are omitted).
     */
    @Test
    void simpleExampleWithSingleElementArray() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  @interface Endorsers {
                      String[] value();
                  }
                  """
              )
            ),
          java(
            """
              @Endorsers(value = {"Epicurus"})
              public class Lollipop {
              }
              """,
            """
              @Endorsers("Epicurus")
              public class Lollipop {
              }
              """
          )
        );
    }

    /**
     * Here is an example of a single-element annotation that uses an enum type defined inside the annotation type.
     */
    @Test
    void simpleExampleWithSingleElementEnum() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  public @interface Quality {
                      Level value();

                      enum Level {
                          POOR, AVERAGE, GOOD, EXCELLENT
                      }
                  }
                  """
              )
            ),
          java(
            """
              @Quality(value = Quality.Level.GOOD)
              class Karma {
              }
              """,
            """
              @Quality(Quality.Level.GOOD)
              class Karma {
              }
              """
          )
        );
    }

    @Test
    void noChanges() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion()
              .dependsOn(
                """
                  @interface Copyright {
                      String value();
                  }
                  """
              )
            ),
          java(
            """
              @Copyright("2002 Yoyodyne Propulsion Systems, Inc.")
              class OscillationOverthruster {
              }
              """
          )
        );
    }

    @Test
    void nestedAnnotations() {
        rewriteRun(
          java(
            """
              import jakarta.validation.constraints.*;
              class Example {
                  @NotNull(value = @Size(min = 5))
                  String field1;
                  @Deprecated(value = @SuppressWarnings(value = "unchecked"))
                  void method() {}
              }
              """,
            """
              import jakarta.validation.constraints.*;
              class Example {
                  @NotNull(@Size(min = 5))
                  String field1;
                  @Deprecated(@SuppressWarnings("unchecked"))
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void nestedAnnotationArrays() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @interface Container {
                  Repeatable[] value();
              }
              @interface Repeatable {
                  String value();
              }
              class Example {
                  @Container(value = {@Repeatable(value = "one"), @Repeatable(value = "two")})
                  void method1() {}
                  @Container(value = {@Repeatable(value = "single")})
                  void method2() {}
              }
              """,
            """
              import java.lang.annotation.*;
              @interface Container {
                  Repeatable[] value();
              }
              @interface Repeatable {
                  String value();
              }
              class Example {
                  @Container({@Repeatable("one"), @Repeatable("two")})
                  void method1() {}
                  @Container(@Repeatable("single"))
                  void method2() {}
              }
              """
          )
        );
    }

    @Test
    void classLiterals() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @interface TestWith {
                  Class<?> value();
              }
              @interface TestWithArray {
                  Class<?>[] value();
              }
              class Example {
                  @TestWith(value = String.class)
                  void test1() {}
                  @TestWithArray(value = {String.class, Integer.class})
                  void test2() {}
                  @TestWithArray(value = {String.class})
                  void test3() {}
              }
              """,
            """
              import java.lang.annotation.*;
              @interface TestWith {
                  Class<?> value();
              }
              @interface TestWithArray {
                  Class<?>[] value();
              }
              class Example {
                  @TestWith(String.class)
                  void test1() {}
                  @TestWithArray({String.class, Integer.class})
                  void test2() {}
                  @TestWithArray(String.class)
                  void test3() {}
              }
              """
          )
        );
    }

    @Test
    void multipleAnnotationsOnSameElement() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @interface A {
                  String value();
              }
              @interface B {
                  int value();
              }
              class Example {
                  @A(value = "test")
                  @B(value = 42)
                  @Deprecated
                  void method() {}
                  @A(value = "field")
                  @B(value = 100)
                  String field;
              }
              """,
            """
              import java.lang.annotation.*;
              @interface A {
                  String value();
              }
              @interface B {
                  int value();
              }
              class Example {
                  @A("test")
                  @B(42)
                  @Deprecated
                  void method() {}
                  @A("field")
                  @B(100)
                  String field;
              }
              """
          )
        );
    }

    @Test
    void annotationsWithMultipleAttributes() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @interface Complex {
                  String value();
                  int priority() default 0;
              }
              class Example {
                  // Should not be simplified - has multiple attributes
                  @Complex(value = "test", priority = 5)
                  void method1() {}
                  // Should be simplified - only value attribute
                  @Complex(value = "test")
                  void method2() {}
              }
              """,
            """
              import java.lang.annotation.*;
              @interface Complex {
                  String value();
                  int priority() default 0;
              }
              class Example {
                  // Should not be simplified - has multiple attributes
                  @Complex(value = "test", priority = 5)
                  void method1() {}
                  // Should be simplified - only value attribute
                  @Complex("test")
                  void method2() {}
              }
              """
          )
        );
    }

    @Test
    void emptyArrays() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @interface Tags {
                  String[] value();
              }
              class Example {
                  @Tags(value = {})
                  void method1() {}
              }
              """,
            """
              import java.lang.annotation.*;
              @interface Tags {
                  String[] value();
              }
              class Example {
                  @Tags({})
                  void method1() {}
              }
              """
          )
        );
    }

    @Test
    void annotationOnAnnotation() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @Target(value = ElementType.TYPE)
              @Retention(value = RetentionPolicy.RUNTIME)
              @interface MyAnnotation {
                  String description();
              }
              """,
            """
              import java.lang.annotation.*;
              @Target(ElementType.TYPE)
              @Retention(RetentionPolicy.RUNTIME)
              @interface MyAnnotation {
                  String description();
              }
              """
          )
        );
    }

    @Test
    void constantExpressions() {
        rewriteRun(
          java(
            """
              import java.lang.annotation.*;
              @interface MaxValue {
                  int value();
              }
              class Example {
                  static final int MAX = 100;
                  @MaxValue(value = 50 + 50)
                  void method1() {}
                  @MaxValue(value = MAX)
                  void method2() {}
              }
              """,
            """
              import java.lang.annotation.*;
              @interface MaxValue {
                  int value();
              }
              class Example {
                  static final int MAX = 100;
                  @MaxValue(50 + 50)
                  void method1() {}
                  @MaxValue(MAX)
                  void method2() {}
              }
              """
          )
        );
    }
}
