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
    void addition() {
        rewriteRun(
          kotlin("val a = 1 + 1")
        );
    }

    @Test
    void singleVariableDeclarationWithTypeConstraint() {
        rewriteRun(
          kotlin("val a: Int = 1")
        );
    }

    @Test
    void anonymousObject() {
        rewriteRun(
          kotlin("open class Test"),
          kotlin("val o : Test = object : Test ( ) { }")
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

    @Test
    void inline() {
        rewriteRun(
          kotlin("class Spec"),
          kotlin("inline val Spec . `java-base` : String get ( ) = \"  \"")
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

    @Test
    void stringTemplate() {
        rewriteRun(
          kotlin("""
              val a = "Hello"
              val b = "World"
              val c = "${a} ${b}!"
          """)
        );
    }

    @Test
    void stringTemplateNoBraces() {
        rewriteRun(
          kotlin("""
              val a = "Hello"
              val b = "World"
              val c = "$a $b!"
          """)
        );
    }

    @Test
    void propertyAccessor() {
        rewriteRun(
          kotlin("""
              class Test {
                  val value = 10
              }
              val a = Test()
              val b = "${a.value}"
          """)
        );
    }

    @Test
    void multipleFieldAccess() {
        rewriteRun(
          kotlin("""
              class Test {
                  val testValue = Inner()
                  class Inner {
                      val innerValue = 10
                  }
              }

              val a = Test()
              val b = "${a.testValue.innerValue}"
          """)
        );
    }

    @Test
    void tripleQuotedString() {
        rewriteRun(
          kotlin("""
              val template = \"\"\"
                Hello world!
              \"\"\"
          """)
        );
    }
}
