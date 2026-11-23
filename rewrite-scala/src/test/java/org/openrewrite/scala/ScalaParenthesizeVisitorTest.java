/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.scala;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScalaParenthesizeVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(Reparenthesize::new));
    }

    @DocumentExample
    @Test
    void operatorsRankByTheirFirstCharacter() {
        rewriteRun(
          scala(
            """
              object T {
                def f(a: Int, b: Int, c: Int, x: Boolean, y: Boolean, z: Boolean): Unit = {
                  val r1 = x && (y & z)
                  val r2 = (x & y) && z
                  val r3 = x || (y | z)
                  val r4 = (x ^ y) && z
                  val r5 = x | (y && z)
                  val r6 = a < (b << c)
                  val r7 = (a < b) == x
                  val r8 = (a + b) << c
                  val r9 = (a * b) + c
                  val r10 = a - (b - c)
                }
              }
              """,
            """
              object T {
                def f(a: Int, b: Int, c: Int, x: Boolean, y: Boolean, z: Boolean): Unit = {
                  val r1 = x && (y & z)
                  val r2 = (x & y) && z
                  val r3 = x || (y | z)
                  val r4 = (x ^ y) && z
                  val r5 = x | y && z
                  val r6 = a < (b << c)
                  val r7 = a < b == x
                  val r8 = a + b << c
                  val r9 = a * b + c
                  val r10 = a - (b - c)
                }
              }
              """
          )
        );
    }

    @Test
    void infixCallsAndPrefixOperators() {
        rewriteRun(
          scala(
            """
              object T {
                def f(a: Int, b: Int, c: Int, x: Boolean, l: List[Int]): Unit = {
                  val i1 = c + (a max b)
                  val i2 = (a + b) max c
                  val i3 = a max (b min c)
                  val i4 = a :: (b :: l)
                  val i5 = (a + b) +: l
                  val i6 = (a max b).toString
                  val u1 = -(a.abs)
                  val u2 = (-a).abs
                  val u3 = -(-a)
                  val u4 = !(!x)
                  val u5 = (-a).asInstanceOf[Long]
                }
              }
              """,
            """
              object T {
                def f(a: Int, b: Int, c: Int, x: Boolean, l: List[Int]): Unit = {
                  val i1 = c + (a max b)
                  val i2 = a + b max c
                  val i3 = a max (b min c)
                  val i4 = a :: b :: l
                  val i5 = (a + b) +: l
                  val i6 = (a max b).toString
                  val u1 = -a.abs
                  val u2 = (-a).abs
                  val u3 = -(-a)
                  val u4 = !(!x)
                  val u5 = (-a).asInstanceOf[Long]
                }
              }
              """
          )
        );
    }

    static class Reparenthesize extends ScalaVisitor<ExecutionContext> {
        @Override
        public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
            J j = super.visit(tree, ctx);
            // restoring parentheses that were all needed rebuilds a tree that prints the same
            return tree instanceof SourceFile sf && j instanceof SourceFile after &&
                   after.printAll().equals(sf.printAll()) ? (J) tree : j;
        }

        @Override
        public @Nullable J postVisit(J tree, ExecutionContext ctx) {
            return tree instanceof S.CompilationUnit ?
              new ScalaParenthesizeVisitor<>().visit(tree, ctx) : super.postVisit(tree, ctx);
        }

        @Override
        public <T extends J> J visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
            J j = super.visitParentheses(parens, ctx);
            return j instanceof J.Parentheses<?> p ? p.getTree().withPrefix(p.getPrefix()) : j;
        }
    }
}
