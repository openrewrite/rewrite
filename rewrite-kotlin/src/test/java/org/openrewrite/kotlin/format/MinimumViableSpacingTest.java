/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("All")
class MinimumViableSpacingTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(
          toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                  if (ctx.getCycle() == 1) {
                      return space.withWhitespace("");
                  }
                  return space;
              }
          }),
          toRecipe(() -> new MinimumViableSpacingVisitor<>(null))
        );
    }

    @DocumentExample
    @Test
    void classDeclaration() {
        rewriteRun(
          kotlin(
            """
              class A {
              }
              """,
            """
              class A{}
              """
          )
        );
    }


    @Test
    void classDeclarationWithFinalModifier() {
        rewriteRun(
          kotlin(
            """
              private    final    class A {
              }
              """,
            """
              private final class A{}
              """
          )
        );
    }

    @Test
    void classDeclarationWithModifier() {
        rewriteRun(
          kotlin(
            """
              private    class A {
              }
              """,
            """
              private class A{}
              """
          )
        );
    }

    @Test
    void method() {
        rewriteRun(
          kotlin(
            """
              class A {
                  fun <T> foo() {
                  }
              }
              """,
            """
              class A{fun <T> foo(){}}
              """
          )
        );
    }

    @Test
    void returnExpression() {
        rewriteRun(
          kotlin(
            """
              class A {
                  fun foo() :   String {
                      return "foo"
                  }
              }
              """,
            """
              class A{fun foo():String{return "foo"}}
              """
          )
        );
    }

    @Test
    void trailingLambda() {
        rewriteRun(
          kotlin(
            """
              val x = "foo".let {}
              """,
            """
              val x="foo".let{}
              """
          )
        );
    }

    @Test
    void ifElse() {
        rewriteRun(
          kotlin(
            """
              fun method(a: Int, b: Int) {
                  val max = if (a > b) a else   b
              }
              """,
            """
              fun method(a:Int,b:Int){val max=if(a>b)a else b}
              """
          )
        );
    }

    @Test
    void variableDeclaration() {
        rewriteRun(
          kotlin(
            """
              val zero: Int = 0
              """,
            """
              val zero:Int=0
              """
          )
        );
    }

    @Test
    void variableDeclarations() {
        rewriteRun(
          kotlin(
            """
              val zero: Int = 0
                  var one: Int = 1
              """,
            """
              val zero:Int=0
              var one:Int=1
              """
          )
        );
    }

    @Test
    void variableDeclarationsInClass() {
        rewriteRun(
          kotlin(
            """
              class A {
                  val zero: Int = 0
                  var one: Int = 1
              }
              """,
            """
              class A{val zero:Int=0
              var one:Int=1}
              """
          )
        );
    }

    @Test
    void variableDeclarationsInClass2() {
        rewriteRun(
          spec -> spec.recipes(
            toRecipe(() -> new MinimumViableSpacingVisitor<>())
          ),
          kotlin(
            """
              class A {
                  val zero: Int = 0
                  var one: Int = 1
              }
              """
          )
        );
    }


    @Test
    void variableDeclarationsInMethod() {
        rewriteRun(
          kotlin(
            """
              class A {
                  fun foo(paramA : Int, paramB : Int) {
                      val unassigned:Int
                      var a = 1
                      val b = 2
                  }
              }
              """,
            """
              class A{fun foo(paramA:Int,paramB:Int){val unassigned:Int
              var a=1
              val b=2}}
              """
          )
        );
    }

    @Test
    void variableDeclarationsWithIn() {
        rewriteRun(
          kotlin(
            """
              fun foo(arr: IntArray) {
                  var x = 1 in arr
              }
              """,
            """
              fun foo(arr:IntArray){var x=1 in arr}
              """
          )
        );
    }

    @Test
    void forloop() {
        rewriteRun(
          kotlin(
            """
              fun foo(arr: IntArray) {
                  for (i in arr) {
                  }
              }
              """,
            """
              fun foo(arr:IntArray){for(i in arr){}}
              """
          )
        );
    }

    @Test
    void variableDeclarationsInForLoops() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun foo(arr: IntArray) {
                      for (n in 0..arr.size) {
                      }

                      for (i in arr) {
                      }

                      for (i: Int in arr) {
                      }
                  }
              }
              """,
            """
              class Test{fun foo(arr:IntArray){for(n in 0..arr.size){}
              for(i in arr){}
              for(i:Int in arr){}}}
              """
          )
        );
    }

    @Test
    void noSpaceAferAnnotation() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpath("junit-jupiter-api"))
            .recipes(
              toRecipe(() -> new MinimumViableSpacingVisitor<>())
            ),
          kotlin(
            """
              import org.junit.jupiter.api.Test

              class A {
                  @Test
                  fun testA() {
                  }
              }
              """
          )
        );
    }

    @Test
    void classConstructor() {
        rewriteRun(
          spec -> spec.recipes(
            toRecipe(() -> new MinimumViableSpacingVisitor<>())
          ),
          kotlin(
            """
              package com.netflix.graphql.dgs.client.codegen

              class BaseProjectionNode (
                      val type: Int = 1
                      ) {
              }
              """
          )
        );
    }

    @Test
    void spaceAfterPublic() {
        rewriteRun(
          spec -> spec.recipes(
            toRecipe(() -> new MinimumViableSpacingVisitor<>())
          ),
          kotlin(
            """
              class A {
                  public fun me() {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/322")
    @Test
    void statementWithCommentInPrefix() {
        rewriteRun(
          spec -> spec.recipes(
            toRecipe(() -> new MinimumViableSpacingVisitor<>())
          ),
          kotlin(
            """
              fun x() {
                  "s".length; "s".length
                  // c1
                  "s".length
              }
              """
          )
        );
    }

    @Test
    void compilationUnitDeclarations() {
        rewriteRun(
          spec -> spec.recipes(
            toRecipe(() -> new MinimumViableSpacingVisitor<>())
          ),
          kotlin(
            """
              val one = 1
              val two = 2

              class Test
              """
          )
        );
    }

    @Test
    void blockWithImplicitReturn() {
        rewriteRun(
          spec -> spec.recipes(
            toRecipe(() -> new MinimumViableSpacingVisitor<>())
          ),
          kotlin(
            """
              val maybeOne = listOf(1, 2).firstOrNull {
                  print("Found $it")
                  it == 1
              }
              """
          )
        );
    }

    @Test
    void infixOperator() {
        rewriteRun(
          kotlin(
            """
              val l = listOf('a'to 1)
              """,
            """
              val l=listOf('a' to 1)
              """
          )
        );
    }

    @Test
    void doNotFoldImport() {
        rewriteRun(
          kotlin(
            """
              val a = when (5) {
                  null, !in listOf(1, 2) -> 3
                  else -> 4
              }
              """,
            """
              val a=when(5){null,!in listOf(1,2)->3
              else->4}
              """
          )
        );
    }

    @Test
    void propertyName() {
        rewriteRun(
          kotlin(
            """
              val containingFiles: Int
                  get() = 1
              """,
            "val containingFiles:Int get()=1"
          )
        );
    }
}
