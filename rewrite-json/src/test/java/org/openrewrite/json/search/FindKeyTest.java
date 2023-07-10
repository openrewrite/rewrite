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
package org.openrewrite.json.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.json.Assertions.json;

class FindKeyTest implements RewriteTest {

    @DocumentExample
    @SuppressWarnings("JsonStandardCompliance")
    @Test
    void findKey() {
        rewriteRun(
          spec -> spec.recipe(new FindKey("$.metadata.name")),
          json("""
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
                  /*~~>*/"name": "monitoring-tools",
                  "namespace": "monitoring-tools"
                }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3401")
    @Test
    void findKeyWithMultipleBinaryExpressions() {
        rewriteRun(
          spec -> spec.recipe(new FindKey("$.foo.bar[?(@.types == 'something' && @.group == 'group' && @.category == 'match' && @.type == 'type')].pattern")),
          json("""
              {
                "foo": {
                  "bar": [
                    {
                      "type": "type",
                      "group": "group",
                      "category": "other",
                      "types": "something",
                      "pattern": "p1"
                    },
                    {
                      "type": "type",
                      "group": "group",
                      "category": "match",
                      "types": "something",
                      "pattern": "p2"
                    }
                  ]
                }
              }
              """,
            """
              {
                "foo": {
                  "bar": [
                    {
                      "type": "type",
                      "group": "group",
                      "category": "other",
                      "types": "something",
                      "pattern": "p1"
                    },
                    {
                      "type": "type",
                      "group": "group",
                      "category": "match",
                      "types": "something",
                      /*~~>*/"pattern": "p2"
                    }
                  ]
                }
              }
              """
          )
        );
    }
}
