/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.InlineMethodCalls;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class InlineMethodCallsTest implements RewriteTest {

    @Test
    void leavesKotlinUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new InlineMethodCalls("Lib cmp(long, long)", "java.lang.Long.compare(a, b)", null, null, null)),
          kotlin(
            """
              object Lib {
                  @JvmStatic fun cmp(a: Long, b: Long): Int = 0
              }
              fun usage(x: Long, y: Long): Int {
                  return Lib.cmp(x, y)
              }
              """
          )
        );
    }
}
