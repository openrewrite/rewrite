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

class Java26ImplicitLambdaParameterTest implements RewriteTest {

    /**
     * On JDK 26+ the synthesized {@code vartype} for an implicit lambda parameter
     * is reported as a zero-width {@code JCErroneous} sitting at the parameter
     * name (instead of having {@code NOPOS} as on JDK ≤ 25). The parser used to
     * detect the synthesized vartype only via {@code endPos(vartype) < 0}; on
     * JDK 26 that check no longer fires and {@code convert(vartype)} produced a
     * {@code J.Erroneous} that failed the cast to {@code TypeTree}.
     */
    @Issue("https://github.com/openrewrite/rewrite/issues/7554")
    @Test
    void implicitLambdaParameterInMethodChain() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.stream.Collectors;
              import java.util.stream.Stream;

              class Test {
                  List<String> matches(String request) {
                      return Stream.of("a", "b")
                                   .filter(field -> field.toLowerCase().contains(request.toLowerCase()))
                                   .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }
}
