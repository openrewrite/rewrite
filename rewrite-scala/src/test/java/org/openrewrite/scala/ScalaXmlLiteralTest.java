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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class ScalaXmlLiteralTest implements RewriteTest {

    @Test
    void simpleElement() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = <message>Hello World!</message>
                }
                """
            )
        );
    }

    @Test
    void selfClosingElement() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = <br/>
                }
                """
            )
        );
    }

    @Test
    void nestedElements() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = <a><b>text</b></a>
                }
                """
            )
        );
    }

    @Test
    void elementWithAttributes() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = <a href="x">y</a>
                }
                """
            )
        );
    }

    @Test
    void adjacentElements() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = <a/><b/>
                }
                """
            )
        );
    }

    @Test
    void xmlAsMethodBody() {
        rewriteRun(
            scala(
                """
                object Test {
                  def greet: scala.xml.Elem = <hello/>
                }
                """
            )
        );
    }
}
