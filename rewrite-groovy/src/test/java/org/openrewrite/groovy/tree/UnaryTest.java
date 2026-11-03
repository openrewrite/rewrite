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
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyUnusedAssignment", "GroovyUnusedIncOrDec", "GrUnnecessarySemicolon"})
class UnaryTest implements RewriteTest {
    @Test
    void format() {
        rewriteRun(
          groovy(
            """
              int i = 0;
              int j = ++i;
              int k = i ++;
              """
          )
        );
    }

    @Test
    void postfix() {
        rewriteRun(
          groovy(
            """
              int k = i ++;
              """
          )
        );
    }

    @Test
    void prefix() {
        rewriteRun(
          groovy(
            """
              int k = ++i;
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1524")
    @SuppressWarnings("GroovyPointlessBoolean")
    @Test
    void negation() {
        rewriteRun(
          groovy(
            """
              def a = !true
              """
          )
        );
    }

    @Test
    void negationOfParenthesizedPropertyAccess() {
        rewriteRun(
          groovy(
            """
              !(new String("a").bytes)
              """
          )
        );
    }

    @Test
    void negationOfParenthesizedPropertyAccessOnLocal() {
        rewriteRun(
          groovy(
            """
              def x = "a"
              def b = !(x.bytes)
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9490")
    @ParameterizedTest
    @ValueSource(strings = {
      "+f(3)", "-f(3)", "+ f(3)", "+new BigDecimal(3)", "-new BigDecimal(3)",
      "+'b'", "-'b'", "+\"b\"", "+\"${x}\"", "+x", "-x", "+this.f(3)", "+(f(3))", "-(x)", "(-x)",
      "+m.k", "+l[0]", "-f(3) + 1"
    })
    void signBeforeExpression(String expression) {
        rewriteRun(
          groovy(
            """
              def f(a) { a }
              def x = 3
              def m = [k: 1]
              def l = [1]
              def u = %s
              """.formatted(expression)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9490")
    @Test
    void signIsUnaryOperator() {
        rewriteRun(
          groovy(
            """
              def f(a) { a }
              def x = 3
              def u = +f(3)
              def v = -x
              """,
            spec -> spec.afterRecipe(cu -> assertThat(new GroovyIsoVisitor<List<J.Unary.Type>>() {
                @Override
                public J.Unary visitUnary(J.Unary unary, List<J.Unary.Type> operators) {
                    operators.add(unary.getOperator());
                    return super.visitUnary(unary, operators);
                }
            }.reduce(cu, new ArrayList<>())).containsExactly(J.Unary.Type.Positive, J.Unary.Type.Negative))
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9491")
    @Test
    void signBeforeStringLiteralStatement() {
        rewriteRun(
          groovy(
            """
              return 'a'; + 'b'
              """
          )
        );
    }

    @Test
    void negativeNumberLiteral() {
        rewriteRun(
          groovy(
            """
              def a = -1
              def b = + 2.5
              def c = -(3)
              """
          )
        );
    }
}
