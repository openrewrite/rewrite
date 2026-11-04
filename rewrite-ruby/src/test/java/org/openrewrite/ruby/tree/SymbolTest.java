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

public class SymbolTest implements RewriteTest {

    @Test
    void literal() {
        rewriteRun(
          ruby(
            """
              :sym
              """
          )
        );
    }

    @Test
    void interpolated() {
        rewriteRun(
          ruby(
            """
              :"#{next}"
              """
          )
        );
    }

    @Test
    void delimited() {
        rewriteRun(
          ruby(
            """
              :"sym"
              """
          )
        );
    }

    @Test
    void delimitedHashKeyWhichIsImplicitlyASymbol() {
        rewriteRun(
          ruby(
            """
              { "modules.v1": "/v1/modules/" }
              """
          )
        );
    }

    @Test
    void arbitraryDelimiter() {
        rewriteRun(
          ruby(
            """
              %s[sym ]
              """
          )
        );
    }

    /**
     * See the meaning of <a href="https://docs.ruby-lang.org/en/3.2/syntax/literals_rdoc.html#label-25i+and+-25I-3A+Symbol-Array+Literals">
     * non-interpolable symbol arrays</a>
     */
    @Test
    void nonInterpolableSymbolArrays() {
        rewriteRun(
          ruby(
            // equivalent to [:foo, :bar, :baz]
            """
              %i[ foo bar baz ]
              prints "hi"
              """
          )
        );
    }

    /**
     * See the meaning of <a href="https://docs.ruby-lang.org/en/3.2/syntax/literals_rdoc.html#label-25i+and+-25I-3A+Symbol-Array+Literals">
     * interpolable symbol arrays</a>
     */
    @Test
    void interpolableSymbolArrays() {
        rewriteRun(
          ruby(
            // equivalent to [:"2"]
            """
              %I(#{1 + 1})
              """
          )
        );
    }
}
