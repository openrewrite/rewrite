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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class MatchTest implements RewriteTest {

    @Test
    void atBindingIsNotCollapsedIntoSingleIdentifier() {
        AtomicReference<S.Binding> binding = new AtomicReference<>();
        rewriteRun(
          scala(
            """
            object Test {
              case class Person(name: String, age: Int)
              def handle(x: Any): String = x match {
                case p@Person(name, _) => name
                case _ => ""
              }
            }
            """,
            spec -> spec.afterRecipe(cu -> {
                new ScalaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                        // the whole pattern must never be stuffed into one identifier name
                        assertThat(identifier.getSimpleName()).doesNotContain("@", "(");
                        return identifier;
                    }

                    @Override
                    public J visitBinding(S.Binding b, Integer p) {
                        binding.set(b);
                        return super.visitBinding(b, p);
                    }
                }.visit(cu, 0);

                assertThat(binding.get()).isNotNull();
                assertThat(binding.get().getName().getSimpleName()).isEqualTo("p");
                assertThat(binding.get().getPattern()).isInstanceOf(J.MethodInvocation.class);
                J.MethodInvocation pattern = (J.MethodInvocation) binding.get().getPattern();
                assertThat(pattern.getSelect()).isInstanceOf(J.Identifier.class);
                assertThat(((J.Identifier) pattern.getSelect()).getSimpleName()).isEqualTo("Person");
                assertThat(pattern.getArguments()).hasSize(2);
            })
          )
        );
    }

    @Test
    void emptyCaseBodyWithGuardPreservesAlignmentBeforeArrow() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Int): Unit = x match {
                case 1 if x > 0   =>
                case _ => println(x)
              }
            }
            """
          )
        );
    }

    @Test
    void simpleMatch() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String => s
                case _ => "unknown"
              }
            }
            """
          )
        );
    }

    @Test
    void matchExtraSpaceBeforeKeyword() {
        rewriteRun(
          scala(
            """
            object Test {
              val r = 1  match {
                case _ => 0
              }
            }
            """
          )
        );
    }

    @Test
    void matchExtraSpaceBeforeBrace() {
        rewriteRun(
          scala(
            """
            object Test {
              val r = 1 match  {
                case _ => 0
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithGuard() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String if s.nonEmpty => s
                case _ => "empty"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithAtBinding() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(xs: List[Int]): Int = xs match {
                case all@List(head, _*) => head
                case _ => 0
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithAtBindingAndTypedPattern() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case msg@(_: String) => msg
                case _ => ""
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithGuardAndComplexCondition() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String if s.nonEmpty && s.length > 3 => s
                case i: Int if i > 0 => i.toString
                case _ => "other"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithUnapplyPattern() {
        rewriteRun(
          scala(
            """
            object Test {
              case class Person(name: String, age: Int)
              def handle(x: Any): String = x match {
                case Person(name, age) => name
                case _ => ""
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithGuardExtraWhitespace() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String  if  s.nonEmpty => s
                case _ => "empty"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithBlockGuard() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String if {
                      s.nonEmpty
                    } => s
                case _ => "empty"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithGuardComment() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String /*guard*/ if /*filter*/ s.nonEmpty => s
                case _ => "empty"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithAtBindingAndUnapply() {
        rewriteRun(
          scala(
            """
            object Test {
              case class Person(name: String, age: Int)
              def handle(x: Any): String = x match {
                case p@Person(name, _) => name
                case _ => ""
              }
            }
            """
          )
        );
    }

    @Test
    void matchCaseWithMultipleStatements() {
        rewriteRun(
          scala(
            """
            object Test {
              val x: Any = "hello"
              x match {
                case s: String =>
                  val y = s.length
                  val z = y + 1
                  println(z)
                case i: Int =>
                  val doubled = i * 2
                  println(doubled)
                case _ =>
                  val msg = "unknown"
                  println(msg)
              }
            }
            """
          )
        );
    }

    @Test
    void matchCaseWithMultipleStatementsAndMatch() {
        rewriteRun(
          scala(
            """
            object Test {
              def process(x: Any): Unit = x match {
                case 1 =>
                  val y = 10
                  val z = y + 1
                  println(z)
                case _ =>
                  println("other")
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithExtraWhitespaceBeforeArrow() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case "a"   => "1"
                case _ => "0"
              }
            }
            """
          )
        );
    }

    @Test
    void matchCasesSeparatedBySemicolon() {
        rewriteRun(
          scala(
            """
            object Test {
              def label(x: Any): String = x match { case s: String => s; case _ => null }
            }
            """
          )
        );
    }

    @Test
    void matchCasesSeparatedBySemicolonWithMultiplePatterns() {
        rewriteRun(
          scala(
            """
            object Test {
              def label(x: Any): String = x match {
                case s: String => s
                case i: Int => i.toString; case d: Double => d.toString
                case _ => null
              }
            }
            """
          )
        );
    }

    @Test
    void matchCaseWithCallInForLoopFollowedByCase() {
        rewriteRun(
          scala(
            """
            object Test {
              def foo(args: java.util.ArrayList[Int]): Unit = {
                for (a <- List(1, 2, 3)) {
                  a match {
                    case x: Int =>
                      val y = x + 1
                      args.add(y)
                    case _ => return
                  }
                }
              }
            }
            """
          )
        );
    }

    @Test
    void matchCaseWithMultipleStatementsEndingWithCallFollowedByCase() {
        rewriteRun(
          scala(
            """
            object Test {
              val args = new java.util.ArrayList[String]()
              def visit(t: Any): Unit = t match {
                case block: String =>
                  val blockExpr = wrap(block)
                  args.add(blockExpr.asInstanceOf[String])
                case _ => cursor = savedCursor
              }
            }
            """
          )
        );
    }

    @Test
    void matchCaseWithNestedDefsAndMultiLineReturn() {
        rewriteRun(
          scala(
            """
            object Test {
              def visit(t: Any): String = t match {
                case innerApp: String =>
                  def asExpression(j: Int): Int = j match {
                    case e: Int => e
                    case _ => 0
                  }
                  def finishAtEnd(): Unit = if (true) {
                    val adjustedEnd = 1
                    if (adjustedEnd > 0) cursor = adjustedEnd
                  }
                  val methodType = "x" match { case m: String => m; case _ => null }
                  finishAtEnd()
                  return doCall(prefix, Empty,
                    new Wrapper(fn, Empty),
                    Container.build(Empty, args, Empty), methodType)

                case _ =>
                  ""
              }
            }
            """
          )
        );
    }

    @Test
    void matchCaseWithNestedDefAndReturn() {
        rewriteRun(
          scala(
            """
            object Test {
              def foo(x: Any): Int = x match {
                case _: String =>
                  def helper(): Int = if (true) {
                    42
                  } else {
                    0
                  }
                  return helper()

                case _ =>
                  0
              }
            }
            """
          )
        );
    }

    @Test
    void matchCaseWithMultiLineReturnFollowedByCase() {
        rewriteRun(
          scala(
            """
            object Test {
              def foo(x: Any): String = x match {
                case s: String =>
                  doFirst()
                  return doSecond(
                    "a",
                    "b")

                case _ =>
                  "other"
              }
            }
            """
          )
        );
    }

    @Test
    void matchCaseWithIfBlockBeforeReturn() {
        rewriteRun(
          scala(
            """
            object Test {
              def foo(x: Any): String = x match {
                case s: String =>
                  if (s.nonEmpty) {
                    doSomething()
                  }
                  return s
                case _ =>
                  "other"
              }
            }
            """
          )
        );
    }

    @Test
    void matchWithLineCommentContainingIfBeforeGuard() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String // if something
                    if s.nonEmpty => s
                case _ => "empty"
              }
            }
            """
          )
        );
    }

    @Test
    void matchCaseWithMultiStatementBody() {
        rewriteRun(
          scala(
            """
            object Test {
              def handle(x: Any): String = x match {
                case s: String =>
                  println("got string")
                  s
                case _ =>
                  println("other")
                  "other"
              }
            }
            """
          )
        );
    }

    @Test
    void partialFunctionLiteralWithMultiStatementCaseBody() {
        rewriteRun(
          scala(
            """
            object Test {
              val handler: PartialFunction[Any, String] = {
                case s: String =>
                  println("got string")
                  s
                case _ =>
                  println("other")
                  "other"
              }
            }
            """
          )
        );
    }

    @Test
    void significantCharactersInComments() {
        // buildCasesBlock — `=>` arrow in case line comment
        rewriteRun(
          scala(
            """
            val r = 1 match {
              case x // =>
              => x
            }
            """
          )
        );
    }
}
