/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.ruby.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NestingDepthTest {

    @Test
    void bracketsAndBlocks() {
        assertThat(NestingDepth.of("foo(bar[1], { a: -> { 2 } })\n")).isEqualTo(3);
        assertThat(NestingDepth.of("items.each do |i|\n  i.map { |j| j }\nend\n")).isEqualTo(2);
    }

    @Test
    void keywordBlocks() {
        assertThat(NestingDepth.of("""
                module A
                  class B
                    def c
                      if d
                        while e
                          case f
                          when 1 then begin; g; end
                          end
                        end
                      end
                    end
                  end
                end
                """)).isEqualTo(7);
    }

    @Test
    void modifiersOpenNothing() {
        assertThat(NestingDepth.of("""
                def a
                  return if b
                  c unless d
                  e += 1 while f
                  g rescue nil
                  begin; h; end until i
                end
                """)).isEqualTo(2);
    }

    @Test
    void literalsAndCommentsHideBrackets() {
        assertThat(NestingDepth.of("""
                def m
                a = "((((" + '[[[[' + %w[{ { {] .join # ((((
                b = /\\(\\(\\(/ =~ :[] ? ?( : :end
                c = <<~SQL
                  select ((((((
                SQL
                =begin
                ((((((((
                =end
                d = x.end + y.class
                end
                """)).isEqualTo(1);
    }

    @Test
    void interpolationNests() {
        assertThat(NestingDepth.of("x = \"#{\"#{\"#{1}\"}\"}\"\n")).isEqualTo(3);
        assertThat(NestingDepth.of("x = %x{ function f() { return #{y}; } }\nz = 1\n")).isEqualTo(1);
    }

    @Test
    void endlessMethodsCloseThemselves() {
        assertThat(NestingDepth.of("""
                class A
                  def b = 1
                  def c(d) = d
                  def e=(f)
                    @e = f
                  end
                end
                """)).isEqualTo(2);
    }

    @Test
    void deepNesting() {
        assertThat(NestingDepth.of("module M\n".repeat(200) + "end\n".repeat(200))).isEqualTo(200);
        assertThat(NestingDepth.of("x = " + "-> a { ".repeat(60) + "a" + " }".repeat(60) + "\n")).isEqualTo(60);
    }
}
