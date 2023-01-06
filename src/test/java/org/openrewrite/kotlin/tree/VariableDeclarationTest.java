/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

public class VariableDeclarationTest implements RewriteTest {

    @Test
    void singleVariableDeclaration() {
        rewriteRun(
          kotlin("val a = 1")
        );
    }

    @Test
    void singleVariableDeclarationWithTypeConstraint() {
        rewriteRun(
          kotlin("val a: Int = 1")
        );
    }

    @Disabled("Requires size and init to be parsed.")
    @Test
    void diamondOperator() {
        rewriteRun(
          kotlin("val a: Array<Int> = Array<Int>(1){1}")
        );
    }

    @Test
    void ifExpression() {
        rewriteRun(
          kotlin(
            """
                val latest = if (true) {
                    "latest.release"
                } else {
                    "latest.integration"
                }
            """)
        );
    }

    @Disabled("Confirm where the RecieveType should be added.")
    @Test
    void inline() {
        rewriteRun(
          kotlin("class Spec"),
          kotlin("inline val Spec . `java-base` : String get ( ) = \"\"")
        );
    }

    @Test
    void getter() {
        rewriteRun(
          kotlin("class Spec"),
          kotlin("""
              val isEmpty: Boolean
                  get ( ) : Boolean = 1 == 1
          """)
        );
    }

    @Test
    void quotedIdentifier() {
        rewriteRun(
          kotlin("val `quoted-id` = true")
        );
    }
}
