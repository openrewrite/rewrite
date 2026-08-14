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
 * Method and class blocks are not Ruby blocks ({@link Rb.Block}),
 * but instead map to {@link org.openrewrite.java.tree.J.Block}.
 */
public class RubyBlockTest implements RewriteTest {

    /**
     * While the Ruby compiler treats multiple statements at the root note as a {@link org.jruby.ast.BlockNode},
     * we simply store the statements in a statement list on {@link org.openrewrite.ruby.tree.Rb.CompilationUnit}.
     * Otherwise, this just doesn't match the syntax we expect of blocks as they can occur elsewhere.
     */
    @Test
    void topLevelBlock() {
        rewriteRun(
          ruby(
            """
              a = 42
              b = 42
              """
          )
        );
    }

    @Test
    void multiline() {
        rewriteRun(
          ruby(
            """
              5.times do |i|
                puts i
                puts i
              end
              """
          )
        );
    }

    @Test
    void inline() {
        rewriteRun(
          ruby(
            """
              5.times { |i| puts i }
              """
          )
        );
    }

    @Test
    void blockArgument() {
        rewriteRun(
          ruby(
            """
              def wrap_in_h1
                  "<h1>#{yield}</h1>"
              end
              wrap_in_h1 { "Here's my heading" }
              """
          )
        );
    }
}
