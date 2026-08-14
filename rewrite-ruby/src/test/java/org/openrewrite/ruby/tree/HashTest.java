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

/**
 * See <a href="https://stackoverflow.com/a/8198831">this explanation</a> for why
 * there are two different key-value delimiters for hashes in Ruby.
 */
public class HashTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"=>", ":"})
    void hash(String delimiter) {
        rewriteRun(
          ruby(
            """
              {a%s 1}
              """.formatted(delimiter)
          )
        );
    }

    @Test
    void colonHashWithSymbolValue() {
        rewriteRun(
          ruby(
            """
              {a: :b}
              """
          )
        );
    }

    @Test
    void hashIter() {
        rewriteRun(
          ruby(
            """
              hash = {:a=>1, :b=>2, :c=>3}
              hash.each do |key,value|
                  puts "#{key} => #{value}"
              end
              """
          )
        );
    }

    @Test
    void empty() {
        rewriteRun(
          ruby(
            """
              {}
              """
          )
        );
    }

    @Test
    void keywordArg() {
        rewriteRun(
          ruby(
            """
              expect(**kw)
              """
          )
        );
    }

    @Test
    void interpolatedStringLabel() {
        rewriteRun(
          ruby(
            """
              h = { "#{prefix}_URL": 1 }
              """
          )
        );
    }

    @Test
    void interpolatedStringLabelInArgument() {
        rewriteRun(
          ruby(
            """
              f(a, "#{x}": 1)
              """
          )
        );
    }

    @Test
    void interpolatedStringRocketKey() {
        rewriteRun(
          ruby(
            """
              h = { "#{prefix}_URL" => 1, :"#{prefix}" => 2 }
              """
          )
        );
    }

    @Test
    void stringLabel() {
        rewriteRun(
          ruby(
            """
              h = { "literal": 1, 'single': 2 }
              """
          )
        );
    }
}
