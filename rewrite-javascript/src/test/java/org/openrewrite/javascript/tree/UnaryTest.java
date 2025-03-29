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
package org.openrewrite.javascript.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.javaScript;

@SuppressWarnings("JSUnusedLocalSymbols")
class UnaryTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {
      "++ n",
      "-- n",
      "n ++",
      "n --",
    })
    void incrementAndDecrement(String arg) {
        rewriteRun(
          javaScript(
            """
              var n = 1
              %s
              """.formatted(arg)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "! n",
      "- n",
      "+ n",
    })
    void unaryMinusAndPlus(String arg) {
        rewriteRun(
          javaScript(
            """
              const n1 = 1
              const n2 = %s
              """.formatted(arg)
          )
        );
    }

    @Test
    void keyofKeyword() {
        rewriteRun(
          javaScript(
            """
              type Person = { name: string , age: number };
              type KeysOfPerson = keyof Person;
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-javascript/issues/82")
    @Test
    void spreadOperator() {
        rewriteRun(
          javaScript(
            """
              function spread(arr) {
                return [...arr]
              }
              """
          )
        );
    }
}
