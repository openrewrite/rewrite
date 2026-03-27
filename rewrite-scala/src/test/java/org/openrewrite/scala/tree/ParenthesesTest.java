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

class ParenthesesTest implements RewriteTest {

    @Test
    void simpleParentheses() {
        rewriteRun(
          scala("(x)")
        );
    }

    @Test
    void parenthesesAroundLiteral() {
        rewriteRun(
          scala("(42)")
        );
    }

    @Test
    void parenthesesAroundBinary() {
        rewriteRun(
          scala("(a + b)")
        );
    }

    @Test
    void parenthesesForPrecedence() {
        rewriteRun(
          scala("(a + b) * c")
        );
    }

    @Test
    void nestedParentheses() {
        rewriteRun(
          scala("((a + b))")
        );
    }

    @Test
    void multipleParenthesesGroups() {
        rewriteRun(
          scala("(a + b) * (c - d)")
        );
    }

    @Test
    void parenthesesWithUnary() {
        rewriteRun(
          scala("-(a + b)")
        );
    }

    @Test
    void complexExpression() {
        rewriteRun(
          scala("((a + b) * c) / (d - e)")
        );
    }

    @Test
    void parenthesesWithMethodCall() {
        rewriteRun(
          scala("(x.foo())")
        );
    }

    @Test
    void parenthesesWithSpaces() {
        rewriteRun(
          scala("( x + y )")
        );
    }
}