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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"GroovyUnusedAssignment", "GrUnnecessarySemicolon", "UnnecessaryQualifiedReference", "GrMethodMayBeStatic"})
class BinaryTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/8958")
    @ParameterizedTest
    @ValueSource(strings = {
      "a[0,1]",
      "a[0,1][2]",
      "a[[0,1], [2,3]]",
      "a[(0),1]",
      "a[([0,1]),2]",
      "a[b[0,1],2]",
      "a[*xs]",
      "a[0,*xs]",
      "a[0,1] = 2",
      "a[0,1]; b[2]",
      "a[0..1,2]",
      "a[ /* first */ 0 /* before comma */, /* second */ 1 /* end */ ]",
      "a[\n    0, // first index\n    1\n]",
      "commandLine ['cmd', '/c'] + azCmd"
    })
    void multiIndexAccess(String source) {
        rewriteRun(
          groovy(source)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "a[0]",
      "a[[0,1]]",
      "a[([0,1])]",
      "a[[*xs]]",
      "commandLine(['cmd', '/c'] + azCmd)"
    })
    void singleIndexAccessAndListLiteral(String source) {
        rewriteRun(
          groovy(source)
        );
    }

    @Test
    void multiIndexListCanBeMovedOutsideAccess() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GroovyVisitor<>() {
              @Override
              public J visitBinary(G.Binary binary, ExecutionContext ctx) {
                  return binary.getRight().withPrefix(binary.getPrefix());
              }
          })),
          groovy("def indices = a[0,1]", "def indices = [0,1]")
        );
    }

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

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "d1 + d2 | double",
      "(d1 + d2) * 2 | double",
      "f * f | double",
      "i + d1 | double",
      "i + l | long",
      "boxed + i | int",
      "b + s | int",
      "i - i | int",
      "i % l | long",
      "i / i | java.math.BigDecimal",
      "i / f | double",
      "big + i | java.math.BigInteger",
      "big / i | java.math.BigDecimal",
      "i & l | long",
      "l << i | long",
      "i >>> 1 | int",
      "flag & flag | boolean",
      "i < d1 | boolean",
      "str == str | boolean",
      "flag && flag | boolean",
      "str + i | java.lang.String",
      "i + str | java.lang.String",
      "i + 1.5 | java.math.BigDecimal",
      "-1.5 + i | java.math.BigDecimal",
      "i / 2.0 | java.math.BigDecimal",
      "big * 0.1 | java.math.BigDecimal",
      "1.5 * i + 1 | java.math.BigDecimal",
      "d1 + 1.5 | double",
      "i + 1.5d | double"
    })
    void binaryTypeFollowsOperands(String expression, String expectedType) {
        rewriteRun(
          groovy(
            """
              double d1 = 1.0d
              double d2 = 2.0d
              float f = 1.0f
              int i = 1
              long l = 2L
              byte b = 1
              short s = 1
              Integer boxed = 3
              BigInteger big = BigInteger.ONE
              boolean flag = true
              String str = "x"
              def result = %s
              """.formatted(expression),
            spec -> spec.beforeRecipe(cu -> {
                var result = (J.VariableDeclarations) cu.getStatements().getLast();
                var initializer = requireNonNull(result.getVariables().getFirst().getInitializer());
                assertThat(String.valueOf(initializer.getType())).isEqualTo(expectedType);
            })
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "f % f",
      "f << 1",
      "f & f",
      "list + 1",
      "list << 1",
      "obj() + obj()"
    })
    void binaryTypeStaysObjectWhereGroovyDecidesAtRuntime(String expression) {
        rewriteRun(
          groovy(
            """
              float f = 1.0f
              List<Integer> list = []
              Object obj() { 1 }
              def result = %s
              """.formatted(expression),
            spec -> spec.beforeRecipe(cu -> {
                var result = (J.VariableDeclarations) cu.getStatements().getLast();
                var initializer = requireNonNull(result.getVariables().getFirst().getInitializer());
                assertThat(TypeUtils.isObject(initializer.getType())).as(String.valueOf(initializer.getType())).isTrue();
            })
          )
        );
    }
}
