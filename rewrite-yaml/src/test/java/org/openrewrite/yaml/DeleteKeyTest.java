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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class DeleteKeyTest implements RewriteTest {

    @DocumentExample
    @Test
    void deleteNestedKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.metadata.name", null)),
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
                    namespace: monitoring-tools
              """
          )
        );
    }

    @Test
    void deleteRelativeKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(".name", null)),
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
                    namespace: monitoring-tools
              """
          )
        );
    }

    @Test
    void deleteSequenceEntry() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.subjects[?(@.kind == 'User')]", null)),
          yaml(
            """
                  subjects:
                    - kind: User
                      name: some-user
                      restore-keys: |
                        gradle
                    - kind: ServiceAccount
                      name: monitoring-tools
              """,
            """
                  subjects:
                    - kind: ServiceAccount
                      name: monitoring-tools
              """
          )
        );
    }

    @Test
    void deleteScalarSequenceEntry() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.widget.list[?(@ == 'item 2')]", null)),
          yaml(
            """
                  widget:
                    on: yes
                    list:
                      - item 1
                      - item 2
                      - item 3
              """,
            """
                  widget:
                    on: yes
                    list:
                      - item 1
                      - item 3
              """
          )
        );
    }

    @Test
    void deleteSequenceKeyByWildcard() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.subjects[*].kind", null)),
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
                    - name: some-user
                    - name: monitoring-tools
              """
          )
        );
    }

    @Test
    void deleteSubSequenceKeyByExactMatch() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.subjects[?(@.kind == 'ServiceAccount')].kind", null)),
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
                    - name: monitoring-tools
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1175")
    @Test
    void deleteNestedKeyRemovingUnusedKeysRecursively() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.b.c.d", null)),
          yaml(
            """
                  a: a-value
                  b:
                    c:
                      d: d-value
              """,
            """
              a: a-value
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1175")
    @Test
    void deleteKeyKeepingUnrelatedUnusedKeys() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.jobs.build.strategy.fail-fast", null)),
          yaml(
            """
                  on:
                    push:
                      branches:
                        - main
                      tags-ignore:
                        - "*"
                    pull_request:
                      branches:
                        - main
                    workflow_dispatch: {}
                    schedule:
                      - cron: 0 18 * * *
                  jobs:
                    build:
                      strategy:
                        fail-fast: false
                        matrix:
                          java: ["11"]
                          os: ["ubuntu-latest"]
              """,
            """
                  on:
                    push:
                      branches:
                        - main
                      tags-ignore:
                        - "*"
                    pull_request:
                      branches:
                        - main
                    workflow_dispatch: {}
                    schedule:
                      - cron: 0 18 * * *
                  jobs:
                    build:
                      strategy:
                        matrix:
                          java: ["11"]
                          os: ["ubuntu-latest"]
              """
          )
        );
    }

    @Test
    void deleteSequenceEntryUsingChildRegexMatch() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.b.c[?(@.name =~ 'b1-.*')]", null)),
          yaml(
            """
                  a: a-value
                  b:
                    c:
                      - name: b1-value
                      - name: b2-value
              """,
            """
                  a: a-value
                  b:
                    c:
                      - name: b2-value
              """
          )
        );
    }

    @Test
    void deleteSequenceEntriesUsingChildRegexMatchRemovingUnusedKeysRecursively() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.b.c[?(@.name =~ '.*-value')]", null)),
          yaml(
            """
            a: a-value
            b:
              c:
                - name: b1-value
                - name: b2-value
            """,
            """
            a: a-value
            """
          )
        );
    }

    @Test
    void keepsTheCommentsOfTheLinesAround() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.b", null)),
          yaml(
            """
              a: 1 # one
              b: 2 # two
              c: 3 # three
              """,
            """
              a: 1 # one
              c: 3 # three
              """
          )
        );
    }

    @Test
    void deletesTheCommentOfTheLastEntry() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.c", null)),
          yaml(
            """
              a: 1 # one
              b: 2 # two
              c: 3 # three
              """,
            """
              a: 1 # one
              b: 2 # two
              """
          )
        );
    }

    @Test
    void deletesTheFirstEntryWithItsComment() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a", null)),
          yaml(
            """
              a: 1 # one
              b: 2 # two
              """,
            """
              b: 2 # two
              """
          )
        );
    }

    @Test
    void keepsTheCommentOfTheKeyLine() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.x", null)),
          yaml(
            """
              a: # about a
                x: 1
                y: 2
              b: 3
              """,
            """
              a: # about a
                y: 2
              b: 3
              """
          )
        );
    }

    @Test
    void deletesTheCommentOfANestedLastEntry() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.jobs.test.strategy.matrix.os", null)),
          yaml(
            """
              jobs:
                test:
                  strategy:
                    matrix:
                      node-version: [16, 18]
                      os:
                        - ubuntu-latest # LTS runner
                lint:
                  runs-on: ubuntu-latest
              """,
            """
              jobs:
                test:
                  strategy:
                    matrix:
                      node-version: [16, 18]
                lint:
                  runs-on: ubuntu-latest
              """
          )
        );
    }

    @Test
    void deletesTheLastSequenceItemWithItsComment() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a[?(@ == 'y')]", null)),
          yaml(
            """
              a:
                - x # one
                - y # two
              b: 1
              """,
            """
              a:
                - x # one
              b: 1
              """
          )
        );
    }

    @Test
    void deletesTheLastEntryAfterABlockScalar() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.y", null)),
          yaml(
            """
              a:
                x: |
                  t
                y: 2 # y
              b: 1
              """,
            """
              a:
                x: |
                  t
              b: 1
              """
          )
        );
    }

    @Test
    void deletesABlockScalarThatEndsTheMapping() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.x", null)),
          yaml(
            """
              a:
                w: 1 # w
                x: |
                  t
              b: 1
              """,
            """
              a:
                w: 1 # w
              b: 1
              """
          )
        );
    }

    @Test
    void deletingTheOnlyNestedKeyKeepsTheCommentsAround() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.x", null)),
          yaml(
            """
              p: 1 # p
              a: # about a
                x: 1 # x
              b: 3
              """,
            """
              p: 1 # p
              b: 3
              """
          )
        );
    }

    @Test
    void keepsTheCommentBeforeAnEmptiedNestedMapping() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$..q", null)),
          yaml(
            """
              a:
                x:
                  p: 1 # p
                  q: 2 # q
                y:
                  q: 3 # q2
              b: 1
              """,
            """
              a:
                x:
                  p: 1 # p
              b: 1
              """
          )
        );
    }

    @Test
    void deletesABlockScalarBetweenEntries() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a", null)),
          yaml(
            """
              x: 1
              a: |
                t
              b: 2
              """,
            """
              x: 1
              b: 2
              """
          )
        );
    }

    @Test
    void deletesTheEntryAfterABlockScalar() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.b", null)),
          yaml(
            """
              a: |
                t
              b: 1
              c: 2
              """,
            """
              a: |
                t
              c: 2
              """
          )
        );
    }

    @Test
    void deletesTheCommentLinesAboveADeletedFirstKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.x", null)),
          yaml(
            """
              a:
                # about x
                x: 1
                y: 2
              """,
            """
              a:
                y: 2
              """
          )
        );
    }

    @Test
    void deletesTheCommentLinesAboveADeletedMiddleKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.x", null)),
          yaml(
            """
              a:
                w: 0
                # about x
                x: 1
                y: 2
              """,
            """
              a:
                w: 0
                y: 2
              """
          )
        );
    }

    @Test
    void keepsTheFileHeaderWhenDeletingTheFirstKey() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.x", null)),
          yaml(
            """
              # header
              x: 1
              y: 2
              """,
            """
              # header
              y: 2
              """
          )
        );
    }

    @Test
    void emptiesANestedMappingThatEndsWithABlockScalar() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.y.q", null)),
          yaml(
            """
              a:
                x: 1 # x
                y:
                  q: |
                    t
              b: 1
              """,
            """
              a:
                x: 1 # x
              b: 1
              """
          )
        );
    }

    @Test
    void deletesTheLastItemOfAFlowSequence() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a[?(@ == 'z')]", null)),
          yaml(
            """
              a: [x, y, z]
              """,
            """
              a: [x, y]
              """
          )
        );
    }

    @Test
    void deletesTheLastItemOfAFlowSequenceWithSpaces() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a[?(@ == 'z')]", null)),
          yaml(
            """
              a: [ x , y , z ]
              """,
            """
              a: [ x , y ]
              """
          )
        );
    }

    @Test
    void deletesTheLastItemOfAFlowSequenceOverSeveralLines() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a[?(@ == 'y')]", null)),
          yaml(
            """
              a: [
                x,
                y
              ]
              """,
            """
              a: [
                x
              ]
              """
          )
        );
    }

    @Test
    void keepsATrailingCommaOfAFlowSequence() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a[?(@ == 'y')]", null)),
          yaml(
            """
              a: [x, y, ]
              """,
            """
              a: [x, ]
              """
          )
        );
    }

    @Test
    void deletesTheFirstItemOfAFlowSequence() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a[?(@ == 'x')]", null)),
          yaml(
            """
              a: [x, y, z]
              """,
            """
              a: [y, z]
              """
          )
        );
    }

    @Test
    void deletesTheFirstEntryOfAFlowMapping() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey("$.a.x", null)),
          yaml(
            """
              a: { x: 1, y: 2 }
              """,
            """
              a: { y: 2 }
              """
          )
        );
    }
}
