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
package org.openrewrite.java;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RecipeMarkupDemonstrationTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"debug", "info", "warning", "error"})
    void markup(String level) {
        rewriteRun(
          spec -> spec.recipe(new RecipeMarkupDemonstration(level)),
          java(
            """
              class Test {
              }
              """,
            String.format("""
              /*~~(This is a%s %s message.)~~>*/class Test {
              }
              """, "error".equals(level) || "info".equals(level) ? "n" : "", level)
          )
        );
    }
}
