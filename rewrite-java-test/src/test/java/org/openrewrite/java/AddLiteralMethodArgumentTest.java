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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddLiteralMethodArgumentTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn("""
          class B {
             static void foo() {}
             static void foo(int n) {}
             static void foo(int n1, int n2) {}
             static void foo(int n1, int n2, int n3) {}
             static void bar(String n) {}
             static void bar(String n1, String n2) {}
             static void bar(String n1, String n2, String n3) {}
             static void baz(String n1) {}
             static void baz(String n1, boolean n2) {}
             static void baz(String n1, boolean n2, int n3) {}
             static void baz(String n1, boolean n2, int n3, long n4) {}
             static void baz(String n1, boolean n2, int n3, long n4, double n5) {}
             static void baz(String n1, boolean n2, int n3, long n4, double n5, float n6) {}
             static void baz(String n1, boolean n2, int n3, long n4, double n5, float n6, short n7) {}
             static void baz(String n1, boolean n2, int n3, long n4, double n5, float n6, short n7, char n8) {}
             B() {}
             B(int n) {}
          }
          """));
    }

    @Test
    void addToMiddleArgument() {
        rewriteRun(
          spec -> spec.recipe(new AddLiteralMethodArgument("B foo(int, int)", 1, -1, "int")),
          java(
            "class A {{ B.foo(0, 1); }}",
            "class A {{ B.foo(0, -1, 1); }}"
          )
        );
    }

    @Test
    void addToMiddleArgumentAsString() {
        rewriteRun(
          spec -> spec.recipe(new AddLiteralMethodArgument("B foo(int, int)", 1, "-1", "int")),
          java(
            "class A {{ B.foo(0, 1); }}",
            "class A {{ B.foo(0, -1, 1); }}"
          )
        );
    }

    @Test
    void addToMiddleStringArgument() {
        rewriteRun(
          spec -> spec.recipe(new AddLiteralMethodArgument("B bar(String, String)", 1, "-1", "String")),
          java(
            "class A {{ B.bar(\"0\", \"1\"); }}",
            "class A {{ B.bar(\"0\", \"-1\", \"1\"); }}"
          )
        );
    }

    @Test
    void addArgumentsConsecutively() {
        rewriteRun(
          spec -> spec.recipes(
            new AddLiteralMethodArgument("B foo(int)", 1, 1, "int"),
            new AddLiteralMethodArgument("B foo(int, int)", 2, "2", "int")
          ),
          java(
            "class A {{ B.foo(0); }}",
            "class A {{ B.foo(0, 1, 2); }}"
          )
        );
    }

    @Test
    void addToConstructorArgument() {
        rewriteRun(
          spec -> spec.recipe(new AddLiteralMethodArgument("B <constructor>()", 0, 1, "int")),
          java(
            "class A { B b = new B(); }",
            "class A { B b = new B(1); }"
          )
        );
    }

    @Test
    void addOtherTypes() {
        rewriteRun(
          spec -> spec.recipes(
            new AddLiteralMethodArgument("B baz(String)", 1, true, "boolean"),
            new AddLiteralMethodArgument("B baz(String, boolean)", 2, 1, "int"),
            new AddLiteralMethodArgument("B baz(String, boolean, int)", 3, "2L", "long"),
            new AddLiteralMethodArgument("B baz(String, boolean, int, long)", 4, 2.5, "double"),
            new AddLiteralMethodArgument("B baz(String, boolean, int, long, double)", 5, "3.5f", "float"),
            new AddLiteralMethodArgument("B baz(String, boolean, int, long, double, float)", 6, 32767, "short"),
            new AddLiteralMethodArgument("B baz(String, boolean, int, long, double, float, short)", 7, 'c', "char")
          ),
          java(
            "class A {{ B.baz(\"hi\"); }}",
            "class A {{ B.baz(\"hi\", true, 1, 2L, 2.5, 3.5f, 32767, 'c'); }}"
          )
        );
    }

}
