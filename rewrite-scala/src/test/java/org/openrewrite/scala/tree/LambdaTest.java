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

class LambdaTest implements RewriteTest {

    @Test
    void simpleLambda() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f = (x: Int) => x + 1
                }
                """
            )
        );
    }

    @Test
    void lambdaWithMultipleParams() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f = (x: Int, y: Int) => x + y
                }
                """
            )
        );
    }

    @Test
    void lambdaWithTypeInference() {
        rewriteRun(
            scala(
                """
                object Test {
                  val list = List(1, 2, 3)
                  val doubled = list.map(x => x * 2)
                }
                """
            )
        );
    }

    @Test
    void lambdaWithUnderscore() {
        rewriteRun(
            scala(
                """
                object Test {
                  val list = List(1, 2, 3)
                  val doubled = list.map(_ * 2)
                }
                """
            )
        );
    }

    @Test
    void lambdaWithBlock() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f = (x: Int) => {
                    val y = x + 1
                    y * 2
                  }
                }
                """
            )
        );
    }

    @Test
    void multiLineLambda() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f = (x: Int) =>
                    x + 1
                }
                """
            )
        );
    }

    @Test
    void nestedLambda() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f = (x: Int) => (y: Int) => x + y
                }
                """
            )
        );
    }

    @Test
    void lambdaAsMethodArgument() {
        rewriteRun(
            scala(
                """
                object Test {
                  List(1, 2, 3).filter(x => x > 1)
                }
                """
            )
        );
    }

    @Test
    void noParamLambda() {
        rewriteRun(
            scala(
                """
                object Test {
                  val f = () => println("hello")
                }
                """
            )
        );
    }
}