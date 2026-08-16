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

    /**
     * The `(` opening the body is a grouped expression; a parameter list has to be on the same
     * line as the method name.
     */
    @Test
    void bodyStartingWithParenthesizedExpression() {
        rewriteRun(
          ruby(
            """
              def offset
                (page - 1) * per_page
              end
              """
          )
        );
    }

    /**
     * Ruby 3.0 endless method definitions.
     */
    @Test
    void endless() {
        rewriteRun(
          ruby(
            """
              def pi = 3.14
              """
          )
        );
    }

    @Test
    void endlessWithParameters() {
        rewriteRun(
          ruby(
            """
              def area(r) = 3.14 * r * r
              """
          )
        );
    }

    @Test
    void endlessWithDefaultParameter() {
        rewriteRun(
          ruby(
            """
              def scale(n, by = 2) = n * by
              """
          )
        );
    }

    @Test
    void endlessClassMethod() {
        rewriteRun(
          ruby(
            """
              class Money
                def self.zero = new(0)
              end
              """
          )
        );
    }

    /**
     * Ruby 3.1 relaxed endless method bodies to allow a command call without parentheses.
     */
    @Test
    void endlessCommandBody() {
        rewriteRun(
          ruby(
            """
              def log = puts "hello"
              """
          )
        );
    }

    @Test
    void endlessAmongOtherMethods() {
        rewriteRun(
          ruby(
            """
              class Money
                def initialize(cents)
                  @cents = cents
                end

                def to_s = "$#{@cents / 100.0}"

                def zero?
                  @cents == 0
                end
              end
              """
          )
        );
    }

    /**
     * Only the parenthesized form can omit the space before `=`; `def pi=3.14` is a setter
     * definition to Ruby, not an endless method.
     */
    @Test
    void endlessNoSpaceAroundEquals() {
        rewriteRun(
          ruby(
            """
              def pi()=3.14
              """
          )
        );
    }

    /**
     * A method named `==` must not be mistaken for an endless method body.
     */
    @Test
    void equalityOperatorMethod() {
        rewriteRun(
          ruby(
            """
              class Money
                def ==(other)
                  other.cents == @cents
                end
              end
              """
          )
        );
    }

    /**
     * `&nil` refuses a block and `**nil` refuses keyword arguments.
     */
    @Test
    void refusedParameters() {
        rewriteRun(
          ruby(
            """
              def f(&nil)
              end
              def g(a, **nil)
              end
              def h(a, ** nil, & nil)
              end
              """
          )
        );
    }
}
