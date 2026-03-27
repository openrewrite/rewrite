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

class UnaryTest implements RewriteTest {

    @Test
    void negation() {
        rewriteRun(
          scala("!true")
        );
    }

    @Test
    void unaryMinus() {
        rewriteRun(
          scala("-5")
        );
    }

    @Test
    void unaryPlus() {
        rewriteRun(
          scala("+5")
        );
    }

    @Test
    void bitwiseNot() {
        rewriteRun(
          scala("~5")
        );
    }

    @Test
    void postfixOperator() {
        rewriteRun(
          scala("5!")
        );
    }

    @Test
    void prefixMethodCall() {
        rewriteRun(
          scala("x.unary_-")
        );
    }

    @Test
    void withParentheses() {
        rewriteRun(
          scala("-(x + y)")
        );
    }
}