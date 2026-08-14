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
import org.openrewrite.java.tree.J;
import org.openrewrite.ruby.RubyVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void destructuredParameters() {
        rewriteRun(
          ruby(
            """
              h.each { |(a, b)| p a }
              """
          )
        );
    }

    @Test
    void destructuredParametersAfterPlainParameter() {
        rewriteRun(
          ruby(
            """
              h.each { |k, (v1, v2)| p k }
              """
          )
        );
    }

    @Test
    void destructuredParametersWithSplat() {
        rewriteRun(
          ruby(
            """
              h.each do |k, (v1, *rest)|
                p k
              end
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

    /**
     * A numbered parameter is written nowhere but the body, so the block has no parameter list and
     * `_1` is an ordinary identifier.
     */
    @Test
    void numberedParameters() {
        rewriteRun(
          ruby(
            """
              users.map { _1.name }
              pairs.each { puts "#{_1}=#{_2}" }
              users.select { _1.active? }.map { _1.email }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(blocksWithParameters(cu)).containsOnly(false))
          )
        );
    }

    @Test
    void numberedParametersInsideMultilineBlock() {
        rewriteRun(
          ruby(
            """
              rows.each do |row|
                row.values.map { _1.to_s }
              end
              """
          )
        );
    }

    @Test
    void itParameter() {
        rewriteRun(
          ruby(
            """
              [1, 2, 3].map { it * 2 }
              users.select { it.active? }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(blocksWithParameters(cu)).containsOnly(false))
          )
        );
    }

    @Test
    void itParameterInsideMultilineBlock() {
        rewriteRun(
          ruby(
            """
              rows.each do
                it.values.each do
                  puts it
                end
              end
              """
          )
        );
    }

    /**
     * RSpec's `it "..." do ... end` is a method call that happens to be named `it`, and must not be
     * mistaken for the implicit block parameter.
     */
    @Test
    void rspecItIsAMethodInvocation() {
        rewriteRun(
          ruby(
            """
              RSpec.describe User do
                it "is valid" do
                  expect(user).to be_valid
                end
              end
              """,
            spec -> spec.afterRecipe(cu -> {
                List<String> invocations = new ArrayList<>();
                new RubyVisitor<Integer>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, Integer p) {
                        invocations.add(method.getSimpleName());
                        return super.visitMethodInvocation(method, p);
                    }
                }.visit(cu, 0);
                assertThat(invocations).contains("it");
            })
          )
        );
    }

    /**
     * @return one element per {@link Rb.Block} in the source, true when it has a parameter list.
     */
    private static List<Boolean> blocksWithParameters(Rb.CompilationUnit cu) {
        List<Boolean> parameterized = new ArrayList<>();
        new RubyVisitor<Integer>() {
            @Override
            public J visitBlock(Rb.Block block, Integer p) {
                parameterized.add(block.getParameters() != null);
                return super.visitBlock(block, p);
            }
        }.visit(cu, 0);
        return parameterized;
    }
}
