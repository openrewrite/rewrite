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
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ChangeLiteralTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new JavaVisitor<>() {
            @Override
            public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                doAfterVisit(new ChangeLiteral<>(literal, s -> s == null ? s : s.toString().replace("%s", "{}")));
                return super.visitLiteral(literal, ctx);
            }
        }));
    }

    @Language("java")
    String b = """
      package b;
      public class B {
         public void singleArg(String s) {}
      }
      """;

    @Test
    void changeStringLiteralArgument() {
        rewriteRun(
          java(b),
          java(
            """
              import b.*;
              class A {
                 public void test() {
                     String s = "bar";
                     new B().singleArg("foo (%s)" + s + 0L);
                 }
              }
              """,
            """
              import b.*;
              class A {
                 public void test() {
                     String s = "bar";
                     new B().singleArg("foo ({})" + s + 0L);
                 }
              }
              """
          )
        );
    }

    @Test
    void changeStringLiteralArgumentWithEscapableCharacters() {
        rewriteRun(
          java(b),
          java(
            """
              import b.*;
              public class A {
                  B b;
                  public void test() {
                      b.singleArg("mystring '%s'");
                  }
              }
              """,
            """
              import b.*;
              public class A {
                  B b;
                  public void test() {
                      b.singleArg("mystring '{}'");
                  }
              }
              """
          )
        );
    }
}
