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

class InlineMethodCallsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new InlineMethodCalls());
    }

    @DocumentExample
    @Test
    void inlineMeSimple() {
        //language=java
        rewriteRun(
          java(
            """
              import org.openrewrite.java.InlineMe;

              class Lib {
                  @Deprecated
                  @InlineMe(replacement = "this.replacement()")
                  public void deprecated() {}

                  public void replacement() {}

                  public static void usage(Lib lib) {
                      lib.deprecated();
                  }
              }
              """,
            """
              import org.openrewrite.java.InlineMe;

              class Lib {
                  @Deprecated
                  @InlineMe(replacement = "this.replacement()")
                  public void deprecated() {}

                  public void replacement() {}

                  public static void usage(Lib lib) {
                      lib.replacement();
                  }
              }
              """
          )
        );
    }

    @Test
    void inlineMeNonStatic() {
        //language=java
        rewriteRun(
          java(
            """
              import org.openrewrite.java.InlineMe;

              class Lib {
                  @Deprecated
                  @InlineMe(replacement = "this.replacement()")
                  public void deprecated() {}
                  public void replacement() {}

                  public void usage() {
                      deprecated();
                  }
              }
              """,
            """
              import org.openrewrite.java.InlineMe;

              class Lib {
                  @Deprecated
                  @InlineMe(replacement = "this.replacement()")
                  public void deprecated() {}
                  public void replacement() {}

                  public void usage() {
                      replacement();
                  }
              }
              """
          )
        );
    }

    @Test
    void inlineMeChained() {
        //language=java
        rewriteRun(
          java(
            """
              import org.openrewrite.java.InlineMe;
              import java.time.Duration;

              class Lib {
                  private final Duration deadline;

                  public Duration getDeadline() {
                      return deadline;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.getDeadline().toMillis()")
                  public long getDeadlineMillis() {
                      return getDeadline().toMillis();
                  }

                  long usage() {
                      return getDeadlineMillis();
                  }
              }
              """,
            """
              import org.openrewrite.java.InlineMe;
              import java.time.Duration;

              class Lib {
                  private final Duration deadline;

                  public Duration getDeadline() {
                      return deadline;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.getDeadline().toMillis()")
                  public long getDeadlineMillis() {
                      return getDeadline().toMillis();
                  }

                  long usage() {
                      return getDeadline().toMillis();
                  }
              }
              """
          )
        );
    }

    @Test
    void instanceMethodWithImports() {
        //language=java
        rewriteRun(
          java(
            """
              import org.openrewrite.java.InlineMe;

              class MyClass {
                  private java.time.Duration deadline;

                  public void setDeadline(java.time.Duration deadline) {
                      this.deadline = deadline;
                  }

                  @Deprecated
                  @InlineMe(
                      replacement = "this.setDeadline(Duration.ofMillis(millis))",
                      imports = {"java.time.Duration"})
                  public void setDeadline(long millis) {
                      setDeadline(java.time.Duration.ofMillis(millis));
                  }

                  void usage() {
                      setDeadline(1000L);
                  }
              }
              """,
            """
              import org.openrewrite.java.InlineMe;

              import java.time.Duration;

              class MyClass {
                  private Duration deadline;

                  public void setDeadline(Duration deadline) {
                      this.deadline = deadline;
                  }

                  @Deprecated
                  @InlineMe(
                      replacement = "this.setDeadline(Duration.ofMillis(millis))",
                      imports = {"java.time.Duration"})
                  public void setDeadline(long millis) {
                      setDeadline(Duration.ofMillis(millis));
                  }

                  void usage() {
                      setDeadline(Duration.ofMillis(1000L));
                  }
              }
              """
          )
        );
    }

    @Test
    void staticMethodReplacement() {
        //language=java
        rewriteRun(
          java(
            """
              package com.google.frobber;

              import org.openrewrite.java.InlineMe;

              class Frobber {

                  public static Frobber fromName(String name) {
                      return new Frobber();
                  }

                  @Deprecated
                  @InlineMe(
                      replacement = "Frobber.fromName(name)",
                      imports = {"com.google.frobber.Frobber"})
                  public static Frobber create(String name) {
                      return fromName(name);
                  }

                  void usage() {
                      Frobber f = Frobber.create("test");
                  }
              }
              """,
            """
              package com.google.frobber;

              import org.openrewrite.java.InlineMe;

              class Frobber {

                  public static Frobber fromName(String name) {
                      return new Frobber();
                  }

                  @Deprecated
                  @InlineMe(
                      replacement = "Frobber.fromName(name)",
                      imports = {"com.google.frobber.Frobber"})
                  public static Frobber create(String name) {
                      return fromName(name);
                  }

                  void usage() {
                      Frobber f = Frobber.fromName("test");
                  }
              }
              """
          )
        );
    }

    @Test
    void constructorToFactoryMethod() {
        //language=java
        rewriteRun(
          java(
            """
              package com.google.frobber;

              import org.openrewrite.java.InlineMe;

              class MyClass {

                  @Deprecated
                  @InlineMe(
                      replacement = "MyClass.create()",
                      imports = {"com.google.frobber.MyClass"})
                  public MyClass() {
                  }

                  public static MyClass create() {
                      return new MyClass();
                  }

                  void usage() {
                      MyClass obj = new MyClass();
                  }
              }
              """,
            """
              package com.google.frobber;

              import org.openrewrite.java.InlineMe;

              class MyClass {

                  @Deprecated
                  @InlineMe(
                      replacement = "MyClass.create()",
                      imports = {"com.google.frobber.MyClass"})
                  public MyClass() {
                  }

                  public static MyClass create() {
                      return new MyClass();
                  }

                  void usage() {
                      MyClass obj = MyClass.create();
                  }
              }
              """
          )
        );
    }

    @Test
    void constructorAddLiteralString() {
        rewriteRun(
          java(
            """
              import org.openrewrite.java.InlineMe;

              class MyClass {
                  @Deprecated
                  @InlineMe(replacement = "this(one, \\"two\\")")
                  public MyClass(String one) {
                      this("one", "two");
                  }

                  public MyClass(String one, String two) {
                  }

                  void usage() {
                      MyClass obj = new MyClass("one");
                  }
              }
              """,
            """
              import org.openrewrite.java.InlineMe;

              class MyClass {
                  @Deprecated
                  @InlineMe(replacement = "this(one, \\"two\\")")
                  public MyClass(String one) {
                      this("one", "two");
                  }

                  public MyClass(String one, String two) {
                  }

                  void usage() {
                      MyClass obj = new MyClass("one", "two");
                  }
              }
              """
          )
        );
    }

    @Test
    void constructorAddLiteralNull() {
        rewriteRun(
          java(
            """
              import org.openrewrite.java.InlineMe;

              class MyClass {
                  @Deprecated
                  @InlineMe(replacement = "this(one, null)")
                  public MyClass(String one) {
                      this("one", null);
                  }

                  public MyClass(String one, String two) {
                  }

                  void usage() {
                      MyClass obj = new MyClass("one");
                  }
              }
              """,
            """
              import org.openrewrite.java.InlineMe;

              class MyClass {
                  @Deprecated
                  @InlineMe(replacement = "this(one, null)")
                  public MyClass(String one) {
                      this("one", null);
                  }

                  public MyClass(String one, String two) {
                  }

                  void usage() {
                      MyClass obj = new MyClass("one", null);
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleParameters() {
        //language=java
        rewriteRun(
          java(
            """
              import org.openrewrite.java.InlineMe;

              class Calculator {

                  public int addAndMultiply(int a, int b, int c) {
                      return (a + b) * c;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.addAndMultiply(x, y, z)")
                  public int compute(int x, int y, int z) {
                      return addAndMultiply(x, y, z);
                  }

                  void foo(Calculator calc) {
                      int result = calc.compute(1, 2, 3);
                  }
              }
              """,
            """
              import org.openrewrite.java.InlineMe;

              class Calculator {

                  public int addAndMultiply(int a, int b, int c) {
                      return (a + b) * c;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.addAndMultiply(x, y, z)")
                  public int compute(int x, int y, int z) {
                      return addAndMultiply(x, y, z);
                  }

                  void foo(Calculator calc) {
                      int result = calc.addAndMultiply(1, 2, 3);
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedMethodCalls() {
        //language=java
        rewriteRun(
          java(
            """
              import org.openrewrite.java.InlineMe;

              class Builder {

                  public Builder withName(String name) {
                      return this;
                  }

                  public Builder withAge(int age) {
                      return this;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.withName(name).withAge(age)")
                  public Builder configure(String name, int age) {
                      return withName(name).withAge(age);
                  }

                  void foo(Builder builder) {
                      builder.configure("John", 30);
                  }
              }
              """,
            """
              import org.openrewrite.java.InlineMe;

              class Builder {

                  public Builder withName(String name) {
                      return this;
                  }

                  public Builder withAge(int age) {
                      return this;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.withName(name).withAge(age)")
                  public Builder configure(String name, int age) {
                      return withName(name).withAge(age);
                  }

                  void foo(Builder builder) {
                      builder.withName("John").withAge(30);
                  }
              }
              """
          )
        );
    }

    @Test
    void sameArgumentUsedTwice() {
        //language=java
        rewriteRun(
          java(
            """
              import org.openrewrite.java.InlineMe;

              class MathUtils {

                  public int square(int n) {
                      return n * n;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.square(x + x)")
                  public int doubleAndSquare(int x) {
                      return square(x + x);
                  }

                  void usage() {
                      int result = doubleAndSquare(5);
                  }
              }
              """,
            """
              import org.openrewrite.java.InlineMe;

              class MathUtils {

                  public int square(int n) {
                      return n * n;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.square(x + x)")
                  public int doubleAndSquare(int x) {
                      return square(x + x);
                  }

                  void usage() {
                      int result = square(5 + 5);
                  }
              }
              """
          )
        );
    }
}
