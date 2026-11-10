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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReorderMethodArgumentsTest implements RewriteTest {

    @Test
    void reorderArguments() {
        rewriteRun(
          spec -> spec.recipes(
            new ReorderMethodArguments("a.A foo(String, Integer, Integer)",
              new String[]{"n", "m", "s"}, null, null, null)),
          java(
            """
              package a;
              public class A {
                 public void foo(String s, Integer m, Integer n) {}
                 public void foo(Integer n, Integer m, String s) {}
              }
              """
          ),
          java(
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo(
                         "mystring",
                         1,
                         2
                     );
                 }
              }
              """,
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo(
                         2,
                         1,
                         "mystring"
                     );
                 }
              }
              """
          )
        );
    }

    @Test
    void reorderArgumentsWithNoSourceAttachment() {
        rewriteRun(
          spec -> spec.recipe(new ReorderMethodArguments("a.A foo(String,..)",
            new String[]{"s", "n"}, new String[]{"n", "s"}, null, null)),
          java(
            """
              package a;
              public class A {
                 public void foo(String arg0, Integer... arg1) {}
                 public void foo(Integer arg0, Integer arg1, String arg2) {}
              }
              """
          ),
          java(
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo("s", 0, 1);
                 }
              }
              """,
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo(0, 1, "s");
                 }
              }
              """
          )
        );
    }

    @Test
    void reorderArgumentsWhereOneOfTheOriginalArgumentsIsVararg() {
        rewriteRun(
          spec -> spec.recipe(new ReorderMethodArguments("a.A foo(String,Integer,..)",
            new String[]{"s", "o", "n"}, null, null, null)),
          java(
            """
              package a;
              public class A {
                 public void foo(String s, Integer n, Object... o) {}
                 // what the reordered call invokes, as no method takes varargs before another parameter
                 public void foo(String s, Object o1, Object o2, Integer n) {}
              }
              """
          ),
          java(
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo("mystring", 0, "a", "b");
                 }
              }
              """,
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo("mystring", "a", "b", 0);
                 }
              }
              """
          )
        );
    }

    @Test
    void reorderASingleVarargsArgumentBeforeAnother() {
        rewriteRun(
          spec -> spec.recipe(new ReorderMethodArguments("a.A foo(String,..)",
            new String[]{"o", "s"}, null, null, null)),
          java(
            """
              package a;
              public class A {
                 public void foo(String s, Object... o) {}
                 public void foo(Object o, String s) {}
              }
              """
          ),
          java(
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo("mystring", 0);
                 }
              }
              """,
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo(0, "mystring");
                 }
              }
              """
          )
        );
    }

    @Test
    void reorderArgumentsBeforeVarargs() {
        rewriteRun(
          spec -> spec.recipe(new ReorderMethodArguments("a.A foo(Integer,String,..)",
            new String[]{"s", "n", "o"}, null, null, null)),
          java(
            """
              package a;
              public class A {
                 public void foo(Integer n, String s, Object... o) {}
                 public void foo(String s, Integer n, Object... o) {}
              }
              """
          ),
          java(
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo(0, "mystring", "a", "b");
                 }
              }
              """,
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo("mystring", 0, "a", "b");
                 }
              }
              """
          )
        );
    }

    @Test
    void reorderWithFewerOldParameterNamesThanTheVarargsMethodHas() {
        rewriteRun(
          spec -> spec.recipe(new ReorderMethodArguments("a.A foo(Integer,String,..)",
              new String[]{"s", "n"}, new String[]{"n", "s"}, null, null))
            // the reordered call's type leaves out the parameter that the old parameter names don't name
            .recipeQualityValidation(RecipeQualityValidation.builder().parserTypedTrees(false).build()),
          java(
            """
              package a;
              public class A {
                 public void foo(Integer n, String s, Object... o) {}
                 public void foo(String s, Integer n, Object... o) {}
              }
              """
          ),
          java(
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo(0, "mystring");
                 }
              }
              """,
            """
              import a.*;
              public class B {
                 A a;
                 public void test() {
                     a.foo("mystring", 0);
                 }
              }
              """
          )
        );
    }

    @Test
    void reorderArgumentsWhereTheLastArgumentIsVarargAndNotPresentInInvocation() {
        rewriteRun(
          spec -> spec.recipe(new ReorderMethodArguments("a.A foo(..)",
            new String[]{"o", "s"}, null, null, null)),
          java(
            """
              package a;
              public class A {
                 public void foo(String s, Object... o) {}
              }
              """
          ),
          java(
            """
              import a.*;
              public class B {
                 public void test() {
                     new A().foo("mystring");
                 }
              }
              """
          )
        );
    }

    @Test
    void reorderArgumentsInConstructors() {
        rewriteRun(
          spec -> spec.recipes(
            new ReorderMethodArguments("a.A <constructor>(String, Integer, Integer)",
              new String[]{"n", "m", "s"}, null, null, null)),
          java(
            """
              package a;
              public class A {
                 public A(String s, Integer m, Integer n) {}
                 public A(Integer n, Integer m, String s) {}
              }
              """
          ),
          java(
            """
              import a.*;
              public class B {
                 public void test() {
                     A a = new A(
                         "mystring",
                         1,
                         2
                     );
                 }
              }
              """,
            """
              import a.*;
              public class B {
                 public void test() {
                     A a = new A(
                         2,
                         1,
                         "mystring"
                     );
                 }
              }
              """
          )
        );
    }

}
