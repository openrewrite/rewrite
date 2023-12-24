/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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

public class StringTest implements RewriteTest {

    @Test
    void string() {
        rewriteRun(
          ruby(
            """
              "The programming language is"
              """
          )
        );
    }

    @Test
    void singleQuoteDelimiter() {
        rewriteRun(
          ruby(
            """
              'The programming language is'
              """
          )
        );
    }

    @Test
    void delimitedString() {
        rewriteRun(
          ruby(
            """
              "#{a1} The programming language is #{a1}"
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Q", "q", "x"})
    void percentDelimitedString(String delim) {
        rewriteRun(
          ruby(
            """
              %%%s!#{a1} The programming language is #{a1}!
              """.formatted(delim)
          )
        );
    }

    @Test
    void regexRDString() {
        rewriteRun(
          ruby(
            """
              %r|^/usr/local/.*|
              """
          )
        );
    }

    @Test
    void regexSlashDelimited() {
        rewriteRun(
          ruby(
            """
              /my name is #{myname}/o
              """
          )
        );
    }
}
