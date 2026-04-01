/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.python.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

class CollectionTest implements RewriteTest {

    @Test
    void listExpression() {
        rewriteRun(
          python("xs = [1, 2, 3]")
        );
    }

    @Test
    void listExpressionEmpty() {
        rewriteRun(
          python("xs = []")
        );
    }

    @Test
    void tupleExpression() {
        rewriteRun(
          python("xs = (1, 2, 3)")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "()", "( )"
    })
    void tupleExpressionEmpty(String arg) {
        rewriteRun(
          python("xs = %s".formatted(arg))
        );
    }

    @Test
    void tupleExpressionSingle() {
        // ()     => tuple
        // (1)    => int
        // (1,)   => tuple      <-- test this case
        // (1, 2) => tuple
        rewriteRun(
          python("xs = (1,)")
        );
    }

    @Test
    void dictExpression() {
        rewriteRun(
          python("xs = {\"foo\": \"bar\", \"foo2\": \"bar2\"}")
        );
    }

    @Test
    void dictExpressionEmpty() {
        rewriteRun(
          python("xs = {}")
        );
    }
}
