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

class AssignmentTest implements RewriteTest {

    @Test
    void simpleAssignment() {
        rewriteRun(
          scala(
            """
            object Test {
              var x = 1
              x = 5
            }
            """
          )
        );
    }

    @Test
    void compoundAssignmentAdd() {
        rewriteRun(
          scala(
            """
            object Test {
              var x = 1
              x += 1
            }
            """
          )
        );
    }

    @Test
    void compoundAssignmentSubtract() {
        rewriteRun(
          scala(
            """
            object Test {
              var x = 10
              x -= 5
            }
            """
          )
        );
    }

    @Test
    void compoundAssignmentMultiply() {
        rewriteRun(
          scala(
            """
            object Test {
              var x = 2
              x *= 3
            }
            """
          )
        );
    }

    @Test
    void compoundAssignmentDivide() {
        rewriteRun(
          scala(
            """
            object Test {
              var x = 10
              x /= 2
            }
            """
          )
        );
    }

    @Test
    void fieldAssignment() {
        rewriteRun(
          scala(
            """
            object Test {
              obj.field = 42
            }
            """
          )
        );
    }

    @Test
    void arrayAssignment() {
        rewriteRun(
          scala(
            """
            object Test {
              arr(0) = 10
            }
            """
          )
        );
    }

    @Test
    void tupleDestructuringAssignment() {
        rewriteRun(
          scala(
            """
            object Test {
              var (a, b) = (1, 2)
              (a, b) = (3, 4)
            }
            """
          )
        );
    }
}