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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class CopyValueTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
        spec.executionContext(ctx);
    }

    @Test
    void changeCurrentFileWhenNull() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", null, "$.destination", null)
          ),
          yaml(
            """
              source: value
              destination: original
              """,
            """
              source: value
              destination: value
              """,
            spec -> spec.path("a.yml")
          )
        );
    }

    @Test
    void changeOnlyMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", "a.yml", "$.destination", null)
          ),
          yaml(
            """
              source: value
              destination: original
              """,
            """
              source: value
              destination: value
              """,
            spec -> spec.path("a.yml")
          ),
          yaml(
            """
              source: other
              destination: original
              """,
            spec -> spec.path("b.yml")
          )
        );
    }

    @Test
    void copyComplexValue() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", null, "$.destination", null)
          ),
          yaml(
            """
              source:
                foo: bar
              destination:
                foo: baz
              """,
            """
              source:
                foo: bar
              destination:
                foo: bar
              """,
            spec -> spec.path("a.yml")
          )
        );
    }

    @Test
    void copyToOtherFile() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", "a.yml", "$.destination", "b.yml")
          ),
          yaml(
            """
              source: value
              destination: original
              """,
            spec -> spec.path("a.yml")
          ),
          yaml(
            """
              """,
            """
              destination: value
              """,
            spec -> spec.path("b.yml")
          )
        );
    }
}
