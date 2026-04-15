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

class ControlFlowTest implements RewriteTest {

    @Test
    void ifStatement() {
        rewriteRun(
            scala(
                """
                object Test {
                  if (true) println("yes")
                }
                """
            )
        );
    }

    @Test
    void ifElseStatement() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = 5
                  if (x > 0) println("positive") else println("not positive")
                }
                """
            )
        );
    }

    @Test
    void whileLoop() {
        rewriteRun(
            scala(
                """
                object Test {
                  var i = 0
                  while (i < 10) {
                    println(i)
                    i += 1
                  }
                }
                """
            )
        );
    }
    
    @Test 
    void ifWithBlock() {
        rewriteRun(
            scala(
                """
                object Test {
                  if (true) {
                    println("line 1")
                    println("line 2")
                  }
                }
                """
            )
        );
    }
    
    @Test
    void nestedIf() {
        rewriteRun(
            scala(
                """
                object Test {
                  if (true) {
                    if (false) {
                      println("nested")
                    }
                  }
                }
                """
            )
        );
    }
    
    @Test
    void ifElseExpressionLiteralOperands() {
        rewriteRun(
            scala(
                """
                object Test {
                  val flag = true
                  val result = if (flag) "yes" else "no"
                }
                """
            )
        );
    }

    @Test
    void ifElseExpressionIdentifierOperands() {
        rewriteRun(
            scala(
                """
                object Test {
                  val a = 1
                  val b = 2
                  val flag = true
                  val picked = if (flag) a else b
                }
                """
            )
        );
    }

    @Test
    void returnWithIfExpression() {
        rewriteRun(
            scala(
                """
                class Calc {
                  def pick(flag: Boolean): Int = {
                    return if (flag) 1 else 2
                  }
                }
                """
            )
        );
    }

    @Test
    void assignmentWithIfExpression() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 0
                  val flag = true
                  x = if (flag) 1 else 2
                }
                """
            )
        );
    }

    @Test
    void binaryOpWithIfExpressionOperand() {
        rewriteRun(
            scala(
                """
                object Test {
                  val flag = true
                  val sum = 10 + (if (flag) 1 else 2)
                }
                """
            )
        );
    }

    @Test
    void tupleWithIfExpressionElements() {
        rewriteRun(
            scala(
                """
                object Test {
                  val flag = true
                  val pair = (if (flag) 1 else 2, if (flag) "a" else "b")
                }
                """
            )
        );
    }

    @Test
    void throwWithIfExpression() {
        rewriteRun(
            scala(
                """
                class Check {
                  def verify(flag: Boolean): Unit = {
                    throw (if (flag) new RuntimeException("a") else new IllegalArgumentException("b"))
                  }
                }
                """
            )
        );
    }

    @Test
    void whileWithUnitBody() {
        rewriteRun(
            scala(
                """
                object Test {
                  var i = 0
                  while (i < 10) { i += 1 }
                }
                """
            )
        );
    }

    @Test
    void ifElseIfElse() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = 5
                  if (x > 10) {
                    println("greater than 10")
                  } else if (x > 0) {
                    println("greater than 0")
                  } else {
                    println("less than or equal to 0")
                  }
                }
                """
            )
        );
    }

    @Test
    void forLoop() {
        rewriteRun(
            scala(
                """
                object Test {
                  for (i <- 1 to 10) {
                    println(i)
                  }
                }
                """
            )
        );
    }
    
    @Test
    void forLoopWithTo() {
        rewriteRun(
            scala(
                """
                object Test {
                  for (i <- 1 to 10) {
                    println(i)
                  }
                }
                """
            )
        );
    }
    
    @Test
    void forLoopWithUntil() {
        rewriteRun(
            scala(
                """
                object Test {
                  for (i <- 0 until 10) {
                    println(i)
                  }
                }
                """
            )
        );
    }
    
    @Test
    void forLoopWithToVariable() {
        rewriteRun(
            scala(
                """
                object Test {
                  val n = 10
                  for (i <- 1 to n) {
                    println(i)
                  }
                }
                """
            )
        );
    }
    
    @Test
    void forLoopWithUntilVariable() {
        rewriteRun(
            scala(
                """
                object Test {
                  val n = 10
                  for (i <- 0 until n) {
                    println(i)
                  }
                }
                """
            )
        );
    }
    
    @Test
    void forLoopWithToExpression() {
        rewriteRun(
            scala(
                """
                object Test {
                  val n = 5
                  for (i <- 0 to (n * 2)) {
                    println(i)
                  }
                }
                """
            )
        );
    }
    
    @Test
    void forLoopWithUntilExpression() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3, 4, 5)
                  for (i <- 0 until arr.length) {
                    println(arr(i))
                  }
                }
                """
            )
        );
    }
    
    @Test
    void forLoopWithCollection() {
        rewriteRun(
            scala(
                """
                object Test {
                  val list = List(1, 2, 3, 4, 5)
                  for (item <- list) {
                    println(item)
                  }
                }
                """
            )
        );
    }
    
    @Test
    void simpleAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 10
                  x = 20
                }
                """
            )
        );
    }
    
    @Test
    void compoundAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 10
                  x += 5
                  x -= 3
                  x *= 2
                  x /= 4
                }
                """
            )
        );
    }
}