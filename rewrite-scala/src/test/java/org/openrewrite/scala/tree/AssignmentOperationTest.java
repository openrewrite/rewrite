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

class AssignmentOperationTest implements RewriteTest {

    @Test
    void additionAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 10
                  x += 5
                }
                """
            )
        );
    }

    @Test
    void subtractionAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 10
                  x -= 3
                }
                """
            )
        );
    }

    @Test
    void multiplicationAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 5
                  x *= 2
                }
                """
            )
        );
    }

    @Test
    void divisionAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 20
                  x /= 4
                }
                """
            )
        );
    }

    @Test
    void moduloAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 17
                  x %= 5
                }
                """
            )
        );
    }

    @Test
    void bitwiseAndAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 15
                  x &= 7
                }
                """
            )
        );
    }

    @Test
    void bitwiseOrAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 8
                  x |= 4
                }
                """
            )
        );
    }

    @Test
    void bitwiseXorAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 12
                  x ^= 7
                }
                """
            )
        );
    }

    @Test
    void leftShiftAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 4
                  x <<= 2
                }
                """
            )
        );
    }

    @Test
    void rightShiftAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = 16
                  x >>= 2
                }
                """
            )
        );
    }

    @Test
    void unsignedRightShiftAssignment() {
        rewriteRun(
            scala(
                """
                object Test {
                  var x = -8
                  x >>>= 2
                }
                """
            )
        );
    }

    @Test
    void assignmentToArrayElement() {
        rewriteRun(
            scala(
                """
                object Test {
                  val arr = Array(1, 2, 3)
                  arr(0) += 10
                }
                """
            )
        );
    }

    @Test
    void assignmentToField() {
        rewriteRun(
            scala(
                """
                object Test {
                  class Person(var age: Int)
                  val p = new Person(25)
                  p.age += 1
                }
                """
            )
        );
    }
}