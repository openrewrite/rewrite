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
package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.ruby.RubyIsoVisitor;
import org.openrewrite.ruby.RubyParser;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.ruby.Assertions.ruby;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * A heredoc's body lives outside its node's span, which makes both ends of it — the terminator scan
 * in the parser and the replay in the printer — worth pinning down on their own.
 */
class HeredocTest implements RewriteTest {

    @Test
    void bodyLineBeginningWithTheTerminator() {
        rewriteRun(
          ruby(
            """
              sql = <<~SQL
                SQL fragment
              SQL
              puts sql
              """
          )
        );
    }

    @Test
    void bodyLineBeginningWithTheTerminatorAsAPrefix() {
        rewriteRun(
          ruby(
            """
              q = <<~SQL
                SQL_MODE = 1
              SQL
              puts q
              """
          )
        );
    }

    @Test
    void plainHeredoc() {
        rewriteRun(
          ruby(
            """
              x = <<EOS
                hello
              EOS
              puts x
              """
          )
        );
    }

    /**
     * Only {@code <<~} and {@code <<-} may indent the terminator; for a plain {@code <<ID} an
     * indented line that reads exactly like the id is body text.
     */
    @Test
    void plainHeredocTerminatesOnlyAtColumnZero() {
        rewriteRun(
          ruby(
            """
              x = <<EOS
                EOS
              EOS
              puts x
              """
          )
        );
    }

    @Test
    void indentedTerminator() {
        rewriteRun(
          ruby(
            """
              def sql
                x = <<-SQL
                  SELECT 1
                  SQL
                puts x
              end
              """
          )
        );
    }

    @Test
    void heredocEndsTheFile() {
        rewriteRun(
          ruby(
            """
              puts <<~MSG
                MSG is broken
              MSG
              """
          )
        );
    }

    @Test
    void emptyBody() {
        rewriteRun(
          ruby(
            """
              x = <<~SQL
              SQL
              puts x
              """
          )
        );
    }

    /**
     * Serialization and any prefix-rewriting recipe hand back an equal-but-distinct {@link Space},
     * which must not cost the heredoc its body.
     */
    @Test
    void rebuildingEverySpaceKeepsTheBody() {
        String source = "x = <<~A # note\n  body\nA\nputs x\n";
        SourceFile rebuilt = (SourceFile) new RubyIsoVisitor<Integer>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer p) {
                return Space.build(space.getWhitespace(), space.getComments());
            }
        }.visitNonNull(parse(source), 0);
        assertThat(rebuilt.printAll()).isEqualTo(source);
    }

    /**
     * The body has no following statement to hang off of any more, and dropping it would leave the
     * file unparseable.
     */
    @Test
    void deletingTheLastStatementDrainsTheBody() {
        Rb.CompilationUnit cu = parse("x = <<-SQL\n  SELECT 1\nSQL\ny = 2\n");
        assertThat(cu.withStatements(cu.getStatements().subList(0, 1)).printAll())
                .isEqualTo("x = <<-SQL\n  SELECT 1\nSQL\n");
    }

    @Test
    void insertingAStatementAfterAHeredocKeepsTheBodyInPlace() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new RubyIsoVisitor<ExecutionContext>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  if (b.getStatements().size() != 1) {
                      return b;
                  }
                  Statement puts = parseStatement("puts \"x\"").withPrefix(Space.format("\n  "));
                  return b.withStatements(ListUtils.concat(b.getStatements(), puts));
              }
          })),
          ruby(
            """
              def sql
                <<-SQL
                  SELECT 1
                SQL
              end
              """,
            """
              def sql
                <<-SQL
                  SELECT 1
                SQL
                puts "x"
              end
              """
          )
        );
    }

    @Test
    void deletingTheStatementAfterAHeredocKeepsTheBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new RubyIsoVisitor<ExecutionContext>() {
              @Override
              public Rb.CompilationUnit visitCompilationUnit(Rb.CompilationUnit cu, ExecutionContext ctx) {
                  List<Statement> statements = cu.getStatements();
                  return statements.size() != 3 ? cu :
                          cu.withStatements(Arrays.asList(statements.get(0), statements.get(2)));
              }
          })),
          ruby(
            """
              x = <<-SQL
                SELECT 1
              SQL
              y = 2
              puts x
              """,
            """
              x = <<-SQL
                SELECT 1
              SQL
              puts x
              """
          )
        );
    }

    /**
     * The body is an ordinary {@link J.Literal}, so a recipe that has never heard of heredocs
     * rewrites it and the marker and terminator have to survive.
     */
    @Test
    void rewritingTheBodyLiteral() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new RubyIsoVisitor<ExecutionContext>() {
              @Override
              public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                  return literal.getValue() != null && literal.getValue().toString().contains("SELECT 1") ?
                          literal.withValue("SELECT 2\n").withValueSource("  SELECT 2\n") : literal;
              }
          })),
          ruby(
            """
              x = <<~SQL
                SELECT 1
              SQL
              """,
            """
              x = <<~SQL
                SELECT 2
              SQL
              """
          )
        );
    }

    /**
     * The naive literal idiom writes a quoted, newline-free source; the terminator still has to
     * start a line of its own so that the output parses.
     */
    @Test
    void rewritingTheBodyLiteralWithoutATrailingNewline() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new RubyIsoVisitor<ExecutionContext>() {
              @Override
              public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                  return literal.getValue() != null && literal.getValue().toString().contains("SELECT 1") ?
                          literal.withValue("SELECT 2").withValueSource("\"SELECT 2\"") : literal;
              }
          })),
          ruby(
            """
              x = <<~SQL
                SELECT 1
              SQL
              """,
            """
              x = <<~SQL
              "SELECT 2"
              SQL
              """
          )
        );
    }

    private static Rb.CompilationUnit parse(String source) {
        return (Rb.CompilationUnit) RubyParser.builder().build().parse(source)
                .findFirst().orElseThrow();
    }

    private static Statement parseStatement(String source) {
        return parse(source).getStatements().get(0);
    }
}
