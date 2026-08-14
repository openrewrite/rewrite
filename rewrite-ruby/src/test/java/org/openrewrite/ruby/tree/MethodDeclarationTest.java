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

public class MethodDeclarationTest implements RewriteTest {

    @Test
    void kwargs() {
        rewriteRun(
          ruby(
            """
              def method(h, **kwargs)
                p h
              end
              """
          )
        );
    }

    @Test
    void vararg() {
        rewriteRun(
          ruby(
            """
              def sum(*args)
                  args.inject(0) { |s, x| s + x }
              end
              """
          )
        );
    }

    @Test
    void defaultArguments() {
        rewriteRun(
          ruby(
            """
              def sum(a = 1, b = 2)
                  a + b
              end
              """
          )
        );
    }

    @Test
    void noArgs() {
        rewriteRun(
          ruby(
            """
              def test
                  i = 42
              end
              """
          )
        );
    }

    @Test
    void noArgsWithParentheses() {
        rewriteRun(
          ruby(
            """
              def test()
                  i = 42
              end
              """
          )
        );
    }

    @Test
    void blockArg() {
        rewriteRun(
          ruby(
            """
              def accept(&arg)
              end
              """
          )
        );
    }

    @Test
    void singleArg() {
        rewriteRun(
          ruby(
            """
              def test(a1 = "Ruby")
                  puts "The programming language is #{a1}"
              end
              """
          )
        );
    }

    @Test
    void twoArgs() {
        rewriteRun(
          ruby(
            """
              def test(a1 = "Ruby", a2 = "Perl")
                  puts "The programming language is #{a1}"
              end
              """
          )
        );
    }

    @Test
    void argNoInitializer() {
        rewriteRun(
          ruby(
            """
              def sum(a1, a2)
                  a1 + a2
              end
              """
          )
        );
    }

    @Test
    void multipleBlockStatements() {
        rewriteRun(
          ruby(
            """
              def sum(a1, a2)
                  s = a1 + a2
                  s
              end
              """
          )
        );
    }

    /**
     * Search for "delegation" in <a href="https://www.ruby-lang.org/en/news/2019/12/12/separation-of-positional-and-keyword-arguments-in-ruby-3-0/">this blog</a>
     * for more explanation.
     */
    @Test
    void delegation() {
        rewriteRun(
          ruby(
            """
              def foo(...)
                  target(...)
              end
              """
          )
        );
    }

    /**
     * In Ruby 2.1, required keyword arguments were added. In Ruby 2.0, keyword
     * arguments must have default values.
     */
    @Test
    void keywordArguments() {
        rewriteRun(
          ruby(
            """
              def render_video(video, subscriber: false, has_access:)
              end
              """
          )
        );
    }
}
