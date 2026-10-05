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

public class SplatTest implements RewriteTest {

    @Test
    void splat() {
        rewriteRun(
          ruby(
            """
              a = [1, 2, 3]
              b = *a
              c = 1, *a
              d = 1, 2, *a
              """
          )
        );
    }

    @Test
    void doubleSplatInHashLiteral() {
        rewriteRun(
          ruby(
            """
              h = {**a, b: 1}
              """
          )
        );
    }

    @Test
    void doubleSplatArgument() {
        rewriteRun(
          ruby(
            """
              f(**opts)
              g(a, **SharedHelpers.excon_defaults(headers: headers))
              """
          )
        );
    }

    @Test
    void anonymousDoubleSplatForwarding() {
        rewriteRun(
          ruby(
            """
              def fwd(name, **)
                g(name, **)
              end
              """
          )
        );
    }
}
