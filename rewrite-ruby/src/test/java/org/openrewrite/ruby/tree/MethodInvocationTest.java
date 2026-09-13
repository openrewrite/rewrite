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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.RubyIsoVisitor;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class MethodInvocationTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"\"test\"", "(\"test\")"})
    void print(String args) {
        rewriteRun(
          ruby(
            """
              print %s
              """.formatted(args)
          )
        );
    }

    @Test
    void blockPass() {
        rewriteRun(
          ruby(
            """
              accept(&consumer)
              """
          )
        );
    }

    @Test
    void safeNavigation() {
        rewriteRun(
          ruby(
            """
              obj&.accept(consumer)
              """
          )
        );
    }

    @Test
    void blockLastArgument() {
        rewriteRun(
          ruby(
            """
              accept(1) { |a| a }
              """
          )
        );
    }

    @Test
    void callSugar() {
        rewriteRun(
          ruby(
            """
              Sweep.()
              MarkForToken.(t)
              MarkForToken.(t, 1)
              obj&.()
              """
          )
        );
    }

    @Test
    void callSugarWithBlock() {
        rewriteRun(
          ruby(
            """
              Sweep.() { |a| a }
              """
          )
        );
    }

    @Test
    void explicitCall() {
        rewriteRun(
          ruby(
            """
              Sweep.call()
              Sweep.call
              MarkForToken.call(t)
              """
          )
        );
    }

    @Test
    void colon2Call() {
        rewriteRun(
          ruby(
            """
              Nokogiri::XML(response.body)
              WEBrick::Log::new(log_path)
              Integer::sqrt(9)
              """
          )
        );
    }

    @Test
    void noParens() {
        rewriteRun(
          ruby(
            """
              Struct.new :x, :y
              """
          )
        );
    }

    @Test
    void memberReferenceNewClass() {
        rewriteRun(
          ruby(
            """
              Gem::Specification.new do |spec|
              end
              """
          )
        );
    }

    @Test
    void nested() {
        rewriteRun(
          ruby(
            """
              expect(map(&:name)).to
              """
          )
        );
    }

    @Test
    void keywordArgumentsBeforeBlockArgument() {
        rewriteRun(
          ruby(
            """
              tag.span(class: 'wrapper', data: {id: 1}, &block)
              """
          )
        );
    }

    @Test
    void blockOnConstructor() {
        rewriteRun(
          ruby(
            """
              klass = Class.new(described_class) do
                def name
                  "anonymous"
                end
              end
              """
          )
        );
    }

    @Test
    void blockOnConstructorWithoutArguments() {
        rewriteRun(
          ruby(
            """
              threads << Thread.new do
                work
              end
              """
          )
        );
    }

    /**
     * A `(` that opens the next line is a grouped expression, not the argument list of the call
     * that happens to end the line before it.
     */
    @Test
    void parenthesizedExpressionAfterParenthesisLessCall() {
        rewriteRun(
          ruby(
            """
              def default_value
                if self[:default_value].present?
                  (User.current.today + Integer(self[:default_value], 10)).to_s
                end
              end
              """
          )
        );
    }

    @Test
    void parenthesizedExpressionAfterConstructor() {
        rewriteRun(
          ruby(
            """
              q = ProjectQuery.new

              ((q.count / 2) + 1).times do |i|
                p i
              end
              """
          )
        );
    }

    @Test
    void safeNavigationIndex() {
        rewriteRun(
          ruby(
            """
              source_type = new_source&.[](:type)
              first = match&.captures&.[](1)
              """
          )
        );
    }

    @Test
    void trailingCommaInMultilineArguments() {
        rewriteRun(
          ruby(
            """
              super(
                name: name,
                requirement: requirement,
              )
              """
          )
        );
    }

    @Test
    void trailingCommaAfterPositionalArgument() {
        rewriteRun(
          ruby(
            """
              build(
                name,
              )
              """
          )
        );
    }

    @Test
    void indexWrittenAsMethod() {
        rewriteRun(
          ruby(
            """
              value = details.[]('version')
              """
          )
        );
    }

    /**
     * A block rides in the argument container, so appending an argument — which is what a recipe
     * does — leaves it in the middle of the list and it still has to print after the parentheses.
     */
    @Test
    void appendingAnArgumentToACallWithABlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new RubyIsoVisitor<ExecutionContext>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (!"foo".equals(m.getSimpleName()) || m.getArguments().size() != 2) {
                      return m;
                  }
                  Expression two = new J.Literal(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                          2, "2", null, JavaType.Primitive.Int);
                  return m.withArguments(ListUtils.concat(m.getArguments(), two));
              }
          })),
          ruby(
            """
              foo(1) { |x| x }
              """,
            """
              foo(1, 2) { |x| x }
              """
          )
        );
    }
}
