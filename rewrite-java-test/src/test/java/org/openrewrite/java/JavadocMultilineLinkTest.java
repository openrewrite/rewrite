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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JavadocMultilineLinkTest implements RewriteTest {

    @Test
    void multilineLinkTagIdempotency() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * This is a test with a multiline link tag
                   * {@link java.lang.String#
                   * substring(int, int) substring method}
                   */
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void multilineLinkplainTagIdempotency() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * This is a test with a multiline linkplain tag
                   * {@linkplain java.util.List#
                   * add(Object) add method}
                   */
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void multipleLinesInLinkTag() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * This is a test with a multiline link tag
                   * {@link
                   * java.lang.String#
                   * substring(int,
                   * int) substring
                   * method}
                   */
                  void method() {}
              }
              """
          )
        );
    }

    @Test
    void linkTagWithComplexReference() {
        rewriteRun(
          java(
            """
              class Test {
                  /**
                   * Reference to {@link java.util.Map.Entry#
                   * getValue() getValue method} in nested class
                   */
                  void method() {}
              }
              """
          )
        );
    }
}
