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
package org.openrewrite.ruby;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.ruby.tree.Rb;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RubyVisitorTest {

    /**
     * Recipes are written against the {@code visitStatement}/{@code visitExpression} hooks, so
     * every Rb type that is one has to route through them.
     */
    @Test
    void everyStatementAndExpressionReachesItsHook() {
        List<String> seen = new ArrayList<>();
        new RubyIsoVisitor<Integer>() {
            @Override
            public Statement visitStatement(Statement statement, Integer p) {
                seen.add(statement.getClass().getSimpleName());
                return statement;
            }

            @Override
            public Expression visitExpression(Expression expression, Integer p) {
                seen.add(expression.getClass().getSimpleName());
                return expression;
            }
        }.visit(parse("""
          BEGIN { setup }
          puts "hi #{name}"
          begin
            work
          rescue Errors::Timeout => e
            retry
          end
          END { teardown }
          """), 0);

        assertThat(seen).contains("PreExecution", "PostExecution", "ExpressionTypeTree", "Value");
    }

    /**
     * A padded child's before/after space is only reachable if the visitor goes through the
     * padding rather than straight to the element.
     */
    @Test
    void paddingAroundASubArrayIndexIsVisited() {
        List<String> spaces = new ArrayList<>();
        new RubyIsoVisitor<Integer>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer p) {
                if (!space.getWhitespace().isEmpty()) {
                    spaces.add(space.getWhitespace());
                }
                return space;
            }
        }.visit(parse("a[0   ,  2]\n"), 0);

        assertThat(spaces).contains("   ", "  ");
    }

    private static J parse(String source) {
        return (Rb.CompilationUnit) RubyParser.builder().build().parse(source)
                .findFirst().orElseThrow();
    }
}
