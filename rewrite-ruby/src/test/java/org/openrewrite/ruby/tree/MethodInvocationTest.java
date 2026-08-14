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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

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
}
