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

class AddNullMethodArgumentTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn("""
          class B {
             static void foo() {}
             static void foo(Integer n) {}
             static void foo(Integer n1, Integer n2) {}
             static void foo(Integer n1, Integer n2, Integer n3) {}
             static void foo(Integer n1, Integer n2, Integer n3, Integer n4) {}
             static void foo(Integer n1, Integer n2, Integer n3, String n4) {}
             B() {}
             B(Integer n) {}
          }
          """));
    }

    @Test
    void addToMiddleArgument() {
        rewriteRun(
          spec -> spec.recipe(new AddNullMethodArgument("B foo(Integer, Integer)", 1, "java.lang.Integer", "n2", false)),
          java(
            "class A {{ B.foo(0, 1); }}",
            "class A {{ B.foo(0, null, 1); }}"
          )
        );
    }

    @Test
    void addArgumentsConsecutively() {
        rewriteRun(
          spec -> spec.recipes(
            new AddNullMethodArgument("B foo(Integer)", 1, "java.lang.Integer", "n2", false),
            new AddNullMethodArgument("B foo(Integer, Integer)", 1, "java.lang.Integer", "n2", false)
          ),
          java(
            "class A {{ B.foo(0); }}",
            "class A {{ B.foo(0, null, null); }}"
          )
        );
    }

    @Test
    void addToConstructorArgument() {
        rewriteRun(
          spec -> spec.recipe(new AddNullMethodArgument("B <constructor>()", 0, "java.lang.Integer", "arg", false)),
          java(
            "class A { B b = new B(); }",
            "class A { B b = new B(null); }"
          )
        );
    }

    @Test
    void addCastToConflictingArgumentType() {
        rewriteRun(
          spec -> spec.recipe(new AddNullMethodArgument("B foo(Integer,Integer,Integer)", 3, "java.lang.String", "n2", true)),
          java(
            "class A {{ B.foo(0, 1, 2); }}",
            "class A {{ B.foo(0, 1, 2, (java.lang.String) null); }}"
          )
        );
    }

}
