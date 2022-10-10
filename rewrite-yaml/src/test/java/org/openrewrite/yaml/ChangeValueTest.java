package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
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
