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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

public class BinaryTest implements RewriteTest {

    @Test
    void equals() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                  val n = 0
                  val b = n == 0
                }
            """
          )
        );
    }

    @Test
    void endOfLineBreaks() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                    val b : Boolean = 1 == 1 // c1
                                && 2 == 2 // c2
                                && 3 == 3
                }
            """
          )
        );
    }

    @Test
    void bitwiseAnd() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                  val a = 0
                  val b = a and 1
                }
            """
          )
        );
    }

    @Test
    void bitwiseOr() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                  val a = 0
                  val b = a or 1
                }
            """
          )
        );
    }

    @Test
    void bitwiseXOr() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                  val a = 0
                  val b = a xor 1
                }
            """
          )
        );
    }

    @Test
    void inversion() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                  val a = 0
                  val b = a.inv()
                }
            """
          )
        );
    }

    @Test
    void shiftLeft() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                  val a = 0
                  val b = a shl 1
                }
            """
          )
        );
    }

    @Test
    void shiftRight() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                  val a = 0
                  val b = a shr 1
                }
            """
          )
        );
    }

    @Test
    void unsignedShiftRight() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                  val a = 0
                  val b = a ushr 1
                }
            """
          )
        );
    }

    @Test
    void identityOperation() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                  val a = 0 === 0
                }
            """
          )
        );
    }

    @Test
    void notIdentityOperation() {
        rewriteRun(
          kotlin(
            """
                fun method ( ) {
                  val a = 0 !== 0
                }
            """
          )
        );
    }
}
