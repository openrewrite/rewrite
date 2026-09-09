/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.scala.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.ScalaIsoVisitor;
import org.openrewrite.scala.ScalaParser;
import org.openrewrite.scala.tree.S;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class MergeSpacesVisitorTest {

    @Test
    void javaMergeVisitorIgnoresExpressionStatementContext() {
        J.Parentheses<?> parens = parentheses("(true && false)");
        S.ExpressionStatement expressionStatement = new S.ExpressionStatement(Tree.randomId(), parens);

        J merged = new org.openrewrite.java.format.MergeSpacesVisitor(emptyList()).visit(parens, expressionStatement);

        assertThat(merged).isSameAs(parens);
    }

    @Test
    void mergesSpacesInsideParenthesizedExpressionStatements() {
        J.Parentheses<?> originalParens = parentheses("(true  &&  false)");
        J.Parentheses<?> formattedParens = parentheses("(true && false)");

        S.ExpressionStatement original = new S.ExpressionStatement(Tree.randomId(), originalParens);
        S.ExpressionStatement formatted = new S.ExpressionStatement(Tree.randomId(), formattedParens);

        S.ExpressionStatement merged = (S.ExpressionStatement) new MergeSpacesVisitor(emptyList()).visit(original, formatted);

        assertThat(merged.printTrimmed()).isEqualTo("(true && false)");
    }

    private static J.Parentheses<?> parentheses(String expression) {
        S.CompilationUnit cu = ScalaParser.builder().build()
          .parse("""
            object Test {
              def method() = {
                %s
              }
            }
            """.formatted(expression))
          .map(S.CompilationUnit.class::cast)
          .findFirst()
          .orElseThrow();

        AtomicReference<J.Parentheses<?>> parentheses = new AtomicReference<>();
        new ScalaIsoVisitor<AtomicReference<J.Parentheses<?>>>() {
            @Override
            public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens,
                                                                    AtomicReference<J.Parentheses<?>> found) {
                found.set(parens);
                return super.visitParentheses(parens, found);
            }
        }.visit(cu, parentheses);

        return parentheses.get();
    }
}
