package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class DeleteKeyTest implements RewriteTest {

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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1175")
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
    void changeOnlyMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new DeleteKey(".apiVersion", "**/a.yml")),
          yaml("apiVersion: v1", "", spec -> spec.path("a.yml")),
          yaml("apiVersion: v1", spec -> spec.path("b.yml"))
        );
    }
}
