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

class BlockTest implements RewriteTest {

    @Test
    void simpleBlock() {
        rewriteRun(
            scala(
                """
                object Test {
                  {
                    println("line 1")
                    println("line 2")
                  }
                }
                """
            )
        );
    }

    @Test
    void blockAsExpression() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = {
                    val temp = 10
                    temp * 2
                  }
                }
                """
            )
        );
    }

    @Test
    void nestedBlocks() {
        rewriteRun(
            scala(
                """
                object Test {
                  {
                    println("outer")
                    {
                      println("inner")
                    }
                  }
                }
                """
            )
        );
    }

    @Test
    void emptyBlock() {
        rewriteRun(
            scala(
                """
                object Test {
                  {}
                }
                """
            )
        );
    }
}