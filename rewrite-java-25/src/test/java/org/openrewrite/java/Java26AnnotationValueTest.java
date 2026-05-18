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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class Java26AnnotationValueTest implements RewriteTest {

    /**
     * On JDK 26+ a single-element annotation shorthand like {@code @SuppressWarnings("foo")}
     * is reported by javac as a {@code JCAssign} whose lhs is a synthesized
     * {@code value=} identifier, even though that identifier does not appear in
     * source. On JDK ≤ 25 that synthesized assignment had {@code endPos < 0}, so
     * the parser could detect it and print only the rhs. On JDK 26 the assignment
     * carries the same source positions as its rhs, so the old check no longer
     * fires and the printer renders {@code value} into the literal, breaking
     * print-idempotence (e.g. {@code @SuppressWarnings(valueecked")}).
     */
    @Issue("https://github.com/openrewrite/rewrite/issues/7554")
    @Test
    void singleElementAnnotationShorthand() {
        rewriteRun(
          java(
            """
              @SuppressWarnings("unchecked")
              class Test {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7554")
    @Test
    void singleElementAnnotationOnMethod() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  @SuppressWarnings("unchecked")
                  List<String> raw() {
                      return (List<String>) (List) new ArrayList<>();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7554")
    @Test
    void singleElementAnnotationWithExplicitValueKeyword() {
        rewriteRun(
          java(
            """
              @SuppressWarnings(value = "unchecked")
              class Test {
              }
              """
          )
        );
    }
}
