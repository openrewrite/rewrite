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
import org.openrewrite.json.tree.Json;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void newKeyWithEscapes() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey(
            "$.moduleNameMapper.css",
            "\"\\\\.(css|less)$\""
          )),
          json(
            """
              {
                "moduleNameMapper": {
                  "css": "identity-obj-proxy"
                }
              }
              """,
            """
              {
                "moduleNameMapper": {
                  "\\\\.(css|less)$": "identity-obj-proxy"
                }
              }
              """
          )
        );
    }

    @Test
    void newKeyWithUnpairedSurrogateKeepsItsEscape() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$.kept", "\"high\\ud800\"")),
          json(
            """
              {
                "kept": "value"
              }
              """,
            """
              {
                "high\\ud800": "value"
              }
              """,
            spec -> spec.afterRecipe(doc -> {
                Json.Member member = (Json.Member) ((Json.JsonObject) doc.getValue()).getMembers().get(0);
                assertThat(((Json.Literal) member.getKey()).getValue()).isEqualTo("high\\ud800");
            })
          )
        );
    }

    @Test
    void keyWithUnpairedSurrogateIsFoundByItsEscape() {
        rewriteRun(
          spec -> spec.recipe(new ChangeKey("$['high\\\\ud800']", "\"renamed\"")),
          json(
            """
              {
                "high\\ud800": "value"
              }
              """,
            """
              {
                "renamed": "value"
              }
              """
          )
        );
    }
}
