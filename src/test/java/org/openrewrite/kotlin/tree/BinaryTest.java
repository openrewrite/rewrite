/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class BinaryTest implements RewriteTest {

    @Test
    void equals() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                val n = 0
                val b = n == 0
              }
            """,
            isFullyParsed()
          )
        );
    }

    @Disabled("Requires function call and Convert PSI.")
    @Test
    void minusEquals() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                var n = 0
                n -= 5
              }
            """,
            isFullyParsed()
          )
        );
    }
}
