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

class ChangeMethodParameterTest implements RewriteTest {

    @DocumentExample
    @Test
    void primitive() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "long", "j", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i) {
                  }
              }
              """,
            """
              package foo;
              
              public class Foo {
                  public void bar(long j) {
                  }
              }
              """
          )
        );
    }

    @Test
    void indexLargeThanZero() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "long", "k", 1)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i, int j) {
                  }
              }
              """,
            """
              package foo;
              
              public class Foo {
                  public void bar(int i, long k) {
                  }
              }
              """
          )
        );
    }

    @Test
    void sameName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "long", "i", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i) {
                  }
              }
              """,
            """
              package foo;
              
              public class Foo {
                  public void bar(long i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void sameType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "int", "j", 0)),
          java(
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
    void invalidIndex() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "int", "j", -1)),
          java(
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
    void notExistsIndex() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "int", "j", 1)),
          java(
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
          spec -> spec.recipe(new ChangeMethodParameter("*..*#bar(..)", "long", "j", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i) {
                  }
              }
              """,
            """
              package foo;
              
              public class Foo {
                  public void bar(long j) {
                  }
              }
              """
          )
        );
    }

    @Test
    void primitiveArray() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "int[]", "j", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i) {
                  }
              }
              """,
            """
              package foo;
              
              public class Foo {
                  public void bar(int[] j) {
                  }
              }
              """
          )
        );
    }

    @Test
    void parameterized() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "java.util.List<java.util.regex.Pattern>", "j", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i) {
                  }
              }
              """,
            """
              package foo;
              
              import java.util.List;
              import java.util.regex.Pattern;
              
              public class Foo {
                  public void bar(List<Pattern> j) {
                  }
              }
              """
          )
        );
    }

    @Test
    void wildcard() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "java.util.List<?>", "j", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i) {
                  }
              }
              """,
            """
              package foo;
              
              import java.util.List;
              
              public class Foo {
                  public void bar(List<?> j) {
                  }
              }
              """
          )
        );
    }

    @Test
    void wildcardExtends() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "java.util.List<? extends Object>", "j", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i) {
                  }
              }
              """,
            """
              package foo;
              
              import java.util.List;
              
              public class Foo {
                  public void bar(List<? extends Object> j) {
                  }
              }
              """
          )
        );
    }

    @Test
    void string() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "String", "i", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int j) {
                  }
              
                  public void bar(int i) {
                  }
              
                  public void bar(int j, int k) {
                  }
              }
              """,
            """
              package foo;
              
              public class Foo {
                  public void bar(String i) {
                  }
              
                  public void bar(String i) {
                  }
              
                  public void bar(String i, int k) {
                  }
              }
              """
          )
        );
    }

    @Test
    void first() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "long", "k", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i) {
                  }
              
                  public void bar(int i, int j) {
                  }
              }
              """,
            """
              package foo;
              
              public class Foo {
                  public void bar(long k) {
                  }
              
                  public void bar(long k, int j) {
                  }
              }
              """
          )
        );
    }

    @Test
    void qualified() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "java.util.regex.Pattern", "p", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i) {
                  }
                  public void bar(int i, int j) {
                  }
              }
              """,
            """
              package foo;
              
              import java.util.regex.Pattern;
              
              public class Foo {
                  public void bar(Pattern p) {
                  }
              
                  public void bar(Pattern p, int j) {
                  }
              }
              """
          )
        );
    }

    @Test
    void object() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodParameter("foo.Foo#bar(..)", "Object", "o", 0)),
          java(
            """
              package foo;
              
              public class Foo {
                  public void bar(int i) {
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
