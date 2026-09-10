/*
 * Copyright 2023 the original author or authors.
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
import org.junitpioneer.jupiter.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

class ParenthesesTest implements RewriteTest {

    @Test
    void ifExpressionInParenthesesIsAnExpression() {
        AtomicBoolean sawParenthesizedIf = new AtomicBoolean(false);
        rewriteRun(
          kotlin(
            """
              fun test(a: Int, b: Boolean) {
                  if (a != (if (b) 15 else 16)) return
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, Integer p) {
                    J tree = parens.getTree();
                    if (tree instanceof K.StatementExpression && ((K.StatementExpression) tree).getStatement() instanceof J.If) {
                        sawParenthesizedIf.set(true);
                    }
                    assertThat(tree).isInstanceOf(Expression.class);
                    return super.visitParentheses(parens, p);
                }
            }.visit(cu, 0))
          )
        );
        assertThat(sawParenthesizedIf).isTrue();
    }

    @Test
    void variableTypeInParentheses() {
        rewriteRun(
          kotlin(
            """
              @Suppress("UNUSED_PARAMETER")
              class A {
                  internal fun <T> parseMappedType(
                      mappedType: (String),
                      toTypeName: (String.(isGenericParam: Boolean) -> T),
                      parameterize: ((current: Pair<T, MutableList<T>>) -> T),
                      onCloseBracketCallBack: ((current: Pair<T, MutableList<T>>, typeString: String) -> Unit)
                  ): T? {
                      return null
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/399")
    @Test
    void suspendModifier() {
        rewriteRun(
          kotlin(
            """
              @Suppress("UNUSED_PARAMETER")
              fun <R> runAsync(
                arg: (  suspend   ( ) -> R)? = null
              ) {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/441")
    @Test
    void parensOnType() {
        rewriteRun(
          kotlin(
            """
              val v  :   ( /*C*/ suspend ( param :   ( Int ) )  -> Unit ) = { }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/494")
    @Test
    void crazyNestedParenthesizedAndIsNullableType() {
        rewriteRun(
          kotlin(
            """
              fun x ( input : ( (  (   (    ) ->  String ?  )     ?     ) ?  ) ) {
              }
              """
          )
        );
    }
}
