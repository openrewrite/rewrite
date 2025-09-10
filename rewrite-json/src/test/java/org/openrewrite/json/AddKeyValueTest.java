/*
 * Copyright 2024 the original author or authors.
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

class AddKeyValueTest implements RewriteTest {

    @DocumentExample
    @Test
    void shouldAppendSimpleValue() {
        rewriteRun(
          spec -> spec.recipe(new AddKeyValue("$.", "key", "\"val\"", false)),
          //language=json
          json(
            """
              {
                  "x": "x",
                  "l": [1, 2]
              }
              """,
            """
              {
                  "x": "x",
                  "l": [1, 2],
                  "key": "val"
              }
              """
          )
        );
    }

    @Test
    void shouldAppendToNestedObject() {
        rewriteRun(
          spec -> spec.recipe(new AddKeyValue("$.x.y.*", "key", "\"val\"", false)),
          //language=json
          json(
            """
              {
                  "x": {
                      "y": {}
                  }
              }
              """,
            """
              {
                  "x": {
                      "y": {
                          "key": "val"
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldAppendToNestedObjectInArray() {
        rewriteRun(
          spec -> spec.recipe(new AddKeyValue("$.x[1].y.*", "key", "\"val\"", false)),
          //language=json
          json(
            """
              {
                  "x": [
                      {},
                      {
                          "y" : {}
                      }
                  ]
              }
              """,
            """
              {
                  "x": [
                      {},
                      {
                          "y" : {
                              "key": "val"
                          }
                      }
                  ]
              }
              """
          )
        );
    }

    @Test
    void shouldNotAppendIfExists() {
        rewriteRun(
          spec -> spec.recipe(new AddKeyValue("$.", "key", "\"val\"", false)),
          //language=json
          json(
            """
              {
                  "key": "x"
              }
              """
          )
        );
    }

    @Test
    void shouldAppendObject() {
        rewriteRun(
          spec -> spec.recipe(new AddKeyValue(
            "$.", "key", """
                { "a": "b" }
            """.trim(), false)),
          //language=json
          json(
            """
              {
                  "x": "x",
                  "l": [1, 2]
              }
              """,
            """
              {
                  "x": "x",
                  "l": [1, 2],
                  "key": {
                      "a": "b"
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldPrependObject() {
        rewriteRun(
          spec -> spec.recipe(new AddKeyValue(
            "$.", "key", """
                { "a": "b" }
            """.trim(), true)),
          //language=json
          json(
            """
              {
                  "x": "x",
                  "l": [1, 2]
              }
              """,
            """
              {
                  "key": {
                      "a": "b"
                  },
                  "x": "x",
                  "l": [1, 2]
              }
              """
          )
        );
    }
}
