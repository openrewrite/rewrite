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
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.RubyIsoVisitor;
import org.openrewrite.ruby.RubyParser;
import org.openrewrite.ruby.marker.PatternCase;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.ruby.Assertions.ruby;

/**
 * For a comprehensive summary of the history of pattern matching in Ruby, see
 * <a href="https://www.alchemists.io/articles/ruby_pattern_matching">this blog post</a>.
 */
public class PatternMatchingTest implements RewriteTest {

    @Test
    void booleanCheck() {
        rewriteRun(
          ruby(
            """
              basket = [{kind: "apple", quantity: 1}, {kind: "peach", quantity: 5}]
                            
              basket.any? { |fruit| fruit in {kind: /app/}}      # true
              basket.any? { |fruit| fruit in {kind: /berry/}}  # false
              """
          )
        );
    }

    @Test
    void hash() {
        rewriteRun(
          ruby(
            """
              { foo: 1, bar: 2 } in { foo: f }
              """
          )
        );
    }

    @Test
    void hashWithEmptyValue() {
        rewriteRun(
          ruby(
            """
              if user in {role: role, login:}
                puts "Granting admin scope: #{login}"
              end
              """
          )
        );
    }

    @Test
    void array() {
        rewriteRun(
          ruby(
            """
              [1, 2, 3] in [ Integer, Integer, Integer ]
              """
          )
        );
    }

    @Test
    void findPattern() {
        rewriteRun(
          ruby(
            """
              [1, 2, 3] in [ *, a, * ]
              """
          )
        );
    }

    @Test
    void namedSingleSplats() {
        rewriteRun(
          ruby(
            """
              [1, 2, 3] in [ *first, a, *last ]
              """
          )
        );
    }

    @Test
    void rightwardAssignment() {
        rewriteRun(
          ruby(
            """
              value => Numeric
              """
          )
        );
    }

    /**
     * The `=>` follows a subtree the Java printer prints, so it only stays an `=>` if the hand-off
     * back and forth between the two printers leaves the cursor where it found it.
     */
    @Test
    void rightwardAssignmentOfCase() {
        rewriteRun(
          ruby(
            """
              case error
              when Dependabot::NotImplemented
                {
                  "error-type": "not_implemented",
                  "error-detail": {message: error.message}
                }
              else
                {"error-type": "unknown"}
              end => details
              """
          )
        );
    }

    /**
     * Only optional in case statements, not in standalone patterns
     */
    @Test
    void optionalBracketsAndBraces() {
        rewriteRun(
          ruby(
            """
              case [1, 2, 3]
                in [Integer, Integer] then "match"  # With brackets.
                else "unmatched"
              end
                            
              case {a: 1, b: 2, c: 3}
                in {a: Integer} then "matched"      # With braces.
                else "unmatched"
              end
                            
              # Without brackets and braces.
              case [1, 2, 3]
                in Integer, Integer then "match"   # Without brackets.
                else "unmatched"
              end
                            
              case {a: 1, b: 2, c: 3}
                in a: Integer then "matched"       # Without braces.
                else "unmatched"
              end
              """
          )
        );
    }

    @Test
    void arraySplats() {
        rewriteRun(
          ruby(
            """
              case [:a, 1, :b, :c, 2]
                in *, Symbol, Symbol, * then "matched"
                else "unmatched"
              end
              """
          )
        );
    }

    @Test
    void emptyHash() {
        rewriteRun(
          ruby(
            """
              case {}
                in {} then "matched"
                else "unmatched"
              end
              """
          )
        );
    }

    @Test
    void voids() {
        rewriteRun(
          ruby(
            """
              case {a: 1, b: 2}
                in {a: Integer, **nil} then %(matched "a" part)
                in {a: Integer, b: Integer, **nil} then "matched whole hash"
                else "unmatched"
              end
              """
          )
        );
    }

    @Test
    void namedHashRest() {
        rewriteRun(
          ruby(
            """
              case params
                in {id: Integer, **rest} then update(id, rest)
                in {id: Integer, **nil} then "id only"
              end
              """
          )
        );
    }

    @Test
    void binding() {
        rewriteRun(
          ruby(
            """
              case value
                in Integer => n then n
              end
              """
          )
        );
    }

    @Test
    void nestedBindings() {
        rewriteRun(
          ruby(
            """
              case config
                in {db: {host: String => host, port: Integer => port}} then connect(host, port)
                in [Integer => first, *rest] then first
              end
              """
          )
        );
    }

    @Test
    void bindingOfWholeHash() {
        rewriteRun(
          ruby(
            """
              case params
                in {id: Integer, **rest} => whole then whole
              end
              """
          )
        );
    }

    @Test
    void alternation() {
        rewriteRun(
          ruby(
            """
              case status
                in :draft | :pending | :review then "in progress"
                in Integer | Float then "numeric"
              end
              """
          )
        );
    }

    @Test
    void alternationInsideArrayPattern() {
        rewriteRun(
          ruby(
            """
              case pair
                in [Integer | Float, String] then "matched"
              end
              """
          )
        );
    }

    @Test
    void pinnedLocalVariable() {
        rewriteRun(
          ruby(
            """
              expected = 42
              case value
                in ^expected then "exact"
              end
              """
          )
        );
    }

    @Test
    void pinnedVariables() {
        rewriteRun(
          ruby(
            """
              case pair
                in [_, ^@limit] then "at limit"
                in [_, ^@@limit] then "at class limit"
                in [_, ^$limit] then "at global limit"
              end
              """
          )
        );
    }

    @Test
    void pinnedExpression() {
        rewriteRun(
          ruby(
            """
              case pair
                in [n, ^( n + 2 )] then "twin primes"
              end
              """
          )
        );
    }

    @Test
    void guard() {
        rewriteRun(
          ruby(
            """
              case point
                in [x, y] if x > y then "below diagonal"
                in [x] unless x.zero? then "off axis"
              end
              """
          )
        );
    }

    @Test
    void guardOverAlternationAndBinding() {
        rewriteRun(
          ruby(
            """
              case value
                in Integer | Float => n if n.positive?
                  n
              end
              """
          )
        );
    }

    @Test
    void onelineWithoutBrackets() {
        rewriteRun(
          ruby(
            """
              [0, 1] => _, x
              {y: 2} => y:
              """
          )
        );
    }

    @Test
    void structArrayMatching() {
        rewriteRun(
          ruby(
            """
              Point = Struct.new :x, :y
              case Point[1, 2]
                in Point[..5, ..5] then "matched"
                else "unmatched"
              end
              """
          )
        );
    }

    @Test
    void structHashMatching() {
        rewriteRun(
          ruby(
            """
              Point = Struct.new :x, :y
              case Point[1, 2]
                in Point(x: ..5, y: ..5) then "matched"
                else "unmatched"
              end
              """
          )
        );
    }

    /**
     * Which keyword a case is spelled with is a property of the case, so moving one to another
     * parent must not turn a pattern match into a `when` (a `===` comparison).
     */
    @Test
    void theKeywordTravelsWithTheCase() {
        Rb.CompilationUnit cu = (Rb.CompilationUnit) RubyParser.builder().build()
                .parse("config => {db:}\n").findFirst().orElseThrow();
        J.Case pattern = new RubyIsoVisitor<AtomicReference<J.Case>>() {
            @Override
            public J.Case visitCase(J.Case aCase, AtomicReference<J.Case> found) {
                found.set(aCase);
                return aCase;
            }
        }.reduce(cu, new AtomicReference<>()).get();

        assertThat(pattern.getMarkers().findFirst(PatternCase.class))
                .get()
                .extracting(PatternCase::getOperator)
                .isEqualTo(PatternCase.Operator.Rightward);

        J.Switch moved = new J.Switch(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                new J.ControlParentheses<>(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                        JRightPadded.build(new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                                emptyList(), "config", null, null))),
                new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false),
                        singletonList(JRightPadded.build((Statement) pattern)), Space.format("\n")));
        assertThat(moved.printTrimmed(new Cursor(null, cu)))
                .contains("=>")
                .doesNotContain("when");
    }
}
