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

public class ParenthesesTest implements RewriteTest {

    @Test
    void parentheses() {
        rewriteRun(
          ruby(
            """
              (42)
              """
          )
        );
    }

    @Test
    void parenthesizedExpression() {
        rewriteRun(
          ruby(
            """
              @lockfile if defined?(@lockfile)
              @lockfile = nil
              """
          )
        );
    }

    @Test
    void ambiguousOpenParens() {
        rewriteRun(
          ruby(
            // do the first parentheses belong to the WHOLE expression or a part? it isn't immediately
            // obvious when you pass that first open parentheses
            """
              (((1 + 2)) + 3) + 4
              """
          )
        );
    }

    @Test
    void assignment() {
        rewriteRun(
          ruby(
            """
              (a = 1)
              """
          )
        );
    }
}
