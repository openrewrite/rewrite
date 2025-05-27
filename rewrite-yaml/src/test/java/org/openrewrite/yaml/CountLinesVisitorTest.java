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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class CountLinesVisitorTest implements RewriteTest {

    @Test
    @DocumentExample
    void basic() {
        rewriteRun(
          yaml(
            """
            name: John Doe
            age: 30
            hobbies: !important
              - reading
              - hiking
              - coding
            """,
            // As a human I can see that there are 6 lines in the above YAML snippets, but we seem
            // to count the line breaks only, so the first one doesn't count. Inline with other langauges.
            spec -> spec.afterRecipe(cu ->
              assertThat(CountLinesVisitor.countLines(cu)).isEqualTo(5))
          )
        );
    }
}
