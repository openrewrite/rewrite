/*
 * Copyright 2021 the original author or authors.
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

class ChangeValueTest implements RewriteTest {
    @DocumentExample
    @Test
    void changeNestedValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.metadata.name",
            "\"monitoring\""
          )),
          json(
            """
              {
                "apiVersion": "v1",
                "metadata": {
                  "name": "monitoring-tools",
                  "namespace": "monitoring-tools"
                }
              }
              """,
            """
              {
                "apiVersion": "v1",
                "metadata": {
                  "name": "monitoring",
                  "namespace": "monitoring-tools"
                }
              }
              """
          )
        );
    }

    @Test
    void changeArrayValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.subjects.kind",
            "\"Deployment\""
          )),
          json(
            """
              {
                "subjects": [
                  {
                    "kind": "ServiceAccount",
                    "name": "monitoring-tools"
                  }
                ]
              }
              """,
            """
              {
                "subjects": [
                  {
                    "kind": "Deployment",
                    "name": "monitoring-tools"
                  }
                ]
              }
              """
          )
        );
    }

    @Test
    void changeToNumberAsString() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "\"123\"")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": "123" }
              """
          )
        );
    }

    @Test
    void changeToNumberWithoutQuotes() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "123")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": 123 }
              """
          )
        );
    }

    @Test
    void changeToBooleanAsString() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "\"true\"")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": "true" }
              """
          )
        );
    }

    @Test
    void changeToBooleanWithoutQuotes() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "true")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": true }
              """
          )
        );
    }

    @Test
    void changeToStringWithDoubleQuotes() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "\"v2\"")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": "v2" }
              """
          )
        );
    }

    @Test
    void changeToStringWithDoubleQuotesAroundSingleQuotes() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "\"'v2'\"")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": "'v2'" }
              """
          )
        );
    }

    @Test
    void changeToStringSingleQuotes() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "'v2'")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": "'v2'" }
              """
          )
        );
    }

    @Test
    void changeToStringSingleQuotesAroundDoubleQuotes() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "'\"v2\"'")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": "'\\"v2\\"'" }
              """
          )
        );
    }

    @Test
    void changeToStringWithoutQuotes() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "v2")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": "v2" }
              """
          )
        );
    }

    @Test
    void changeToObjectAsString() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "\"{\\\"a\\\":\\\"b\\\"}\"")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": "{\\"a\\":\\"b\\"}" }
              """
          )
        );
    }

    @Test
    void changeToObject() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "{\"a\":\"b\"}")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": {"a":"b"} }
              """
          )
        );
    }

    @Test
    void changeStringToNumberOfSameValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "123")),
          json(
            """
              { "apiVersion": "123" }
              """,
            """
              { "apiVersion": 123 }
              """
          )
        );
    }

    @Test
    void changeToNullAsString() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "\"null\"")),
          json(
            """
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": "null" }
              """
          )
        );
    }

    @Test
    void changeToLiteralNull() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "null")),
          json(
            """                                                                                                                                                                                                                                                                    
              { "apiVersion": "v1" }
              """,
            """
              { "apiVersion": null }
              """
          )
        );
    }

    @Test
    void intentionalSingleQuotedNullAsString() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "'null'")),
          json(
            """
            { "apiVersion": "v1" }
            """,
            """
            { "apiVersion": "'null'" }
            """
          )
        );
    }

    @Test
    void intentionalSingleQuotedNumberAsString() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "'123'")),
          json(
            """
            { "apiVersion": "v1" }
            """,
            """
            { "apiVersion": "'123'" }
            """
          )
        );
    }

    @Test
    void  intentionalSingleQuotedBooleanAsString() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue("$.apiVersion", "'true'")),
          json(
            """
            { "apiVersion": "v1" }
            """,
            """
            { "apiVersion": "'true'" }
            """
          )
        );
    }
}
