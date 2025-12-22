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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.json.Assertions.json;

class CopyValueTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeCurrentFileWhenNull() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", null, ".", "copiedValue", null)
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
                "copiedValue": "value"
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
            new CopyValue("$.source", "a.json", ".", "copiedValue", null)
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
                "copiedValue": "value"
              }
              """,
            spec -> spec.path("a.json")
          )
        );
    }

    @Test
    void copyComplexValue() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", null, ".", "copiedValue", null)
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
                "copiedValue": {
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
            new CopyValue("$.source", "a.json", ".", "copiedValue", "b.json")
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
                "source": "original"
              }
              """,
            """
              {
                "source": "original",
                "copiedValue": "value"
              }
              """,
            spec -> spec.path("b.json")
          )
        );
    }

    @Test
    void copyToOtherFileWithEmptyObject() {
        rewriteRun(
          spec -> spec.recipe(
            new CopyValue("$.source", "a.json", ".", "copiedValue", "b.json")
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
                "copiedValue": "value"
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
            new CopyValue("$.sourceArray", null, ".", "copiedValue", null)
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
                "copiedValue": [1, 2, 3]
              }
              """
          )
        );
    }
}
