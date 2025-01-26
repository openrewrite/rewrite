/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.Example;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.json.Assertions.json;

class ChangeKeyTest implements RewriteTest {
    @DocumentExample
    @Test
    void simpleChangeRootKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "$.description",
            "\"newDescription\""
          )),
          json(
                """
              {
                "id": "something",
                "description": "desc",
                "other": "whatever"
              }
              """,
            """
              {
                "id": "something",
                "newDescription": "desc",
                "other": "whatever"
              }
              """
          )
        );
    }

    @Example
    @Test
    void changeNestedKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "$.metadata.name",
            "\"name2\""
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
                  "name2": "monitoring-tools",
                  "namespace": "monitoring-tools"
                }
              }
              """
          )
        );
    }

    @Test
    void changeArrayKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "$.subjects.kind",
            "\"kind2\""
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
                    "kind2": "ServiceAccount",
                    "name": "monitoring-tools"
                  }
                ]
              }
              """
          )
        );
    }
}
