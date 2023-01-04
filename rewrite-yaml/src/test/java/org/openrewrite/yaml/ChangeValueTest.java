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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class ChangeValueTest implements RewriteTest {

    @Test
    void changeNestedKeyValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.metadata.name",
            "monitoring",
            null
          )),
          yaml(
            """
                  apiVersion: v1
                  metadata:
                    name: monitoring-tools
                    namespace: monitoring-tools
              """,
            """
                  apiVersion: v1
                  metadata:
                    name: monitoring
                    namespace: monitoring-tools
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2595")
    @Test
    void updateScalarValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.sources[?(@ == 'https://old-url.git')]",
            "https://super-cool-url.git",
            null
          )),
          yaml(
            """
                sources:
                    - https://old-url.git
                    - value2
                maintainers:
                    - name: Mara
                      email: mara@mara.com
            """,
            """
                sources:
                    - https://super-cool-url.git
                    - value2
                maintainers:
                    - name: Mara
                      email: mara@mara.com
            """
          )
        );
    }

    @SuppressWarnings("YAMLUnusedAnchor")
    @Test
    void changeAliasedKeyValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.*.yo",
            "howdy",
            null
          )),
          yaml(
            """
                  bar:
                    &abc yo: friend
                  baz:
                    *abc: friendly
              """,
            """
                  bar:
                    &abc yo: howdy
                  baz:
                    *abc: howdy
              """
          )
        );
    }

    @Test
    void changeSequenceValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.metadata.name",
            "monitoring",
            null
          )),
          yaml(
            """
                  apiVersion: v1
                  metadata:
                    name: [monitoring-tools]
                    namespace: monitoring-tools
              """,
            """
                  apiVersion: v1
                  metadata:
                    name: monitoring
                    namespace: monitoring-tools
              """
          )
        );
    }

    @Test
    void changeRelativeKey() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            ".name",
            "monitoring",
            null
          )),
          yaml(
            """
                  apiVersion: v1
                  metadata:
                    name: monitoring-tools
                    namespace: monitoring-tools
              """,
            """
                  apiVersion: v1
                  metadata:
                    name: monitoring
                    namespace: monitoring-tools
              """
          )
        );
    }

    @Test
    void changeSequenceKeyByWildcard() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.subjects[*].kind",
            "Deployment",
            null
          )),
          yaml(
            """
                  subjects:
                    - kind: User
                      name: some-user
                    - kind: ServiceAccount
                      name: monitoring-tools
              """,
            """
                  subjects:
                    - kind: Deployment
                      name: some-user
                    - kind: Deployment
                      name: monitoring-tools
              """
          )
        );
    }

    @Test
    void changeSequenceKeyByExactMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(
            "$.subjects[?(@.kind == 'ServiceAccount')].kind",
            "Deployment",
            null
          )),
          yaml(
            """
                  subjects:
                    - kind: User
                      name: some-user
                    - kind: ServiceAccount
                      name: monitoring-tools
              """,
            """
                  subjects:
                    - kind: User
                      name: some-user
                    - kind: Deployment
                      name: monitoring-tools
              """
          )
        );
    }

    @Test
    void changeOnlyMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new ChangeValue(".metadata", "monitoring", "**/a.yml")),
          yaml("metadata: monitoring-tools", "metadata: monitoring", spec -> spec.path("a.yml")),
          yaml("metadata: monitoring-tools", spec -> spec.path("b.yml"))
        );
    }
}
