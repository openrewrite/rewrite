/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class KotlinParenthesizeVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(toRecipe(Reparenthesize::new))
          // the visitor reparenthesizes trees with their parentheses stripped, which no parser makes
          .recipeQualityValidation(RecipeQualityValidation.builder().parserShapedTrees(false).build());
    }

    @DocumentExample
    @Test
    void infixFunctionsBindAlike() {
        rewriteRun(
          kotlin(
            """
              fun test(a: Int, b: Int, c: Int) {
                  val i1 = a or (b and c)
                  val i2 = (a or b) and c
                  val i3 = (a and b) and c
                  val i4 = (a + b) shl c
                  val i5 = a shl (b + c)
                  val i6 = a to (b to c)
                  val i7 = (a to b) to c
                  val i8 = a to (b or c)
                  val i9 = (a + b) to c
                  val i10 = (a to b).first
              }
              """,
            """
              fun test(a: Int, b: Int, c: Int) {
                  val i1 = a or (b and c)
                  val i2 = (a or b) and c
                  val i3 = a and b and c
                  val i4 = a + b shl c
                  val i5 = a shl b + c
                  val i6 = a to (b to c)
                  val i7 = a to b to c
                  val i8 = a to (b or c)
                  val i9 = a + b to c
                  val i10 = (a to b).first
              }
              """
          )
        );
    }

    @Test
    void kotlinOperators() {
        rewriteRun(
          kotlin(
            """
              fun test(a: Int, b: Int, c: Int, x: Boolean, y: Boolean, n: Int?, m: Int?, l: List<Int>) {
                  val e1 = n ?: (a + b)
                  val e2 = n ?: (a or b)
                  val e3 = (n ?: a) + b
                  val e4 = (n ?: a) or b
                  val e5 = (n ?: a) > b
                  val e6 = n ?: (m ?: a)
                  val e7 = (n ?: m) ?: a
                  val e8 = x || (y ?: x)
                  val r1 = (a..b).first
                  val r2 = a in (b..c)
                  val r3 = x == (a in l)
                  val r4 = !(a in l)
                  val r5 = (a < b) == x
                  val r6 = a < (b and c)
                  val r7 = x && (y || x)
                  val r8 = (x && y) || x
                  val r9 = x == (y == x)
              }
              """,
            """
              fun test(a: Int, b: Int, c: Int, x: Boolean, y: Boolean, n: Int?, m: Int?, l: List<Int>) {
                  val e1 = n ?: a + b
                  val e2 = n ?: a or b
                  val e3 = (n ?: a) + b
                  val e4 = (n ?: a) or b
                  val e5 = n ?: a > b
                  val e6 = n ?: (m ?: a)
                  val e7 = n ?: m ?: a
                  val e8 = x || y ?: x
                  val r1 = (a..b).first
                  val r2 = a in b..c
                  val r3 = x == a in l
                  val r4 = !(a in l)
                  val r5 = a < b == x
                  val r6 = a < b and c
                  val r7 = x && (y || x)
                  val r8 = x && y || x
                  val r9 = x == (y == x)
              }
              """
          )
        );
    }

    @Test
    void castsTypeChecksAndUnaryOperators() {
        rewriteRun(
          kotlin(
            """
              fun test(a: Int, b: Int, x: Boolean, n: Int?, o: Any) {
                  val c1 = (-a) as Number
                  val c2 = (a + b) as Number
                  val c3 = a + (b as Int)
                  val c4 = (o as Int) < b
                  val c5 = (o as Int) > b
                  val c6 = (o is Int) && x
                  val c7 = (o as String).length
                  val c8 = (o is Int) as Boolean
                  val u1 = -(-a)
                  val u2 = !(!x)
                  val u3 = -(n!!)
                  val u4 = (-a).inc()
                  val u5 = (a + b)!!
              }
              """,
            """
              fun test(a: Int, b: Int, x: Boolean, n: Int?, o: Any) {
                  val c1 = -a as Number
                  val c2 = (a + b) as Number
                  val c3 = a + b as Int
                  val c4 = (o as Int) < b
                  val c5 = o as Int > b
                  val c6 = o is Int && x
                  val c7 = (o as String).length
                  val c8 = (o is Int) as Boolean
                  val u1 = -(-a)
                  val u2 = !!x
                  val u3 = -n!!
                  val u4 = (-a).inc()
                  val u5 = (a + b)!!
              }
              """
          )
        );
    }

    static class Reparenthesize extends KotlinVisitor<ExecutionContext> {
        @Override
        public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
            J j = super.visit(tree, ctx);
            // restoring parentheses that were all needed rebuilds a tree that prints the same
            return tree instanceof SourceFile sf && j instanceof SourceFile after &&
                   after.printAll().equals(sf.printAll()) ? (J) tree : j;
        }

        @Override
        public @Nullable J postVisit(J tree, ExecutionContext ctx) {
            return tree instanceof K.CompilationUnit ?
              new KotlinParenthesizeVisitor<>().visit(tree, ctx) : super.postVisit(tree, ctx);
        }

        @Override
        public <T extends J> J visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
            J j = super.visitParentheses(parens, ctx);
            return j instanceof J.Parentheses<?> p ? p.getTree().withPrefix(p.getPrefix()) : j;
        }
    }
}
