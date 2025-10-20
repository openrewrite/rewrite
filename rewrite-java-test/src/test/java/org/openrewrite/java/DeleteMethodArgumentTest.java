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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
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
         public static void foo(String s) {}
         public static void foo(java.util.List<?> l) {}
      }
      """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn(b));
    }

    @DocumentExample
    @Test
    void deleteMiddleArgumentDeclarative() {
        rewriteRun(
          spec -> spec.recipes(new DeleteMethodArgument("B foo(int, int, int)", 1)),
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
          java(
            "public class A {{ B.foo(0, 1, 2); }}",
            "public class A {{ B.foo(0); }}"
          )
        );
    }

    @Test
    void doNotDeleteEmptyContainingFormatting() {
        rewriteRun(
          spec -> spec.recipe(new DeleteMethodArgument("B foo(..)", 0)),
          java("public class A {{ B.foo( ); }}")
        );
    }

    @Test
    void insertEmptyWhenLastArgumentIsDeleted() {
        rewriteRun(
          spec -> spec.recipe(new DeleteMethodArgument("B foo(..)", 0)),
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
          java(
            "public class A { B b = new B(0); }",
            "public class A { B b = new B(); }"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4676")
    @Test
    void deleteFirstArgument() {
        rewriteRun(
          spec -> spec.recipe(new DeleteMethodArgument("B foo(int, int, int)", 0)),
          java(
            "public class A {{ B.foo(0, 1, 2); }}",
            "public class A {{ B.foo(1, 2); }}"
          ),
          java(
            "public class C {{ B.foo(\n\t\t0, 1, 2); }}",
            "public class C {{ B.foo(\n\t\t1, 2); }}"
          )
        );
    }

    @Test
    void deleteNullArgument() {
        rewriteRun(
          spec -> spec.recipe(new DeleteMethodArgument("B foo(String)", 0)),
          java(
            "public class A {{ B.foo(null); }}",
            "public class A {{ B.foo(); }}"
          )
        );
    }

    @Test
    void removeUnusedImportOfGenericType() {
        rewriteRun(
          spec -> spec.recipes(new DeleteMethodArgument("B foo(java.util.List)", 0)),
          java(
            """
              import java.util.ArrayList;

              class A {{ B.foo(new ArrayList<>()); }}
              """,
            """
              class A {{ B.foo(); }}
              """
          )
        );
    }

    @Test
    void removeUnusedImportOfClassOfStaticMethodWithoutAStaticImport() {
        rewriteRun(
          spec -> spec.recipes(new DeleteMethodArgument("B foo(java.util.List)", 0)),
          java(
            """
              import java.util.Collections;

              class A {{ B.foo(Collections.emptyList()); }}
              """,
            """
              class A {{ B.foo(); }}
              """
          )
        );
    }

    @Test
    void removeUnusedStaticMethodImport() {
        rewriteRun(
          spec -> spec.recipes(new DeleteMethodArgument("B foo(int)", 0)),
          java(
            """
              import static java.lang.Math.max;

              class A {{ B.foo(max(1,2)); }}
              """,
            """
              class A {{ B.foo(); }}
              """
          )
        );
    }

    @Test
    void removeUnusedStaticFieldImport() {
        rewriteRun(
          spec -> spec.recipes(new DeleteMethodArgument("B foo(int)", 0)),
          java(
            """
              import static java.lang.Integer.MAX_VALUE;

              class A {{ B.foo(MAX_VALUE); }}
              """,
            """
              class A {{ B.foo(); }}
              """
          )
        );
    }


    @Test
    void removeUnusedImportOfInnerClass() {
        rewriteRun(
          spec -> spec.recipes(new DeleteMethodArgument("B foo(java.util.List)", 0)),
          java(
            """
              import java.util.AbstractMap.SimpleEntry;
              import java.util.List;

              class A {{
                  B.foo(List.of(new SimpleEntry<>("a", "b")));
              }}
              """,
            """
              class A {{
                  B.foo();
              }}
              """
          )
        );
    }

    @Test
    void removeUnusedImportWithLambda() {
        rewriteRun(
          spec -> spec.recipes(new DeleteMethodArgument("B foo(java.util.List)", 0)),
          java(
            """
              import java.math.BigInteger;
              import java.util.stream.Collectors;
              import java.util.stream.Stream;

              class A {{
                  B.foo(Stream.of("23").map(s -> {
                      return new BigInteger(s).toString();
                  }).collect(Collectors.toList()));
              }}
              """,
            """
              class A {{
                  B.foo();
              }}
              """
          )
        );
    }
}
