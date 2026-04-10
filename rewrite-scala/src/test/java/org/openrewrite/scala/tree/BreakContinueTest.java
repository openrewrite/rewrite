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

public class BreakContinueTest implements RewriteTest {
    
    @Test
    void breakInWhileLoop() {
        rewriteRun(
            scala(
                """
                object BreakTest {
                  def findFirst(): Unit = {
                    var i = 0
                    while (i < 10) {
                      if (i == 5) return
                      i += 1
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void continueInWhileLoop() {
        rewriteRun(
            scala(
                """
                object SkipTest {
                  def skipEven(): Unit = {
                    var i = 0
                    while (i < 10) {
                      i += 1
                      if (i % 2 == 0) println("skip")
                      else println(i)
                    }
                  }
                }
                """
            )
        );
    }
    
    @Test
    void breakInForLoop() {
        rewriteRun(
            scala(
                """
                import scala.util.control.Breaks._
                
                def findInArray(): Unit = {
                  val arr = Array(1, 2, 3, 4, 5)
                  breakable {
                    for (x <- arr) {
                      if (x == 3) break
                      println(x)
                    }
                  }
                }
                """
            )
        );
    }
    
    @Test
    void nestedBreakable() {
        rewriteRun(
            scala(
                """
                import scala.util.control.Breaks._
                
                def nestedLoops(): Unit = {
                  val outer = new Breaks
                  val inner = new Breaks
                  
                  outer.breakable {
                    for (i <- 1 to 5) {
                      inner.breakable {
                        for (j <- 1 to 5) {
                          if (j == 3) inner.break
                          if (i * j > 10) outer.break
                        }
                      }
                    }
                  }
                }
                """
            )
        );
    }
    
    @Test
    void breakWithoutBreakable() {
        rewriteRun(
            scala(
                """
                // This is actually just a method call named 'break'
                def test(): Unit = {
                  break
                }
                
                def break: Unit = println("Not a break statement")
                """
            )
        );
    }
}