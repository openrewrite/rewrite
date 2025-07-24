/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyUnusedAssignment", "GrUnnecessarySemicolon", "UnnecessaryQualifiedReference", "GrMethodMayBeStatic"})
class BinaryTest implements RewriteTest {

    @SuppressWarnings("GroovyConstantConditional")
    @Test
    void insideParentheses() {
        rewriteRun(
          groovy("(1 + 1)"),
          groovy("((1 + 1))"),

          // NOT inside parentheses, but verifies the parser's
          // test for "inside parentheses" condition
          groovy("( 1 ) + 1"),
          groovy("(1) + 1"),
          // And combine the two cases
          groovy("((1) + 1)")
        );
    }

    @Test
    void equals() {
        rewriteRun(
          groovy(
            """
              int n = 0;
              boolean b = n == 0;
              """
          )
        );
    }

    @Test
    void in() {
        rewriteRun(
          groovy(
            """
              def a = []
              boolean b = 42 in a;
              """
          )
        );
    }

    @Test
    void notIn() {
        rewriteRun(
          groovy(
            """
              def a = []
              boolean b = 42 !in a;
              """
          )
        );
    }

    @Test
    void withVariable() {
        rewriteRun(
          groovy(
            """
              def foo(int a) {
                  60 + a
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1531")
    @Test
    void regexFindOperator() {
        rewriteRun(
          groovy(
            """
              def REGEX = /\\d+/
              def text = "123"
              def result = text =~ REGEX
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1531")
    @Test
    void regexMatchOperator() {
        rewriteRun(
          groovy(
            """
              def REGEX = /\\d+/
              def text = "123"
              def result = text ==~ REGEX
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1520")
    @Test
    void minusEquals() {
        rewriteRun(
          groovy(
            """
              def a = 5
              a -= 5
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1520")
    @Test
    void divisionEquals() {
        rewriteRun(
          groovy(
            """
              def a = 5
              a /= 5
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1520")
    @Test
    void bitwiseAnd() {
        rewriteRun(
          groovy(
            """
              def a = 4
              a &= 1
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1520")
    @Test
    void bitwiseOr() {
        rewriteRun(
          groovy(
            """
              def a = 4
              a |= 1
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1520")
    @Test
    void bitwiseXOr() {
        rewriteRun(
          groovy(
            """
              def a = 4
              a ^= 1
              """
          )
        );
    }

    @Test
    void instanceOf() {
        rewriteRun(
          groovy(
            """
              def isString = "" instanceof java.lang.String
              """
          )
        );
    }

    @Test
    void spreadOperator() {
        rewriteRun(
          groovy("[ ] *. toString()"),
          groovy(
            """
              class A {
                  def value = 1
              }
              [ new A() ].findAll { it -> it.value == 1 } *. value = 2
              """
          )
        );
    }

    @Test
    void stringMultiplied() {
        rewriteRun(
          groovy(
            """
              def foo = "-" * 4
              """
          )
        );
    }

    @Test
    void stringMultipliedInParentheses() {
        rewriteRun(
          groovy(
            """
              def foo = ("-" * 4)
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4703")
    @Test
    void cxds() {
        rewriteRun(
          groovy(
            """
              def differenceInDays(int time) {
                  return (int) ((time)/(1000*60*60*24))
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4703")
    @Test
    void extraParensAroundInfixOxxxperator() {
        rewriteRun(
          groovy(
            """
              def timestamp(int hours, int minutes, int seconds) {
                  30 * (hours)
                  (((((hours))))) * 30
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4703")
    @Test
    void extraParensAroundInfixOperator() {
        rewriteRun(
          groovy(
            """
              def timestamp(int hours, int minutes, int seconds) {
                  (hours) * 60 * 60 + (minutes * 60) + seconds
              }
              def differenceInDays(int time) {
                  return (int) ((time)/(1000*60*60*24))
              }
              """
          )
        );
    }

    @Test
    void spaceShipOperator() {
        rewriteRun(
          groovy(
            """
              println(1 <=> 2)
              println('a' <=> 'z')
              def a = 'tiger'
              def b = 'cheetah'
              println(a <=> b)
              """
          ));
    }

    @Test
    void powerOperator() {
        rewriteRun(
          groovy(
            """
              4 ** 3
              """
          ));
    }

    @Test
    void powerAssigmentOperator() {
        rewriteRun(
          groovy(
            """
              def f = 3
              f **= 2
              """
          ));
    }

    @Test
    void identicalOperators() {
        rewriteRun(
          groovy(
            """
              class Creature {}
              
              def cat = new Creature()
              def copyCat = cat
              def lion = new Creature()
              
              assert cat === copyCat
              assert cat !== lion
              """
          ));
    }
}
