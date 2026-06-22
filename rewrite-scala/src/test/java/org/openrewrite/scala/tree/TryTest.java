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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class TryTest implements RewriteTest {

    @Test
    void simpleTryCatch() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  println("risky operation")
                } catch {
                  case e: Exception => println("caught exception")
                }
              }
              """
          )
        );
    }

    @Test
    void tryCatchExtraSpaceBeforeBrace() {
        rewriteRun(
          scala(
            """
              object Test {
                try { 1 } catch  {
                  case _: Exception => 2
                }
              }
              """
          )
        );
    }

    @Test
    void tryCatchCommentBeforeBrace() {
        rewriteRun(
          scala(
            """
              object Test {
                try { 1 } catch /* note */ {
                  case _: Exception => 2
                }
              }
              """
          )
        );
    }

    @Test
    void tryWithFinally() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  println("risky operation")
                } finally {
                  println("cleanup")
                }
              }
              """
          )
        );
    }

    @Test
    void tryCatchFinally() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  val result = 10 / 0
                } catch {
                  case e: ArithmeticException => println("division by zero")
                  case e: Exception => println("other exception")
                } finally {
                  println("cleanup")
                }
              }
              """
          )
        );
    }

    @Test
    void tryWithMultipleCatches() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  val text = "not a number"
                  val num = text.toInt
                } catch {
                  case e: NumberFormatException => println("not a valid number")
                  case e: NullPointerException => println("null pointer")
                  case e: Exception => println("unexpected error")
                }
              }
              """
          )
        );
    }

    @Test
    void tryExpression() {
        rewriteRun(
          scala(
            """
              object Test {
                val result = try {
                  "42".toInt
                } catch {
                  case e: NumberFormatException => 0
                }
              }
              """
          )
        );
    }

    @Test
    void nestedTry() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  try {
                    println("inner try")
                  } catch {
                    case e: Exception => println("inner catch")
                  }
                } catch {
                  case e: Exception => println("outer catch")
                }
              }
              """
          )
        );
    }

    @Test
    void tryWithWildcardCatch() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  throw new RuntimeException("error")
                } catch {
                  case _ => println("caught something")
                }
              }
              """
          )
        );
    }

    @Test
    void catchWithExtraWhitespaceBeforeArrow() {
        rewriteRun(
          scala(
            """
              object Test {
                def run(): Unit = try {
                  println("risky")
                } catch {
                  case e: RuntimeException   => println("rt")
                  case e: Exception          => println("ex")
                }
              }
              """
          )
        );
    }

    @Test
    void tryCatchEmptyCaseBody() {
        rewriteRun(
          scala(
            """
              object Test {
                def run(): Unit = {
                  try {
                    println("risky")
                  } catch {
                    case _: Throwable =>
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void tryCatchLongCommentBeforeCatch() {
        rewriteRun(
          scala(
            """
              object Test {
                def run(): Unit = {
                  try { 1 } /* an absurdly long block comment intended to push the catch keyword past any reasonable fixed-size lookahead window */ catch {
                    case _: Exception => 2
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void tryWithLongCommentBeforeFinally() {
        rewriteRun(
          scala(
            """
              object Test {
                def run(): Unit = {
                  try { 1 } catch { case _: Exception => 2 } /* an absurdly long block comment intended to push the finally keyword past any reasonable fixed-size lookahead window */ finally { 3 }
                }
              }
              """
          )
        );
    }

    @Test
    void tryCatchEmptyCaseBodyWithLineComment() {
        rewriteRun(
          scala(
            """
              object Test {
                def run(): Unit = {
                  try {
                    println("risky")
                  } catch {
                    case _: Throwable => // If traversal fails, maps stay empty; types will be null
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void tryWithTypedPattern() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  val result = riskyOperation()
                } catch {
                  case _: IllegalArgumentException | _: IllegalStateException => 
                    println("illegal argument or state")
                  case e: Throwable =>
                    println(s"unexpected error: ${e.getMessage}")
                }
              }
              """
          )
        );
    }

    @Test
    void catchCaseWithMultipleStatementBody() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  println("risky")
                } catch {
                  case _: Exception =>
                    Thread.sleep(500)
                    println("recover")
                }
              }
              """
          )
        );
    }

    @Test
    void catchMultipleCasesWithMultiStatementBodies() {
        rewriteRun(
          scala(
            """
              object Test {
                try {
                  println("risky")
                } catch {
                  case _: NumberFormatException =>
                    println("nfe a")
                    println("nfe b")
                  case _: Exception =>
                    println("ex a")
                    println("ex b")
                }
              }
              """
          )
        );
    }

    @Test
    void catchCaseWithSemicolonSeparatedBody() {
        rewriteRun(
          scala(
            """
              object Test {
                try { 1 } catch { case _: Exception => println("a"); println("b") }
              }
              """
          )
        );
    }

    @Test
    void catchCaseWithBlockBody() {
        rewriteRun(
          scala(
            """
              object Test {
                def foo: Boolean = {
                  try {
                    true
                  } catch {
                    case e: Exception => {
                      e.printStackTrace()
                      false
                    }
                  }
                }
              }
              """
          )
        );
    }

    @Test
    void spaceBeforeColonOnCatchParameter() {
        rewriteRun(scala(
            """
            object T {
              try { 1 } catch { case e : Exception => 2 }
            }
            """
        ));
    }

    @Test
    void catchExtractorPatternWithBinding() {
        rewriteRun(scala(
            """
            object Test {
              try {
                println("risky")
              } catch {
                case NonFatal(e) => println(e)
              }
            }
            """
        ));
    }

    @Test
    void catchPatternsNotCrammedIntoIdentifier() {
        assertNoPatternInIdentifier(
            """
            object Test {
              try {
                println("risky")
              } catch {
                case NonFatal(e) => println(e)
                case _: IllegalArgumentException | _: IllegalStateException => println("illegal")
                case e @ (_: ClassNotFoundException) => println(e)
              }
            }
            """
        );
    }

    @Test
    void scala3BracelessCatch() {
        rewriteRun(scala(
            """
            object Test {
              def run(): Unit =
                try risky()
                catch
                  case _: RuntimeException => recover()
                  case NonFatal(e) => log(e)
            }
            """
        ));
    }

    @Test
    void scala3BracelessCatchWithFinally() {
        rewriteRun(scala(
            """
            object Test {
              def run(): Unit =
                try risky()
                catch case _: Exception => recover()
                finally cleanup()
            }
            """
        ));
    }

    @Test
    void commonCatchesFitJavaTryModel() {
        // OpenRewrite prefers reusing J.* types so Java recipes apply: the common
        // `[name][: Type]` (guard-free) catch maps onto J.Try.Catch and must stay J.Try.
        assertTryKind(
            """
            object Test {
              def run(): Unit =
                try { a() } catch {
                  case e: Exception => 1
                  case _: java.io.IOException => 2
                  case e => 3
                  case _ => 4
                } finally { b() }
            }
            """, 1, 0);
    }

    @Test
    void scalaSpecificCatchesUseSTry() {
        // Patterns J.Try.Catch cannot hold (extractors, alternatives, guards) fall back to S.Try.
        assertTryKind("object T { try { a() } catch { case NonFatal(e) => 1 } }", 0, 1);
        assertTryKind("object T { try { a() } catch { case _: A | _: B => 1 } }", 0, 1);
        assertTryKind("object T { try { a() } catch { case e: Exception if e != null => 1 } }", 0, 1);
        // A single non-fitting clause routes the whole try to S.Try.
        assertTryKind("object T { try { a() } catch { case e: A => 1\n case NonFatal(e) => 2 } }", 0, 1);
    }

    private void assertTryKind(String source, int expectedJTry, int expectedSTry) {
        AtomicInteger jTry = new AtomicInteger();
        AtomicInteger sTry = new AtomicInteger();
        rewriteRun(
            scala(
                source,
                spec -> spec.afterRecipe(cu -> {
                    new JavaIsoVisitor<Integer>() {
                        @Override
                        public J.Try visitTry(J.Try t, Integer p) {
                            jTry.incrementAndGet();
                            return super.visitTry(t, p);
                        }
                    }.visit(cu, 0);
                    new ScalaIsoVisitor<Integer>() {
                        @Override
                        public S.Try visitSTry(S.Try t, Integer p) {
                            sTry.incrementAndGet();
                            return super.visitSTry(t, p);
                        }
                    }.visit(cu, 0);
                    assertThat(jTry.get()).as("J.Try count").isEqualTo(expectedJTry);
                    assertThat(sTry.get()).as("S.Try count").isEqualTo(expectedSTry);
                })
            )
        );
    }

    private void assertNoPatternInIdentifier(String source) {
        rewriteRun(
            scala(
                source,
                spec -> spec.afterRecipe(cu -> {
                    List<String> identifierNames = new ArrayList<>();
                    new JavaIsoVisitor<Integer>() {
                        @Override
                        public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                            identifierNames.add(identifier.getSimpleName());
                            return super.visitIdentifier(identifier, p);
                        }
                    }.visit(cu, 0);
                    assertThat(identifierNames)
                      .as("catch-pattern source text should not be crammed into an identifier name")
                      .allSatisfy(name -> assertThat(name).doesNotContain("(", "|", "@", ":"));
                })
            )
        );
    }

    @Test
    void significantCharactersInComments() {
        // visitParsedTry / visitTryImpl — `=>` arrow in catch case line comment
        rewriteRun(scala(
            """
            val r = try { 1 } catch {
              case e: Exception // =>
              => 2
            }
            """
        ));
    }
}
