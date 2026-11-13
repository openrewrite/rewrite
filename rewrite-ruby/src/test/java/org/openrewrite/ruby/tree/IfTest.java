/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.RubyParser;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.ruby.Assertions.ruby;

public class IfTest implements RewriteTest {

    @Test
    void nextUnless() {
        rewriteRun(
          ruby(
            """
              next unless true
              """
          )
        );
    }

    @Test
    void unlessElse() {
        rewriteRun(
          ruby(
            """
              unless a
                  puts "a"
              else
                  puts "b"
              end
              """
          )
        );
    }

    /**
     * The nested `unless` modifier used to be picked up as the enclosing conditional, which dropped
     * the outer `else`.
     */
    @Test
    void elseAfterNestedUnlessModifier() {
        rewriteRun(
          ruby(
            """
              def m(a, b)
                if a
                  return [a, nil] unless b
                  2
                else
                  3
                end
              end
              """
          )
        );
    }

    /**
     * The else body is itself a conditional, which must not be confused with an `elsif`.
     */
    @Test
    void elseWholeBodyIsAConditional() {
        rewriteRun(
          ruby(
            """
              if a
                puts "a"
              else
                unless b
                  puts "b"
                end
              end
              """
          )
        );
    }

    @Test
    void unlessStatement() {
        rewriteRun(
          ruby(
            """
              unless h == "hello"
                  puts "world"
              end
              """
          )
        );
    }

    @Test
    void ifModifier() {
        rewriteRun(
          ruby(
            """
              a = 0
              puts "hello" if a == 0
              """
          )
        );
    }

    @Test
    void ifModifierImplicitStatement() {
        rewriteRun(
          ruby(
            """
              latest_version_for_git_dependency if git_dependency?
              """
          )
        );
    }

    @Test
    void singleIf() {
        rewriteRun(
          ruby(
            """
              if n == 42 then
                  puts "42"
              end
              """
          )
        );
    }

    @Test
    void ifElseIfElse() {
        rewriteRun(
          ruby(
            """
              if n == 42 then
                  puts "42"
              elsif n > 42 then
                  puts "greater 42"
              elsif n < 42
                  puts "less 42"
              else
                  puts "something else"
              end
              """
          )
        );
    }

    @Test
    void ifElseIf() {
        rewriteRun(
          ruby(
            """
              if n == 42 then
                  puts "42"
              elsif n < 42
                  puts "less 42"
              end
              """
          )
        );
    }

    @Test
    void ifElse() {
        rewriteRun(
          ruby(
            """
              if n == 42 then
                  puts "42"
              else
                  puts "less 42"
              end
              """
          )
        );
    }

    @Test
    void match() {
        rewriteRun(
          ruby(
            """
              if /lit/ then
              end
              """
          )
        );
    }

    @Test
    void match2() {
        rewriteRun(
          ruby(
            """
              if /#{recv}/ then
              end
              """
          )
        );
    }

    @Test
    void ternary() {
        rewriteRun(
          ruby(
            """
              a = 0
              source_address ? 1 : 2
              """
          )
        );
    }

    @Test
    void noThen() {
        rewriteRun(
          ruby(
            """
              if current_version &&
                 version_class.correct?(current_version) &&
                 version_class.new(current_version).prerelease?
                return true
              end
              """
          )
        );
    }

    @Test
    void multipleStatementElse() {
        rewriteRun(
          ruby(
            """
              def registry_source_details_from(source_string)
                parts = source_string.split("//").first.split("/")
                if parts.count == 3
                  puts 3
                elsif parts.count == 4
                  puts 4
                else
                  msg = "Invalid registry source specified: '#{source_string}'"
                  raise DependencyFileNotEvaluatable, msg
                end
              end
                      
              def git_dependency_name(name, source)
              end
              """
          )
        );
    }

    /**
     * Ruby has no `x if c else y`, so a recipe that adds an else has to drop the modifier marker
     * as well; the printer must not quietly discard the else part instead.
     */
    @Test
    void modifierIfCannotHoldAnElsePart() {
        Rb.CompilationUnit cu = parse("raise err if bad\n");
        J.If iff = (J.If) cu.getStatements().get(0);
        Statement elseBody = parse("puts 1\n").getStatements().get(0);
        Rb.CompilationUnit withElse = cu.withStatements(singletonList(iff.withElsePart(
                new J.If.Else(Tree.randomId(), Space.format("\n"), Markers.EMPTY,
                        JRightPadded.build(elseBody)))));

        assertThatThrownBy(withElse::printAll)
                .rootCause()
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("modifier `if` cannot have an else part");
    }

    private static Rb.CompilationUnit parse(String source) {
        return (Rb.CompilationUnit) RubyParser.builder().build().parse(source)
                .findFirst().orElseThrow();
    }
}
