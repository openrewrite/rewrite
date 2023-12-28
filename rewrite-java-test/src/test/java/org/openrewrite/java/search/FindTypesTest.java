/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.search;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"RedundantThrows", "RedundantCast"})
class FindTypesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindTypes("a.A1", false));
    }

    @Language("java")
    String a1 = """
      package a;
      public class A1 extends Exception {
          public static void stat() {}
      }
      """;

    @DocumentExample
    @Test
    void simpleName() {
        rewriteRun(
          java(
            """
              import a.A1;
              public class B extends A1 {}
              """,
            """
              import a.A1;
              public class B extends /*~~>*/A1 {}
              """
          ),
          java(a1)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1466")
    @Test
    void wildcard() {
        rewriteRun(
          spec -> spec.recipe(new FindTypes("java.io..*", false)),
          java(
            """
              import java.io.File;
              public class Test {
                  File file;
              }
              """,
            """
              import java.io.File;
              public class Test {
                  /*~~>*/File file;
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedName() {
        rewriteRun(
          java(
            "public class B extends a.A1 {}",
            "public class B extends /*~~>*/a.A1 {}"
          ),
          java(a1)
        );
    }

    @Test
    void annotation() {
        rewriteRun(
          spec -> spec.recipe(new FindTypes("A1", false)),
          java(
            "@A1 public class B {}",
            "@/*~~>*/A1 public class B {}"
          ),
          java("public @interface A1 {}")
        );
    }

    @Test
    void array() {
        rewriteRun(
          java(
            """
              import a.A1;
              public class B {
                 A1[] a = new A1[0];
              }
              """,
            """
              import a.A1;
              public class B {
                 /*~~>*/A1[] a = new /*~~>*/A1[0];
              }
              """
          ),
          java(a1)
        );
    }

    @Test
    void multiDimensionalArray() {
        rewriteRun(
          java(
            """
              import a.A1;
              public class B {
                 A1[][] a = new A1[0][0];
              }
              """,
            """
              import a.A1;
              public class B {
                 /*~~>*/A1[][] a = new /*~~>*/A1[0][0];
              }
              """
          ),
          java(a1)
        );
    }

    @Test
    void classDecl() {
        rewriteRun(
          spec -> spec.recipes(
            new FindTypes("I1", false),
            new FindTypes("a.A1", false)
          ),
          java(
            """
              import a.A1;
              public class B extends A1 implements I1 {}
              """,
            """
              import a.A1;
              public class B extends /*~~>*/A1 implements /*~~>*/I1 {}
              """
          ),
          java(a1),
          java("public interface I1 {}")
        );
    }

    @Test
    void method() {
        rewriteRun(
          java(
            """
              import a.A1;
              public class B {
                 public A1 foo() throws A1 { return null; }
              }
              """,
            """
              import a.A1;
              public class B {
                 public /*~~>*/A1 foo() throws /*~~>*/A1 { return null; }
              }
              """
          ),
          java(a1)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-maven-plugin/issues/165")
    @Test
    void methodWithParameterizedType() {
        rewriteRun(
          spec -> spec.recipe(new FindTypes("java.util.List", false)),
          java(
            """
              import java.util.List;
              public class B {
                 public List<String> foo() { return null; }
              }
              """,
            """
              import java.util.List;
              public class B {
                 public /*~~>*/List<String> foo() { return null; }
              }
              """
          )
        );
    }

    @Test
    void methodInvocationTypeParametersAndWildcard() {
        rewriteRun(
          java(
            """
              import a.A1;
              import java.util.List;
              public class B {
                 public <T extends A1> T generic(T n, List<? super A1> in) { return null; }
                 public void test() {
                     A1.stat();
                     this.<A1>generic(null, null);
                 }
              }
              """,
            """
              import a.A1;
              import java.util.List;
              public class B {
                 public <T extends /*~~>*/A1> T generic(T n, List<? super /*~~>*/A1> in) { return null; }
                 public void test() {
                     /*~~>*/A1.stat();
                     this.</*~~>*/A1>generic(null, null);
                 }
              }
              """
          ),
          java(a1)
        );
    }

    @Test
    void multiCatch() {
        rewriteRun(
          java(
            """
              import a.A1;
              public class B {
                 public void test() {
                     try {}
                     catch(A1 | RuntimeException ignored) {}
                 }
              }
              """,
            """
              import a.A1;
              public class B {
                 public void test() {
                     try {}
                     catch(/*~~>*/A1 | RuntimeException ignored) {}
                 }
              }
              """
          ),
          java(a1)
        );
    }

    @Test
    void multiVariable() {
        rewriteRun(
          java(
            """
              import a.A1;
              public class B {
                 A1 f1, f2;
              }
              """,
            """
              import a.A1;
              public class B {
                 /*~~>*/A1 f1, f2;
              }
              """
          ),
          java(a1)
        );
    }

    @Test
    void newClass() {
        rewriteRun(
          java(
            """
              import a.A1;
              public class B {
                 A1 a = new A1();
              }
              """,
            """
              import a.A1;
              public class B {
                 /*~~>*/A1 a = new /*~~>*/A1();
              }
              """
          ),
          java(a1)
        );
    }

    @Test
    void parameterizedType() {
        rewriteRun(
          java(
            """
              import a.A1;
              import java.util.Map;
              public class B {
                 Map<A1, A1> m;
              }
              """,
            """
              import a.A1;
              import java.util.Map;
              public class B {
                 Map</*~~>*/A1, /*~~>*/A1> m;
              }
              """
          ),
          java(a1)
        );
    }

    @Test
    void assignableTypes() {
        rewriteRun(
          spec -> spec.recipe(new FindTypes("java.util.Collection", true)),
          java(
            """
              import java.util.List;
              public class B {
                 List<String> l;
              }
              """,
            """
              import java.util.List;
              public class B {
                 /*~~>*/List<String> l;
              }
              """
          )
        );
    }

    @Test
    void typeCast() {
        rewriteRun(
          java(
            """
              import a.A1;
              public class B {
                 A1 a = (A1) null;
              }
              """,
            """
              import a.A1;
              public class B {
                 /*~~>*/A1 a = (/*~~>*/A1) null;
              }
              """
          ),
          java(a1)
        );
    }

    @Test
    void classReference() {
        rewriteRun(
          java(
            """
              import a.A1;
              class B {
                  Class<?> clazz = A1.class;
              }
              """,
            """
              import a.A1;
              class B {
                  Class<?> clazz = /*~~>*/A1.class;
              }
              """
          ),
          java(a1)
        );
    }
}
