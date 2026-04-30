/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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

class InfixOpTest implements RewriteTest {

    @Test
    void tripleEquals() {
        rewriteRun(
          scala(
            """
              object Test {
                implicit class StringOps(val s: String) {
                  def ===(other: String): Boolean = s == other
                }
                val x = "a" === "b"
              }
              """
          )
        );
    }

    @Test
    void tripleEqualsInFilter() {
        rewriteRun(
          scala(
            """
              object Test {
                implicit class StringOps(val s: String) {
                  def ===(other: String): Boolean = s == other
                }
                val xs = List("a", "b").filter(_ === "a")
              }
              """
          )
        );
    }

    @Test
    void customOperatorWithEqualsSuffix() {
        rewriteRun(
          scala(
            """
              object Test {
                class Box(val value: Int) {
                  def :=(other: Int): Box = new Box(other)
                }
                val b = new Box(1)
                val c = b := 5
              }
              """
          )
        );
    }

    @Test
    void notEquals() {
        rewriteRun(
          scala(
            """
              object Test {
                val x: Boolean = 1 != 2
              }
              """
          )
        );
    }

    @Test
    void compoundAssignPlus() {
        rewriteRun(
          scala(
            """
              object Test {
                def run(): Unit = {
                  var x = 1
                  x += 2
                }
              }
              """
          )
        );
    }
}
