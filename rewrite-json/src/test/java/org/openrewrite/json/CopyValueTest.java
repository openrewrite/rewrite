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
package org.openrewrite.json;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.json.Assertions.json;

class CopyValueTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
        spec.executionContext(ctx);
    }

    @DocumentExample
    @Test
    void changeCurrentFileWhenNull() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", null, "destination", null)
          ),
          json(
            """
              {
                "source": "value"
              }
              """,
            """
              {
                "source": "value",
                "destination": "value"
              }
              """,
            spec -> spec.path("a.json")
          )
        );
    }

    @Test
    void changeOnlyMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", "a.json", "destination", null)
          ),
          json(
            """
              {
                "source": "value"
              }
              """,
            """
              {
                "source": "value",
                "destination": "value"
              }
              """,
            spec -> spec.path("a.json")
          ),
          json(
            """
              {
                "source": "other",
                "destination": "original"
              }
              """,
            spec -> spec.path("b.json")
          )
        );
    }

    @Test
    void copyComplexValue() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", null, "destination", null)
          ),
          json(
            """
              {
                "source": {
                  "foo": "bar"
                }
              }
              """,
            """
              {
                "source": {
                  "foo": "bar"
                },
                "destination": {
                  "foo": "bar"
                }
              }
              """
          )
        );
    }

    @Test
    void copyToOtherFile() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", "a.json", "destination", "b.json")
          ),
          json(
            """
              {
                "source": "value",
                "destination": "original"
              }
              """,
            spec -> spec.path("a.json")
          ),
          json(
            """
              {
              }
              """,
            """
              {
                "destination": "value"
              }
              """,
            spec -> spec.path("b.json")
          )
        );
    }

    @Test
    void copyEntireArray() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.sourceArray", null, "destinationArray", null)
          ),
          json(
            """
              {
                "sourceArray": [1, 2, 3]
              }
              """,
            """
              {
                "sourceArray": [1, 2, 3],
                "destinationArray": [1, 2, 3]
              }
              """
          )
        );
    }
}
