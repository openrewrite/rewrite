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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class DeleteMethodArgumentTest implements RewriteTest {
    @Language("java")
    String b = """
      class B {
         public static void foo() {}
         public static void foo(int n) {}
         public static void foo(int n1, int n2) {}
         public static void foo(int n1, int n2, int n3) {}
         public B() {}
         public B(int n) {}
      }
      """;

    @Test
    void deleteMiddleArgumentDeclarative() {
        rewriteRun(
          spec -> spec.recipes(new DeleteMethodArgument("B foo(int, int, int)", 1)),
          java(b),
          java(
            "public class A {{ B.foo(0, 1, 2); }}",
            "public class A {{ B.foo(0, 2); }}"
          )
        );
    }

    @Test
    void deleteMiddleArgument() {
        rewriteRun(
          spec -> spec.recipe(new DeleteMethodArgument("B foo(int, int, int)", 1)),
          java(b),
          java(
            "public class A {{ B.foo(0, 1, 2); }}",
            "public class A {{ B.foo(0, 2); }}"
          )
        );
    }

    @Test
    void deleteArgumentsConsecutively() {
        rewriteRun(
          spec -> spec.recipes(
            new DeleteMethodArgument("B foo(int, int, int)", 1),
            new DeleteMethodArgument("B foo(int, int)", 1)
          ),
          java(b),
          java("public class A {{ B.foo(0, 1, 2); }}",
            "public class A {{ B.foo(0); }}"
          )
        );
    }

    @Test
    void doNotDeleteEmptyContainingFormatting() {
        rewriteRun(
          spec -> spec.recipe(new DeleteMethodArgument("B foo(..)", 0)),
          java(b),
          java("public class A {{ B.foo( ); }}")
        );
    }

    @Test
    void insertEmptyWhenLastArgumentIsDeleted() {
        rewriteRun(
          spec -> spec.recipe(new DeleteMethodArgument("B foo(..)", 0)),
          java(b),
          java(
            "public class A {{ B.foo(1); }}",
            "public class A {{ B.foo(); }}"
          )
        );
    }

    @Test
    void deleteConstructorArgument() {
        rewriteRun(
          spec -> spec.recipe(new DeleteMethodArgument("B <constructor>(int)", 0)),
          java(b),
          java(
            "public class A { B b = new B(0); }",
            "public class A { B b = new B(); }"
          )
        );
    }
}
