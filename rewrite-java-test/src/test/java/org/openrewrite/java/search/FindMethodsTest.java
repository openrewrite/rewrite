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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.table.MethodCalls;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("RedundantOperationOnEmptyContainer")
class FindMethodsTest implements RewriteTest {

    @DocumentExample
    @Test
    void findConstructors() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("A <constructor>(String)", false)),
          java(
            """
              class Test {
                  A a = new A("test");
              }
              """,
            """
              class Test {
                  A a = /*~~>*/new A("test");
              }
              """
          ),
          java(
            """
              class A {
                  public A(String s) {}
              }
              """
          )
        );
    }

    @Test
    void findMethodReferences() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("A singleArg(String)", false)),
          java(
            """
              class Test {
                  void test() {
                      new java.util.ArrayList<String>().forEach(new A()::singleArg);
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      new java.util.ArrayList<String>().forEach(new A()::/*~~>*/singleArg);
                  }
              }
              """
          ),
          java(
            """
              class A {
                  public void singleArg(String s) {}
              }
              """
          )
        );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void findOverriddenMethodReferences() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("java.util.Collection isEmpty()", true)),
          java(
            """
              class Test {
                  void test() {
                      new java.util.ArrayList<String>().isEmpty();
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      /*~~>*/new java.util.ArrayList<String>().isEmpty();
                  }
              }
              """
          )
        );
    }

    @Test
    void findStaticMethodCalls() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("java.util.Collections emptyList()", false)),
          java(
            """
              import java.util.Collections;
              public class A {
                 Object o = Collections.emptyList();
              }
              """,
            """
              import java.util.Collections;
              public class A {
                 Object o = /*~~>*/Collections.emptyList();
              }
              """
          )
        );
    }

    @Test
    void findStaticallyImportedMethodCalls() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("java.util.Collections emptyList()", false)),
          java(
            """
              import static java.util.Collections.emptyList;
              public class A {
                 Object o = emptyList();
              }
              """,
            """
              import static java.util.Collections.emptyList;
              public class A {
                 Object o = /*~~>*/emptyList();
              }
              """
          )
        );
    }

    @Test
    void matchVarargs() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("A foo(String, Object...)", false)),
          java(
            """
              public class B {
                 public void test() {
                     new A().foo("s", "a", 1);
                 }
              }
              """,
            """
              public class B {
                 public void test() {
                     /*~~>*/new A().foo("s", "a", 1);
                 }
              }
              """
          ),
          java(
            """
              public class A {
                  public void foo(String s, Object... o) {}
              }
              """
          )
        );
    }

    @Test
    void matchOnInnerClass() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("B.C foo()", false)),
          java(
            """
              public class A {
                 void test() {
                     new B.C().foo();
                 }
              }
              """,
            """
              public class A {
                 void test() {
                     /*~~>*/new B.C().foo();
                 }
              }
              """
          ),
          java(
            """
              public class B {
                 public static class C {
                     public void foo() {}
                 }
              }
              """
          )
        );
    }

    @Test
    void datatableFormat() {
        rewriteRun(
          spec -> spec.dataTableAsCsv(MethodCalls.class.getName(),
            """
              sourceFile,method,className,methodName,argumentTypes
              A.java,"new B.C().foo(bar, 123)","B$C",foo,"java.lang.String, int"
              """
          ).recipe(new FindMethods("B.C foo(..)", false)),
          java(
            """
              public class B {
                 public static class C {
                     public void foo(String bar, int baz) {}
                 }
              }
              """
          ),
          java(
            """
              public class A {
                   void test(String bar) {
                       new B.C().foo(bar, 123);
                   }
              }
              """,
            """
              public class A {
                   void test(String bar) {
                       /*~~>*/new B.C().foo(bar, 123);
                   }
              }
              """
          )
        );
    }

    @Test
    void findAnnotationMethod() {
        rewriteRun(
          spec -> spec.recipe(new FindMethods("Example description(..)", false)),
          java(
            """
              public @interface Example {
                  String name() default "";
              
                  String description() default "";
              }
              """
          ),
          java(
            """
              @Example(description = "test")
              public class A {
              }
              """,
            """
              @Example(/*~~>*/description = "test")
              public class A {
              }
              """
          )
        );
    }
}
