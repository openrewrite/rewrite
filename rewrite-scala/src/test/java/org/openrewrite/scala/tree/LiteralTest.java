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

class LiteralTest implements RewriteTest {

    @Test
    void integerLiteral() {
        rewriteRun(
          scala("42")
        );
    }

    @Test
    void hexLiteral() {
        rewriteRun(
          scala("0xFF")
        );
    }

    @Test
    void longLiteral() {
        rewriteRun(
          scala("42L")
        );
    }

    @Test
    void floatLiteral() {
        rewriteRun(
          scala("3.14f")
        );
    }

    @Test
    void doubleLiteral() {
        rewriteRun(
          scala("3.14")
        );
    }

    @Test
    void booleanLiteralTrue() {
        rewriteRun(
          scala("true")
        );
    }

    @Test
    void booleanLiteralFalse() {
        rewriteRun(
          scala("false")
        );
    }

    @Test
    void characterLiteral() {
        rewriteRun(
          scala("'a'")
        );
    }

    @Test
    void stringLiteral() {
        rewriteRun(
          scala("\"hello\"")
        );
    }

    @Test
    void multilineStringLiteral() {
        rewriteRun(
          scala(
            """
            \"""hello
            world\"""
            """
          )
        );
    }

    @Test
    void nullLiteral() {
        rewriteRun(
          scala("null")
        );
    }

    @Test
    void symbolLiteral() {
        // Scala 2 only - deprecated in Scala 3
        rewriteRun(
          scala("'symbol")
        );
    }

    @SuppressWarnings("ScalaUnnecessaryParentheses")
    @Test
    void insideParentheses() {
        rewriteRun(
          scala("(42)"),
          scala("((42))")
        );
    }
}