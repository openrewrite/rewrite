/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class CompilationUnitTest implements RewriteTest {

    @Test
    void packageWithSemicolonBeforeImport() {
        rewriteRun(
          scala(
            """
            package foo;import bar.X
            """
          )
        );
    }

    @Test
    void packageWithSemicolonBeforeStatement() {
        rewriteRun(
          scala(
            """
            package foo;class Bar
            """
          )
        );
    }

    @Test
    void emptyFile() {
        rewriteRun(
          scala("")
        );
    }

    @Test
    void singleStatement() {
        rewriteRun(
          scala("val x = 42")
        );
    }

    @Test
    void withPackage() {
        rewriteRun(
          scala(
            """
            package com.example
            
            val x = 42
            """
          )
        );
    }

    @Test
    void withNestedPackage() {
        rewriteRun(
          scala(
            """
            package com.example.scala
            
            val message = "Hello"
            """
          )
        );
    }

    @Test
    void withImports() {
        rewriteRun(
          scala(
            """
            package com.example
            
            import scala.collection.mutable
            import java.util.List
            
            val x = 42
            """
          )
        );
    }

    @Test
    void multipleStatements() {
        rewriteRun(
          scala(
            """
            val x = 1
            val y = 2
            val z = x + y
            """
          )
        );
    }

    @Test
    void withComments() {
        rewriteRun(
          scala(
            """
            // This is a comment
            val x = 42
            
            /* Multi-line
               comment */
            val y = 84
            """
          )
        );
    }

    @Test
    void withDocComment() {
        rewriteRun(
          scala(
            """
            /** This is a doc comment
              * for the value below
              */
            val important = 42
            """
          )
        );
    }

    @Test
    void multipleFilesGetDistinctPaths() {
        // Regression: sourcePathFromSourceText used to hard-code "file.scala" which caused
        // multi-file tests to collide on the same path and garble each other on print.
        rewriteRun(
          scala(
            """
            package com.example

            class A {
              def use(b: com.other.B): String = b.value
            }
            """
          ),
          scala(
            """
            package com.other

            class B {
              val value: String = "hi"
            }
            """
          )
        );
    }

    @Test
    void packageObjectWithMembers() {
        rewriteRun(
          scala(
            """
            package com.example

            package object pkg {
              type Alias = String

              val Constant: Int = 42
            }
            """,
            spec -> spec.path("package.scala")
          )
        );
    }

    @Test
    void packageWithBraces() {
        rewriteRun(
          scala(
            """
            package foo.bar {
              val x = 42
            }
            """
          )
        );
    }

    @Test
    void indentedPackage() {
        rewriteRun(
          scala(
            """
            package com.example:
              val x = 42
            """
          )
        );
    }

    @Test
    void indentedIfThenElse() {
        rewriteRun(
          scala(
            """
            val x =
              if true then
                1
              else
                2
            """
          )
        );
    }

    @Test
    void indentedWhileDo() {
        rewriteRun(
          scala(
            """
            object O:
              var i = 0
              while i < 10 do
                i = i + 1
            """
          )
        );
    }

    @Test
    void indentedForYield() {
        rewriteRun(
          scala(
            """
            object O:
              val xs =
                for i <- 1 to 10
                yield i * 2
            """
          )
        );
    }

    @Test
    void indentedTryCatch() {
        rewriteRun(
          scala(
            """
            val x =
              try
                42
              catch
                case _: Exception => 0
            """
          )
        );
    }

    @Test
    void indentedMatch() {
        rewriteRun(
          scala(
            """
            val x = 1 match
              case 1 => "one"
              case _ => "other"
            """
          )
        );
    }

    @Test
    void withTrailingWhitespace() {
        rewriteRun(
          scala(
            """
            val x = 42


            """
          )
        );
    }

    @Test
    void foldLeftWithColonPartialFunction() {
        rewriteRun(
          scala(
            """
            val x = List(1, 2, 3).foldLeft(0):
              case (acc, n) if n > 0 => acc + n
              case (acc, _) => acc
            """
          )
        );
    }

    @Test
    void curriedMethodWithColonLambda() {
        rewriteRun(
          scala(
            """
            val x = List(1, 2, 3).foldLeft(0): (acc, n) =>
              acc + n
            """
          )
        );
    }

    @Test
    void chainedMethodWithColonLambda() {
        rewriteRun(
          scala(
            """
            val x = List(1, 2).map: i =>
              i + 1
            """
          )
        );
    }

    @Test
    void topLevelMethodWithColonLambda() {
        rewriteRun(
          scala(
            """
            def f(g: Int => Int): Int = g(1)
            val x = f: i =>
              i + 1
            """
          )
        );
    }

    @Test
    void chainedMethodWithColonPartialFunction() {
        rewriteRun(
          scala(
            """
            val x = List(1, 2).map:
              case 1 => "one"
              case _ => "other"
            """
          )
        );
    }

    @Test
    void colonArgWithMultilineBlock() {
        rewriteRun(
          scala(
            """
            def f(body: => Int): Int = body
            val x = f:
              val y = 1
              y + 2
            """
          )
        );
    }

    @Test
    void typeApplyMethodWithColonArg() {
        rewriteRun(
          scala(
            """
            def f[A](g: A => A): Int = 0
            val x = f[Int]:
              n => n + 1
            """
          )
        );
    }

    @Test
    void contextFunctionWithWildcardParam() {
        rewriteRun(
          scala(
            """
            val f: Int ?=> Int = _ ?=> 1
            """
          )
        );
    }

    @Test
    void contextFunctionWithNamedParam() {
        rewriteRun(
          scala(
            """
            val f: Int ?=> Int = x ?=> x + 1
            """
          )
        );
    }

    @Test
    void nestedColonArgLambdas() {
        rewriteRun(
          scala(
            """
            def f(g: Int => Int => Int): Int = 0
            val x = f: a =>
              b =>
                a + b
            """
          )
        );
    }

    @Test
    void classUsingAnonymousContextParameter() {
        rewriteRun(
          scala(
            """
            class Divider(using Executor)
            """
          )
        );
    }

}
