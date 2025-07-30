/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddMethodParameterTest implements RewriteTest {

    @DocumentExample
    @Test
    void primitive() {
        rewriteRun(
          spec -> spec.recipe(new AddMethodParameter("foo.Foo#bar(..)", "int", "i", null)),
          java(
            """
              package foo;

              public class Foo {
                  public void bar() {
                  }
              }
              """,
            """
              package foo;

              public class Foo {
                  public void bar(int i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void typePattern() {
        rewriteRun(
          spec -> spec.recipe(new AddMethodParameter("*..*#bar(..)", "int", "i", null)),
          java(
            """
              package foo;

              public class Foo {
                  public void bar() {
                  }
              }
              """,
            """
              package foo;

              public class Foo {
                  public void bar(int i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void primitiveArray() {
        rewriteRun(
          spec -> spec.recipe(new AddMethodParameter("foo.Foo#bar(..)", "int[]", "i", null)),
          java(
            """
              package foo;

              public class Foo {
                  public void bar() {
                  }
              }
              """,
            """
              package foo;

              public class Foo {
                  public void bar(int[] i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void parameterized() {
        rewriteRun(
          spec -> spec.recipe(new AddMethodParameter("foo.Foo#bar(..)", "java.util.List<java.util.regex.Pattern>", "i", null)),
          java(
            """
              package foo;

              public class Foo {
                  public void bar() {
                  }
              }
              """,
            """
              package foo;

              import java.util.List;
              import java.util.regex.Pattern;

              public class Foo {
                  public void bar(List<Pattern> i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void wildcard() {
        rewriteRun(
          spec -> spec.recipe(new AddMethodParameter("foo.Foo#bar(..)", "java.util.List<?>", "i", null)),
          java(
            """
              package foo;

              public class Foo {
                  public void bar() {
                  }
              }
              """,
            """
              package foo;

              import java.util.List;

              public class Foo {
                  public void bar(List<?> i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void wildcardExtends() {
        rewriteRun(
          spec -> spec.recipe(new AddMethodParameter("foo.Foo#bar(..)", "java.util.List<? extends Object>", "i", null)),
          java(
            """
              package foo;

              public class Foo {
                  public void bar() {
                  }
              }
              """,
            """
              package foo;

              import java.util.List;

              public class Foo {
                  public void bar(List<? extends Object> i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void string() {
        rewriteRun(
          spec -> spec.recipe(new AddMethodParameter("foo.Foo#bar(..)", "String", "i", null)),
          java(
            """
              package foo;

              public class Foo {
                  public void bar() {
                  }

                  public void bar(int i) {
                  }

                  public void bar(int j) {
                  }
              }
              """,
            """
              package foo;

              public class Foo {
                  public void bar(String i) {
                  }

                  public void bar(int i) {
                  }

                  public void bar(int j, String i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void first() {
        rewriteRun(
          spec -> spec.recipe(new AddMethodParameter("foo.Foo#bar(..)", "int", "i", 0)),
          java(
            """
              package foo;

              public class Foo {
                  public void bar() {
                  }

                  public void bar(int j) {
                  }
              }
              """,
            """
              package foo;

              public class Foo {
                  public void bar(int i) {
                  }

                  public void bar(int i, int j) {
                  }
              }
              """
          )
        );
    }

    @Test
    void qualified() {
        rewriteRun(
          spec -> spec.recipe(new AddMethodParameter("foo.Foo#bar(..)", "java.util.regex.Pattern", "p", null)),
          java(
            """
              package foo;

              public class Foo {
                  public void bar() {
                  }
                  public void bar(int j) {
                  }
              }
              """,
            """
              package foo;

              import java.util.regex.Pattern;

              public class Foo {
                  public void bar(Pattern p) {
                  }

                  public void bar(int j, Pattern p) {
                  }
              }
              """
          )
        );
    }

    @Test
    void object() {
        rewriteRun(
          spec -> spec.recipe(new AddMethodParameter("foo.Foo#bar(..)", "Object", "o", null)),
          java(
            """
              package foo;

              public class Foo {
                  public void bar() {
                  }
              }
              """,
            """
              package foo;

              public class Foo {
                  public void bar(Object o) {
                  }
              }
              """
          )
        );
    }
}
