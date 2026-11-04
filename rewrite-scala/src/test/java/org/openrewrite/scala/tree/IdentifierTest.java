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
    void backtickIdentifierWithSpecialChars() {
        rewriteRun(
          scala(
            """
              object Test {
                val x = Foo.`text/html(UTF-8)`
              }
              """
          )
        );
    }

    @Test
    void backtickIdentifierAcrossContexts() {
        rewriteRun(
          scala(
            """
              import foo.`type`
              import foo.{`type` => t}

              class `My Class`
              object `My Object`
              class `Foo`

              case class CC(`type`: String)

              object O {
                val `type` = 1
                var `var name` = 2
                val withType: `Foo` = new `Foo`
                val ref = `type`

                def `my method`() = 1
                def `other`() = 1
                val call = `other`()

                def f(`type`: Int): Int = `type`
                val named = f(`type` = 5)

                val lambda = (`type`: Int) => `type` + 1
                val xs = for (`type` <- List(1, 2)) yield `type`
                val sel = List(1).`type`
              }
              """
          )
        );
    }

    @Test
    void backtickIdentifierAsMethodInvocation() {
        rewriteRun(
          scala(
            """
              object Test {
                def f(x: Any): Any = x.`type`()
              }
              """
          )
        );
    }

    @Test
    void backtickIdentifierAsInfixInvocation() {
        rewriteRun(
          scala(
            """
              object Test {
                def f(a: Int, b: Int): Int = a `min` b
              }
              """
          )
        );
    }

    @Test
    void backtickIdentifierAsPostfixInvocation() {
        rewriteRun(
          scala(
            """
              object Test {
                def f(a: List[Int]): Any = a `head`
              }
              """
          )
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
          scala("++")
        );
    }
}
