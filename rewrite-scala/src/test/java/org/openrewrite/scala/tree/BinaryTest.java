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

class BinaryTest implements RewriteTest {

    @Test
    void addition() {
        rewriteRun(
          scala("1 + 2")
        );
    }

    @Test
    void subtraction() {
        rewriteRun(
          scala("5 - 3")
        );
    }

    @Test
    void multiplication() {
        rewriteRun(
          scala("2 * 3")
        );
    }

    @Test
    void division() {
        rewriteRun(
          scala("10 / 2")
        );
    }

    @Test
    void modulo() {
        rewriteRun(
          scala("10 % 3")
        );
    }

    @Test
    void lessThan() {
        rewriteRun(
          scala("1 < 2")
        );
    }

    @Test
    void greaterThan() {
        rewriteRun(
          scala("2 > 1")
        );
    }

    @Test
    void lessThanOrEqual() {
        rewriteRun(
          scala("1 <= 2")
        );
    }

    @Test
    void greaterThanOrEqual() {
        rewriteRun(
          scala("2 >= 1")
        );
    }

    @Test
    void equal() {
        rewriteRun(
          scala("1 == 1")
        );
    }

    @Test
    void notEqual() {
        rewriteRun(
          scala("1 != 2")
        );
    }

    @Test
    void logicalAnd() {
        rewriteRun(
          scala("true && false")
        );
    }

    @Test
    void logicalOr() {
        rewriteRun(
          scala("true || false")
        );
    }

    @Test
    void infixMethodCall() {
        rewriteRun(
          scala("list map func")
        );
    }

    @Test
    void infixWithDot() {
        rewriteRun(
          scala("1.+(2)")
        );
    }

    @Test
    void bitwiseAnd() {
        rewriteRun(
          scala("5 & 3")
        );
    }

    @Test
    void bitwiseOr() {
        rewriteRun(
          scala("5 | 3")
        );
    }

    @Test
    void bitwiseXor() {
        rewriteRun(
          scala("5 ^ 3")
        );
    }

    @Test
    void leftShift() {
        rewriteRun(
          scala("1 << 2")
        );
    }

    @Test
    void rightShift() {
        rewriteRun(
          scala("8 >> 2")
        );
    }
}