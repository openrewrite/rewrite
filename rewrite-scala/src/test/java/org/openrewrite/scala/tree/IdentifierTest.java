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

class IdentifierTest implements RewriteTest {

    @Test
    void simpleIdentifier() {
        rewriteRun(
          scala("x")
        );
    }

    @Test
    void camelCaseIdentifier() {
        rewriteRun(
          scala("myVariable")
        );
    }

    @Test
    void underscoreIdentifier() {
        rewriteRun(
          scala("_value")
        );
    }

    @Test
    void dollarSignIdentifier() {
        rewriteRun(
          scala("$value")
        );
    }

    @Test
    void backtickIdentifier() {
        rewriteRun(
          scala("`type`")
        );
    }

    @Test
    void backtickIdentifierWithSpaces() {
        rewriteRun(
          scala("`my variable`")
        );
    }

    @Test
    void operatorIdentifier() {
        rewriteRun(
          scala("+")
        );
    }

    @Test
    void symbolicIdentifier() {
        rewriteRun(
          scala("::"),
          scala("=>"),
          scala("++")
        );
    }
}