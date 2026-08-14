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
import org.openrewrite.test.RewriteTest;

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
}
