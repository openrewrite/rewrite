/*
 * Copyright 2025 the original author or authors.
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
}