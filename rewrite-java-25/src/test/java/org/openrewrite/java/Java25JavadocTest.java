/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class Java25JavadocTest implements RewriteTest {

    // Java 25's Javadoc parser emits "// comment" inside <pre><code> blocks as DCComment nodes
    // whose body lacks the leading space after the margin asterisk. The visitText whitespace
    // compensation then re-inserts that space in the wrong position (after "//" instead of before it),
    // breaking print idempotency: "* // comment" is reprinted as "*//  comment".
    @Issue("https://github.com/openrewrite/rewrite/issues/8002")
    @Test
    void lineCommentInPreCodeBlockIsIdempotent() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * Example:
                   * <pre><code class='java'>
                   *
                   * // assertions will pass
                   * assertThat(1).isEqualTo(1);
                   *
                   * </code></pre>
                   */
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void lineCommentDirectlyAfterAsteriskSpaceIsIdempotent() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * <pre><code>
                   * // line comment
                   * int x = 1;
                   * </code></pre>
                   */
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void lineCommentInPreBlockWithMultipleLines() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * Description.
                   * <pre><code class='java'>
                   *
                   * // first comment
                   * doSomething();
                   * // second comment
                   * doSomethingElse();
                   *
                   * </code></pre>
                   */
                  void method() {}
              }
              """
          )
        );
    }
}
