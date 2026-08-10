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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeMethodTargetToStaticTest implements RewriteTest {

    @Test
    void targetToStatic() {
        rewriteRun(
          spec -> spec.recipes(
              new ChangeMethodTargetToStatic("a.A nonStatic()", "b.B", null, null, false),
            new ChangeMethodName("b.B nonStatic()", "foo", null, null)
          ),
          java(
            """
              package a;
              public class A {
                 public void nonStatic() {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static void foo() {}
              }
              """
          ),
          java(
            """
              import a.*;
              class C {
                 public void test() {
                     new A().nonStatic();
                 }
              }
              """,
            """
              import b.B;

              class C {
                 public void test() {
                     B.foo();
                 }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2302")
    @Test
    void staticTargetToStatic() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A foo()", "b.B", null, null, false)),
          java(
            """
              package b;
              public class B {
                 public static void foo() {}
              }
              """
          ),
          java(
            """
              package a;
              public class A {
                 public static void foo() {}
              }
              """
          ),
          java(
            """
              import static a.A.foo;

              class C {
                 public void test() {
                     foo();
                 }
              }
              """,
            """
              import static b.B.foo;

              class C {
                 public void test() {
                     foo();
                 }
              }
              """
          )
        );
    }

    @Test
    void targetToStaticWhenMethodHasSameName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A method()", "a.A", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public void method() {}
              }
              """
          ),
          java(
            """
              import a.A;
              class Test {
                 public void test() {
                     new A().method();
                 }
              }
              """,
            """
              import a.A;
              class Test {
                 public void test() {
                     A.method();
                 }
              }
              """
          )
        );
    }

    @Test
    void staticMethodCalledOnInstanceToCallOnClass() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A method()", "a.A", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public static void method() {}
              }
              """
          ),
          java(
            """
              import a.A;
              class Test {
                 public void test() {
                     A.method();
                     new A().method();
                     A a = new A();
                     a.method();
                 }
              }
              """,
            """
              import a.A;
              class Test {
                 public void test() {
                     A.method();
                     A.method();
                     A a = new A();
                     A.method();
                 }
              }
              """
          )
        );
    }

    @Test
    void receiverMethodCallIsNotDropped() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A nonStatic()", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public void nonStatic() {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static void nonStatic() {}
              }
              """
          ),
          java(
            """
              import a.A;

              class C {
                 int calls;

                 A receiver() {
                     calls++;
                     return new A();
                 }

                 public void test() {
                     receiver().nonStatic();
                 }
              }
              """
          )
        );
    }

    @Test
    void receiverExpressionsThatCanThrowAreNotDropped() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A nonStatic()", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public void nonStatic() {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static void nonStatic() {}
              }
              """
          ),
          java(
            """
              import a.A;

              class C {
                 A field = new A();
                 A[] array = new A[1];

                 public void test(C other, Object o) {
                     other.field.nonStatic();
                     array[0].nonStatic();
                     ((A) o).nonStatic();
                 }
              }
              """
          )
        );
    }

    @Test
    void volatileFieldReceiverIsNotDropped() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A nonStatic()", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public void nonStatic() {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static void nonStatic() {}
              }
              """
          ),
          java(
            """
              import a.A;

              class C {
                 volatile A shared = new A();

                 public void test() {
                     shared.nonStatic();
                 }
              }
              """
          )
        );
    }

    @Test
    void instantiationArgumentsAreNotDropped() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A nonStatic()", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public A() {}
                 public A(String s) {}
                 public void nonStatic() {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static void nonStatic() {}
              }
              """
          ),
          java(
            """
              import a.A;

              class C {
                 int calls;

                 String argument() {
                     calls++;
                     return "a";
                 }

                 public void test() {
                     new A("a").nonStatic();
                     new A(argument()).nonStatic();
                 }
              }
              """,
            """
              import a.A;
              import b.B;

              class C {
                 int calls;

                 String argument() {
                     calls++;
                     return "a";
                 }

                 public void test() {
                     B.nonStatic();
                     new A(argument()).nonStatic();
                 }
              }
              """
          )
        );
    }

    @Test
    void variableReceiversAreDropped() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A nonStatic()", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public void nonStatic() {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static void nonStatic() {}
              }
              """
          ),
          java(
            """
              import a.A;

              class C {
                 A field = new A();

                 public void test(A parameter) {
                     A local = new A();
                     local.nonStatic();
                     parameter.nonStatic();
                     field.nonStatic();
                 }
              }
              """,
            """
              import a.A;
              import b.B;

              class C {
                 A field = new A();

                 public void test(A parameter) {
                     A local = new A();
                     B.nonStatic();
                     B.nonStatic();
                     B.nonStatic();
                 }
              }
              """
          )
        );
    }

    @Test
    void chainedSelfCallsCollapseOntoTargetType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A value()", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public A value() { return this; }
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static String value() { return "b"; }
              }
              """
          ),
          java(
            """
              import a.A;

              class C {
                 public void test(A legacy) {
                     legacy.value().value();
                     legacy.value().value().value();
                 }
              }
              """,
            """
              import a.A;
              import b.B;

              class C {
                 public void test(A legacy) {
                     B.value();
                     B.value();
                 }
              }
              """
          )
        );
    }

    @Test
    void chainedCallOnRewrittenStaticFactoryCollapses() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A *(..)", "b.B", "java.util.List", null, false)),
          java(
            """
              package a;
              import java.util.List;
              public class A {
                 public static A of(String s) { return new A(); }
                 public List<String> reverse() { return null; }
              }
              """
          ),
          java(
            """
              package b;
              import java.util.List;
              public class B {
                 public static List<String> of(String s) { return null; }
                 public static List<String> reverse() { return null; }
              }
              """
          ),
          java(
            """
              import a.A;

              class C {
                 public void test() {
                     A.of("x").reverse();
                 }
              }
              """,
            """
              import b.B;

              class C {
                 public void test() {
                     B.reverse();
                 }
              }
              """
          )
        );
    }

    @Test
    void chainedCallOnUndiscardableReceiverIsNotChanged() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A value()", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public A value() { return this; }
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static String value() { return "b"; }
              }
              """
          ),
          java(
            """
              import a.A;

              class C {
                 int calls;

                 A receiver() {
                     calls++;
                     return new A();
                 }

                 public void test() {
                     receiver().value().value();
                 }
              }
              """
          )
        );
    }

    @Test
    void thisReceiversAreReplaced() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A value()", "b.B", null, null, false)),
          java(
            """
              package b;
              public class B {
                 public static String value() { return "b"; }
              }
              """
          ),
          java(
            """
              package a;

              import java.util.function.Supplier;

              public class A {
                 public String value() { return "a"; }

                 public Supplier<String> direct() {
                     return this::value;
                 }

                 class Inner {
                     public Supplier<String> qualified() {
                         return A.this::value;
                     }

                     public String call() {
                         return A.this.value();
                     }
                 }
              }
              """,
            """
              package a;

              import b.B;

              import java.util.function.Supplier;

              public class A {
                 public String value() { return "a"; }

                 public Supplier<String> direct() {
                     return B::value;
                 }

                 class Inner {
                     public Supplier<String> qualified() {
                         return B::value;
                     }

                     public String call() {
                         return B.value();
                     }
                 }
              }
              """
          )
        );
    }

    @Test
    void memberReferenceOnMethodCallIsNotChanged() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A value()", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public String value() { return "a"; }
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static String value() { return "b"; }
              }
              """
          ),
          java(
            """
              import a.A;

              import java.util.function.Supplier;

              class C {
                 int calls;

                 A receiver() {
                     calls++;
                     return new A();
                 }

                 public Supplier<String> test() {
                     return receiver()::value;
                 }
              }
              """
          )
        );
    }

    @Test
    void memberReferenceOnVariableIsNotChanged() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A value()", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public String value() { return "a"; }
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static String value() { return "b"; }
              }
              """
          ),
          java(
            """
              import a.A;

              import java.util.function.Supplier;

              class C {
                 public Supplier<String> test(A a) {
                     return a::value;
                 }
              }
              """
          )
        );
    }

    @Test
    void memberReferenceOnRewrittenCallCollapses() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A *(..)", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public A self() { return this; }
                 public String value() { return "a"; }
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static B self() { return new B(); }
                 public static String value() { return "b"; }
              }
              """
          ),
          java(
            """
              import a.A;

              import java.util.function.Supplier;

              class C {
                 public Supplier<String> test(A legacy) {
                     return legacy.self()::value;
                 }
              }
              """,
            """
              import a.A;
              import b.B;

              import java.util.function.Supplier;

              class C {
                 public Supplier<String> test(A legacy) {
                     return B::value;
                 }
              }
              """
          )
        );
    }

    @Test
    void memberReferenceTargetToStatic() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A of(..)", "b.B", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public static String of(String s) { return s; }
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static String of(String s) { return s; }
              }
              """
          ),
          java(
            """
              import a.A;
              import java.util.stream.Stream;

              class Test {
                 public void test() {
                     Stream.of("a", "b").map(A::of);
                 }
              }
              """,
            """
              import b.B;

              import java.util.stream.Stream;

              class Test {
                 public void test() {
                     Stream.of("a", "b").map(B::of);
                 }
              }
              """
          )
        );
    }

    @Test
    void memberReferenceAlreadyOnTargetType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A of(..)", "a.A", null, null, false)),
          java(
            """
              package a;
              public class A {
                 public static String of(String s) { return s; }
              }
              """
          ),
          java(
            """
              import a.A;
              import java.util.stream.Stream;

              class Test {
                 public void test() {
                     Stream.of("a", "b").map(A::of);
                 }
              }
              """
          )
        );
    }

    @Test
    void memberReferenceWithReturnTypeChange() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("a.A of(..)", "b.B", "java.lang.CharSequence", null, false)),
          java(
            """
              package a;
              public class A {
                 public static String of(String s) { return s; }
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static CharSequence of(String s) { return s; }
              }
              """
          ),
          java(
            """
              import a.A;
              import java.util.stream.Stream;

              class Test {
                 public void test() {
                     Stream.of("a", "b").map(A::of);
                 }
              }
              """,
            """
              import b.B;

              import java.util.stream.Stream;

              class Test {
                 public void test() {
                     Stream.of("a", "b").map(B::of);
                 }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1804")
    @Test
    void constructorToStaticMethod() {
        rewriteRun(
          spec -> spec.recipes(
            new ChangeMethodTargetToStatic("a.A <constructor>(String)", "b.B", "b.B", null, false),
            new ChangeMethodName("b.B A(String)", "foo", null, null)
          ),
          java(
            """
              package a;
              public class A {
                 public A(String s) {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static B foo(String s) { return null; }
              }
              """
          ),
          java(
            """
              import a.A;
              class C {
                 public void test() {
                     new A("hello");
                 }
              }
              """,
            """
              import b.B;

              class C {
                 public void test() {
                     B.foo("hello");
                 }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1804")
    @Test
    void constructorToStaticMethodWithReturnType() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeMethodTargetToStatic("a.A <constructor>(String)", "b.B", "b.B", null, false)
          ),
          java(
            """
              package a;
              public class A {
                 public A(String s) {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static B A(String s) { return null; }
              }
              """
          ),
          java(
            """
              import a.A;
              class C {
                 public void test() {
                     new A("hello");
                 }
              }
              """,
            """
              import b.B;

              class C {
                 public void test() {
                     B.A("hello");
                 }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1804")
    @Test
    void constructorWithAnonymousBodyNotChanged() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeMethodTargetToStatic("a.A <constructor>(String)", "b.B", "b.B", null, false)
          ),
          java(
            """
              package a;
              public class A {
                 public A(String s) {}
                 public void doSomething() {}
              }
              """
          ),
          java(
            """
              import a.A;
              class C {
                 public void test() {
                     new A("hello") {
                         @Override
                         public void doSomething() {}
                     };
                 }
              }
              """
          )
        );
    }

    @Test
    void constructorNotChangedWhenMethodPatternIsWildcard() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeMethodTargetToStatic("a.A *(..)", "b.B", null, null, false)
          ),
          java(
            """
              package a;
              public class A {
                 public A() {}
                 public void doSomething() {}
              }
              """
          ),
          java(
            """
              package b;
              public class B {
                 public static void doSomething() {}
              }
              """
          ),
          java(
            """
              import a.A;
              class C {
                 public void test() {
                     A a = new A();
                     a.doSomething();
                 }
              }
              """,
            """
              import a.A;
              import b.B;
              
              class C {
                 public void test() {
                     A a = new A();
                     B.doSomething();
                 }
              }
              """
          )
        );
    }

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/3085")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void keepImportComments() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodTargetToStatic("org.codehaus.plexus.util.StringUtils isBlank(String)", "org.openrewrite.internal.StringUtils", null, null, false)),
          java(
            """
              package org.codehaus.plexus.util;

              public class StringUtils {
                 public boolean isBlank(String s) {
                     s.isBlank();
                 }
              }
              """
          ),
          java(
            """
              package a;

              /*
               * This is a comment
               */

              import org.codehaus.plexus.util.StringUtils;
              import java.util.UUID;

              class Test {
                 public void test() {
                     StringUtils.isBlank("x");
                 }
              }
              """,
            """
              package a;

              /*
               * This is a comment
               */

              import org.openrewrite.internal.StringUtils;
              import java.util.UUID;

              class Test {
                 public void test() {
                     StringUtils.isBlank("x");
                 }
              }
              """
          )
        );
    }
}
