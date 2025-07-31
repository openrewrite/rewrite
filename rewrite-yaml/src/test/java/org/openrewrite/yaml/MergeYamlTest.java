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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.yaml.Assertions.yaml;
import static org.openrewrite.yaml.MergeYaml.InsertMode.After;
import static org.openrewrite.yaml.MergeYaml.InsertMode.Before;

@SuppressWarnings({"KubernetesUnknownResourcesInspection", "KubernetesNonEditableResources"})
class MergeYamlTest implements RewriteTest {

    @DocumentExample
    @Test
    void nonExistentBlock() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec",
            //language=yaml
            """
              lifecycleRule:
                  - action:
                        type: Delete
                    condition:
                        age: 7
              """,
            false,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: true
              """,
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: true
                  lifecycleRule:
                      - action:
                            type: Delete
                        condition:
                            age: 7
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/support-public/issues/5")
    @Test
    void scalarList() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.on",
            //language=yaml
            """
              schedule:
                  - cron: "0 18 * * *"
              """,
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              name: GitHub Actions workflow
              on: [workflow_dispatch]
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1469")
    @Test
    void emptyDocument() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              spring:
                application:
                  name: update
                  description: a description
              """,
            false,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            "",
            """
              spring:
                application:
                  name: update
                  description: a description
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/905")
    @Test
    void existingMultipleEntryBlock() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              spring:
                application:
                  name: update
                  description: a description
              """,
            false,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              spring:
                application:
                  name: main
              """,
            """
              spring:
                application:
                  name: update
                  description: a description
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1598")
    @Test
    void mergeList() {
        rewriteRun(spec ->
            spec.recipe(new MergeYaml(
              "$",
              //language=yaml
              """
                widget:
                  list:
                    - item 2
                """,
              false,
              null,
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              widget:
                list:
                  - item 1
              """,
            """
              widget:
                list:
                  - item 1
                  - item 2
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1598")
    @Test
    void mergeListAcceptTheirs() {
        rewriteRun(
          spec -> spec.recipe(new
              MergeYaml(
              "$",
              //language=yaml
              """
                widget:
                  list:
                    - item 2
                """,
              true,
              null,
              null,
              null,
              null,
            null
            )
          ),
          yaml(
            """
              widget:
                list:
                  - item 1
              """
          )
        );
    }

    @Test
    void scalar() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec",
            //language=yaml
            """
              bucketPolicyOnly: true
              """,
            false,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: false
              """,
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: true
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/418")
    @Test
    void insertYaml() {
        rewriteRun(
          spec -> spec
            .cycles(2)
            .recipe(new MergeYaml(
              "$.spec",
              //language=yaml
              """
                lifecycleRule:
                    - action:
                          type: Delete
                      condition:
                          age: 7
                """,
              false,
              null,
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: true
              """,
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                  bucketPolicyOnly: true
                  lifecycleRule:
                      - action:
                            type: Delete
                        condition:
                            age: 7
              """
          )
        );
    }

    @Test
    void insertAtRoot() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            "spec: 0",
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              """,
            """
              apiVersion: policy/v1beta1
              kind: PodSecurityPolicy
              spec: 0
              """
          )
        );
    }

    @Test
    void insertInSequenceEntries() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec.containers",
            "imagePullPolicy: Always",
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: <container name>
              """,
            """
              kind: Pod
              spec:
                containers:
                  - name: <container name>
                    imagePullPolicy: Always
              """
          )
        );
    }

    @Test
    void insertInSequenceEntriesWithWildcard() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.*.containers",
            "imagePullPolicy: Always",
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: <container name>
              """,
            """
              kind: Pod
              spec:
                containers:
                  - name: <container name>
                    imagePullPolicy: Always
              """
          )
        );
    }

    @Test
    void noInsertInSequenceEntriesWithWildcard() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.*.unknown",
            "imagePullPolicy: Always",
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: <container name>
              """
          )
        );
    }

    @Test
    void insertInSequenceEntriesWithDeepSearch() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$..containers",
            "imagePullPolicy: Always",
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: <container name>
              """,
            """
              kind: Pod
              spec:
                containers:
                  - name: <container name>
                    imagePullPolicy: Always
              """
          )
        );
    }

    @Test
    void noInsertInSequenceEntriesWithDeepSearch() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$..unknown",
            "imagePullPolicy: Always",
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: <container name>
              """
          )
        );
    }

    @Test
    void insertInSequenceEntriesMatchingPredicate() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec.containers[?(@.name == 'pod-0')]",
            //language=yaml
            "imagePullPolicy: Always",
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                  - name: pod-1
              """,
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                    imagePullPolicy: Always
                  - name: pod-1
              """
          )
        );
    }

    @Test
    void noChangeInSequenceEntriesNotMatchingPredicate() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec.containers[?(@.name == 'pod-x')]",
            //language=yaml
            "imagePullPolicy: Always",
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                  - name: pod-1
              """
          )
        );
    }

    @Test
    void insertBlockInSequenceEntriesWithExistingBlock() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec.containers",
            //language=yaml
            """
              securityContext:
                privileged: false
              """,
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                    securityContext:
                      foo: bar
              """,
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                    securityContext:
                      foo: bar
                      privileged: false
              """
          )
        );
    }

    @Test
    void insertNestedBlockInSequenceEntries() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.spec.containers",
            //language=yaml
            """
              securityContext:
                privileged: false
              """,
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
              """,
            """
              kind: Pod
              spec:
                containers:
                  - name: pod-0
                    securityContext:
                      privileged: false
              """
          )
        );
    }

    @Test
    void mergeMappingEntry() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.steps[?(@.uses == 'actions/setup-java')]",
            //language=yaml
            """
              with:
                cache: 'gradle'
              """,
            false,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              steps:
                - uses: actions/checkout
                - uses: actions/setup-java
              """,
            """
              steps:
                - uses: actions/checkout
                - uses: actions/setup-java
                  with:
                    cache: 'gradle'
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1275")
    @Test
    void maintainCorrectSequenceIndent() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              darwin:
                logging:
                  - 1
                  - 2
                finches:
                  species:
                    Geospiza:
                      - Sharp-beaked
                      - Common cactus
                    Camarhynchus:
                      - Woodpecker
                      - Mangrove
              """,
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              com:
                key1: value1
              """,
            """
              com:
                key1: value1
              darwin:
                logging:
                  - 1
                  - 2
                finches:
                  species:
                    Geospiza:
                      - Sharp-beaked
                      - Common cactus
                    Camarhynchus:
                      - Woodpecker
                      - Mangrove
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1292")
    @Test
    void mergeSequenceWithinMap() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              core:
                - map2:
                    value:
                      - 1
                      - 2
              """,
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              noncore:
                key1: value01
              """,
            """
              noncore:
                key1: value01
              core:
                - map2:
                    value:
                      - 1
                      - 2
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1302")
    @Test
    void mergeSequenceMapWithInMap() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              testing:
                mmap4:
                  - mmmap1: v111
                    mmmap2: v222
                  - nnmap1: v111
                    nnmap2: v222
              """,
            true,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              com:
                key1: value1
                key3: value3
              testing:
                core:
                  key1: value01
              """,
            """
              com:
                key1: value1
                key3: value3
              testing:
                core:
                  key1: value01
                mmap4:
                  - mmmap1: v111
                    mmmap2: v222
                  - nnmap1: v111
                    nnmap2: v222
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapAddAdditionalObject() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                table:
                  - name: jdk_version
                    value: 18
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              testing:
                table:
                  - name: build_tool
                    row2key2: maven
              """,
            """
              testing:
                table:
                  - name: build_tool
                    row2key2: maven
                  - name: jdk_version
                    value: 18
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapAddAdditionalDifferentObject() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                table:
                  - name2: jdk_version
                    value: 18
                """,
              false,
              "name2",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              testing:
                table:
                  - name: build_tool
                    row2key2: maven
              """,
            """
              testing:
                table:
                  - name: build_tool
                    row2key2: maven
                  - name2: jdk_version
                    value: 18
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapAddObject() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                table:
                  - name: jdk_version
                    value: 18
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              testing:
                another: value
              """,
            """
              testing:
                another: value
                table:
                  - name: jdk_version
                    value: 18
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapAddObjectFromRoot() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              testing:
                table:
                  - name: jdk_version
                    value: 18
              """,
            false,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              """,
            """
              testing:
                table:
                  - name: jdk_version
                    value: 18
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapWhenOneIdenticalObjectExistsTheSecondIsAdded() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                table:
                  - name: jdk_version
                    value: 18
                  - name: build_tool
                    row2key2: maven
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              testing:
                table:
                  - name: jdk_version
                    value: 18
              """,
            """
              testing:
                table:
                  - name: jdk_version
                    value: 18
                  - name: build_tool
                    row2key2: maven
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapWhenOneDifferentObjectExistsValuesAreChanged() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                table:
                  - name: jdk_version
                    value: 17
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              testing:
                table:
                  - name: jdk_version
                    value: 18
              """,
            """
              testing:
                table:
                  - name: jdk_version
                    value: 17
              """
          )
        );
    }

    @Test
    void mergeMappingIntoNewMapping() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                table:
                  - name: jdk_version
                    value: 17
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              foo: bar
              """,
            """
              foo: bar
              testing:
                table:
                  - name: jdk_version
                    value: 17
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapAddComplexMapping() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.spec",
              //language=yaml
              """
                serviceClaims:
                  - name: db02
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v1
                      kind: Postgres
                      name: customer-profile-database-02
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              spec:
                serviceClaims:
                  - name: db
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v1
                      kind: Postgres
                      name: customer-profile-database
              """,
            """
              spec:
                serviceClaims:
                  - name: db
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v1
                      kind: Postgres
                      name: customer-profile-database
                  - name: db02
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v1
                      kind: Postgres
                      name: customer-profile-database-02
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    void mergeSequenceMapChangeComplexMapping() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.spec",
              //language=yaml
              """
                serviceClaims:
                  - name: db
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v2
                      kind: MySQL
                      name: relation-profile-database
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              spec:
                serviceClaims:
                  - name: db
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v1
                      kind: Postgres
                      name: customer-profile-database
              """,
            """
              spec:
                serviceClaims:
                  - name: db
                    ref:
                      apiVersion: sql.tanzu.vmware.com/v2
                      kind: MySQL
                      name: relation-profile-database
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2218")
    @Test
    void existingEntryBlockWithCommentAtFirstLine() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A:
                B:
                  C:
                    D:
                      4: new desc
                    D2:
                      2: new description
                    D3:
                      2: new text
                      3: more new text
                  E: description
              """,
            false,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              A: # Comment untouched
                B:
                  C:
                    D:
                      1: something else
                      2: something else
                      3: old desc
                    D2:
                      1: old description
                    D3:
                      1: old text
              """,
            """
              A: # Comment untouched
                B:
                  C:
                    D:
                      1: something else
                      2: something else
                      3: old desc
                      4: new desc
                    D2:
                      1: old description
                      2: new description
                    D3:
                      1: old text
                      2: new text
                      3: more new text
                  E: description
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2218")
    @Test
    void existingEntryBlockWithCommentAtLastLine() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              spring:
                application:
                  description: a description
              """,
            false,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              spring:
                application:
                  name: main # Comment moved from root to previous element
              """,
            """
              spring:
                application:
                  name: main # Comment moved from root to previous element
                  description: a description
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2218")
    @Test
    void existingEntryBlockWithCommentsAllOverThePlace() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A:
                B:
                  C:
                    D:
                      3: new desc
                    D2:
                      4: d
                    D3:
                      2: new description
                    D4:
                      2: new text
              """,
            false,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              A: # Comment untouched 1
                B: # Comment untouched 2
                  C: # Comment untouched 3
                    D: # Comment untouched 4
                      1: something else
                      2: old desc # Comment moved from prefix D2 to prefix D->3
                                  # This is also part of prefix D2, but should NOT be moved to D->3
                    D2:
                      1: a
                      # Comment above tag untouched 1
                      2: b                             # Comment with a lot of spaces untouched 5
                      3: c
                    # Comment above tag untouched 2
              # with multilines
                    D3: # Comment untouched 6
                      1: old description                           # Comment with a lot of spaces moved from prefix D4 to prefix D3->2
                    D4: # Comment untouched 7
                      1: old text # Comment moved from end document to prefix D4->2
              """,
            """
              A: # Comment untouched 1
                B: # Comment untouched 2
                  C: # Comment untouched 3
                    D: # Comment untouched 4
                      1: something else
                      2: old desc # Comment moved from prefix D2 to prefix D->3
                      3: new desc
                                  # This is also part of prefix D2, but should NOT be moved to D->3
                    D2:
                      1: a
                      # Comment above tag untouched 1
                      2: b                             # Comment with a lot of spaces untouched 5
                      3: c
                      4: d
                    # Comment above tag untouched 2
              # with multilines
                    D3: # Comment untouched 6
                      1: old description                           # Comment with a lot of spaces moved from prefix D4 to prefix D3->2
                      2: new description
                    D4: # Comment untouched 7
                      1: old text # Comment moved from end document to prefix D4->2
                      2: new text
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2218")
    @Test
    void existingEntryBlockWithCommentNotAtLastLine() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              spring:
                application:
                  description: a description
              """,
            false,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              spring:
                application:
                  name: main # Some comment
                  name2: main
              """,
            """
              spring:
                application:
                  name: main # Some comment
                  name2: main
                  description: a description
              """
          )
        );
    }

    @Test
    void existingEntryBlockWithCommentOnNextBlock() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              first:
                new: value
              """,
            null,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              first:
                existing: value
              # Some comment
              second: value
              """,
            """
              first:
                existing: value
                new: value
              # Some comment
              second: value
              """
          )
        );
    }

    @Test
    void mergeScalar() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.name",
              //language=yaml
              """
                sam
                """,
              false,
              null,
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              name: jon
              """,
            """
              name: sam
              """
          )
        );
    }

    @Test
    void insertScalar() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.name",
              //language=yaml
              """
                sam
                """,
              false,
              null,
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              name:
              """,
            """
              name: sam
              """
          )
        );
    }

    @Test
    void addNewEntryToSequence() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml("$.groups",
              // language=yaml
              """
                - name: newName
                  jobs:
                    - newJob
                """,
              false, "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              groups:
                - name: analysis
                  jobs:
                    - analysis
                - name: update
                  jobs:
                    - update
              """,
            """
              groups:
                - name: analysis
                  jobs:
                    - analysis
                - name: update
                  jobs:
                    - update
                - name: newName
                  jobs:
                    - newJob
              """
          )
        );
    }

    @Test
    // Mimics `org.openrewrite.java.micronaut.UpdateSecurityYamlIfNeeded`
    void mergeEmptyStructureFollowedByCopyValue() {
        rewriteRun(
          spec -> spec.recipes(
              new MergeYaml(
                "$.spec",
                //language=yaml
                """
                  empty:
                    initially:
                  """,
                false,
                null,
                null,
                null,
                null,
                null
              ),
              new CopyValue("$.spec.level1.level2", null, "$.spec.empty.initially", null))
            .expectedCyclesThatMakeChanges(2),
          yaml(
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                level1:
                  level2: true
              """,
            """
              apiVersion: storage.cnrm.cloud.google.com/v1beta1
              kind: StorageBucket
              spec:
                level1:
                  level2: true
                empty:
                  initially: true
              """
          )
        );
    }

    @Test
    void comment() {
        rewriteRun(
          spec -> spec.recipe(
            new MergeYaml(
              "$",
              //language=yaml
              """

                  # new stuff
                new-property: value
                """,
              false,
              null,
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              # config
              activate-auto: true
              activate-mep: true
              """,
            """
              # config
              activate-auto: true
              activate-mep: true
              # new stuff
              new-property: value
              """
          )
        );
    }

    @Test
    void commentInList() {
        rewriteRun(
          spec -> spec.recipe(
            new MergeYaml(
              "$.groups",
              //language=yaml
              """

                # comment
                - id: 3

                  # foo bar
                  foo: bar
                """,
              false,
              "id",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              groups:
                - id: 1
                - id: 2
              """,
            """
              groups:
                - id: 1
                - id: 2
                # comment
                - id: 3

                  # foo bar
                  foo: bar
              """
          )
        );
    }

    @Test
    // Mimics `org.openrewrite.quarkus.AddQuarkusProperty`
    void addPropertyWitCommentAboveLastLine() {
        rewriteRun(
          spec -> spec.recipe(
            new MergeYaml(
              "$",
              //language=yaml
              """
                quarkus:
                  http:
                    # This property was added
                    root-path: /api
                """,
              true,
              null,
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              quarkus:
                http:
                  port: 9090
              """,
            """
              quarkus:
                http:
                  port: 9090
                  # This property was added
                  root-path: /api
              """
          )
        );
    }

    @Test
    void addLiteralStyleBlockAtRoot() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.",
              // language=yaml
              """
                script: |
                  #!/bin/bash
                  echo "hello"
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              some:
                object:
                  with: An existing value
              """,
            """
              some:
                object:
                  with: An existing value
              script: |
                #!/bin/bash
                echo "hello"
              """
          )
        );
    }

    @Test
    void addLiteralStyleBlockWhichDoesAlreadyExist() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.some.object",
              // language=yaml
              """
                script: |
                  #!/bin/bash
                  echo "hellow"
                something: else
                """,
              false,
              null,
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              some:
                object:
                  with: An existing value
                  script: |
                    #!/bin/bash
                    echo "hello"
              """,
            """
              some:
                object:
                  with: An existing value
                  script: |
                    #!/bin/bash
                    echo "hellow"
                  something: else
              """
          )
        );
    }

    @Test
    void addLiteralStyleBlock() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.some.very",
              // language=yaml
              """
                deep:
                  object:

                    script: | # yaml comment
                       #!/bin/bash
                        echo "hello"
                           echo "hello"
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              some:
                very:
                  deep:
                    object:
                      with: An existing value
              """,
            """
              some:
                very:
                  deep:
                    object:
                      with: An existing value

                      script: | # yaml comment
                         #!/bin/bash
                          echo "hello"
                             echo "hello"
              """
          )
        );
    }

    @Test
    // Mimics `org.openrewrite.github.UpgradeSlackNotificationVersion2Test`
    void upgradeSlackNotificationVersion2() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$..steps[?(@.uses =~ 'slackapi/slack-github-action@v1.*')]",
              // language=yaml
              """
                with:
                  method: chat.postMessage
                  token: ${{ secrets.SLACK_MORTY_BOT_TOKEN }}
                  payload: |
                    channel: "##foo-alerts"
                    text: ":boom: Unable run dependency check on: <${{ steps.get_failed_check_link.outputs.failed-check-link }}|${{ inputs.organization }}/${{ inputs.repository }}>"
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              jobs:
                build:
                  steps:
                    - name: Send notification on error
                      if: failure() && inputs.send-notification
                      uses: slackapi/slack-github-action@v1.27.0
                      with:
                        channel-id: "##foo-alerts"
                        slack-message: ":boom: Unable run dependency check on: <${{ steps.get_failed_check_link.outputs.failed-check-link }}|${{ inputs.organization }}/${{ inputs.repository }}>"
                      env:
                        SLACK_BOT_TOKEN: ${{ secrets.SLACK_MORTY_BOT_TOKEN }}
              """,
            """
              jobs:
                build:
                  steps:
                    - name: Send notification on error
                      if: failure() && inputs.send-notification
                      uses: slackapi/slack-github-action@v1.27.0
                      with:
                        channel-id: "##foo-alerts"
                        slack-message: ":boom: Unable run dependency check on: <${{ steps.get_failed_check_link.outputs.failed-check-link }}|${{ inputs.organization }}/${{ inputs.repository }}>"
                        method: chat.postMessage
                        token: ${{ secrets.SLACK_MORTY_BOT_TOKEN }}
                        payload: |
                          channel: "##foo-alerts"
                          text: ":boom: Unable run dependency check on: <${{ steps.get_failed_check_link.outputs.failed-check-link }}|${{ inputs.organization }}/${{ inputs.repository }}>"
                      env:
                        SLACK_BOT_TOKEN: ${{ secrets.SLACK_MORTY_BOT_TOKEN }}
              """
          )
        );
    }

    @Test
    void addLiteralStyleMinusBlock() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.some.object",
              // language=yaml
              """
                script: |-
                  #!/bin/bash
                  echo "hello"
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              some:
                object:
                  with: An existing value
              """,
            """
              some:
                object:
                  with: An existing value
                  script: |-
                    #!/bin/bash
                    echo "hello"
              """
          )
        );
    }

    @Test
    void addFoldedStyleBlock() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml("$.some.object",
              // language=yaml
              """
                script: >
                  #!/bin/bash
                  echo "hello"
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              some:
                object:
                  with: An existing value
              """,
            """
              some:
                object:
                  with: An existing value
                  script: >
                    #!/bin/bash
                    echo "hello"
              """
          )
        );
    }

    @Test
    void addFoldedStyleMinusBlock() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml("$.some.object",
              // language=yaml
              """
                script: >-
                  #!/bin/bash
                  echo "hello"
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            )),
          yaml(
            """
              some:
                object:
                  with: An existing value
              """,
            """
              some:
                object:
                  with: An existing value
                  script: >-
                    #!/bin/bash
                    echo "hello"
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4958")
    @Test
    void preserveSpacingWhenMergingFlowStyle() {
        rewriteRun(spec -> spec
            .recipe(new MergeYaml(//language=jsonpath
              "$.jobs[?(@.name=='test')].plan[?(@.task=='sonar')]",
              // language=yaml
              "vars: { version: 10.3 }",
              false,
              null,
              null,
              null,
              null,
              null
            )),

          yaml(// language=yaml
            """
              jobs:
              - name: test
                plan:
                - task: sonar
              """,
            // language=yaml
            """
              jobs:
              - name: test
                plan:
                - task: sonar
                  vars: { version: 10.3 }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4958")
    @Test
    void preserveSpacingWhenMergingFlowStyleNested() {
        rewriteRun(spec -> spec
            .recipe(new MergeYaml(//language=jsonpath
              "$.jobs[?(@.name=='test')].plan[?(@.task=='sonar')]",
              // language=yaml
              "vars: { mapping: { version: 10.3 } }",
              false,
              null,
              null,
              null,
              null,
              null
            )),

          yaml(// language=yaml
            """
              jobs:
              - name: test
                plan:
                - task: sonar
              """,
            // language=yaml
            """
              jobs:
              - name: test
                plan:
                - task: sonar
                  vars: { mapping: { version: 10.3 } }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4958")
    @Test
    void preserveSpacingWhenMergingFlowStyleWithNewline() {
        rewriteRun(spec -> spec
            .recipe(new MergeYaml(//language=jsonpath
              "$.jobs[?(@.name=='test')].plan[?(@.task=='sonar')]",
              // language=yaml
              """
                vars: {
                  version: 10.3 }
                """,
              false,
              null,
              null,
              null,
              null,
              null
            )),

          yaml(// language=yaml
            """
              jobs:
              - name: test
                plan:
                - task: sonar
              """,
            // language=yaml
            """
              jobs:
              - name: test
                plan:
                - task: sonar
                  vars: {
                    version: 10.3 }
              """
          )
        );
    }

    @Test
    void insertBefore() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A: a
              B:
                A: b
              """,
            null,
            null,
            null,
            Before,
            "second",
            null
          )),
          yaml(
            """
              first:
              second: 2
              third: 3
              """,
            """
              first:
              A: a
              B:
                A: b
              second: 2
              third: 3
              """
          )
        );
    }

    @Test
    void insertBeforeMultiple() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              first:
                key: one
              second:
                key: two
              """,
            null,
            null,
            null,
            Before,
            "level",
            null
          )),
          yaml(
            """
              first:
                level: one
              second:
                level: two
              """,
            """
              first:
                key: one
                level: one
              second:
                key: two
                level: two
              """
          )
        );
    }

    @Test
    void insertBeforeWithNoMatch() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A: a
              """,
            null,
            null,
            null,
            Before,
            "no-key",
            null
          )),
          yaml(
            """
              first:
              """,
            """
              first:
              A: a
              """
          )
        );
    }

    @Test
    void insertBeforeElementFirstLine() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A: a
              """,
            null,
            null,
            null,
            Before,
            "first",
            null
          )),
          yaml(
            """
              first: value
              """,
            """
              A: a
              first: value
              """
          )
        );
    }

    @Test
    void insertBeforeElementWithCommentOnFirstLine() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A: a
              B: b
              """,
            null,
            null,
            null,
            Before,
            "first",
            null
          )),
          yaml(
            """
              # Comment moved from root prefix to first
              first: value
              second: value
              """,
            """
              A: a
              B: b
              # Comment moved from root prefix to first
              first: value
              second: value
              """
          )
        );
    }

    @Test
    void insertBeforeElementWithCommentsWithNesting() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.level",
            //language=yaml
            """
              A: a
              B: b
              """,
            null,
            null,
            null,
            Before,
            "first",
            null
          )),
          yaml(
            """
              # Comment 1
              level:
                  before: value # Comment 2
                  # Comment 3
                  first: value
                  second: value
              """,
            """
              # Comment 1
              level:
                  before: value # Comment 2
                  A: a
                  B: b
                  # Comment 3
                  first: value
                  second: value
              """
          )
        );
    }

    @Test
    void insertBeforeElementWithCommentOnFirstLineWithNesting2() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.level",
            //language=yaml
            """
              A: a
              B: b
              """,
            null,
            null,
            null,
            Before,
            "first",
            null
          )),
          yaml(
            """
              # Comment 1
              level: # Comment 2
                # Comment 3
                first: value
              """,
            """
              # Comment 1
              level: # Comment 2
                A: a
                B: b
                # Comment 3
                first: value
              """
          )
        );
    }

    @Test
    void insertBeforeElementWithComments() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A: a
              """,
            null,
            null,
            null,
            Before,
            "second",
            null
          )),
          yaml(
            """
              # Some comment
              first: value
              # Some comment
              # with multilines
              second: value # Comment should not be moved from root to previous element
              """,
            """
              # Some comment
              first: value
              A: a
              # Some comment
              # with multilines
              second: value # Comment should not be moved from root to previous element
              """
          )
        );
    }

    @Test
    void insertBeforeWithKey() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.some.very.deep.object",
            //language=yaml
            """
              and: B
              """,
            null,
            null,
            null,
            Before,
            "some",
            null
          )),
          yaml(
            """
              some:
                very:
                  deep:
                    object:
                      with: A
                      some: C
              yet: another
              """,
            """
             some:
               very:
                 deep:
                   object:
                     with: A
                     and: B
                     some: C
             yet: another
             """
          )
        );
    }

    @Test
    void insertBeforeMergeList() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              widget:
                list:
                  - item 2
                another:
                  prop: value
              """,
            null,
            null,
            null,
            Before,
            "item 3",
            null
          )),
          yaml(
            """
              widget:
                list:
                  - item 1
                  # Comment untouched
                  - item 3
              """,
            """
              widget:
                list:
                  - item 1
                  - item 2
                  # Comment untouched
                  - item 3
                another:
                  prop: value
              """
          )
        );
    }

    @Test
    void insertBeforeMergeSequenceMapAddAdditionalObject() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                - name: y
                  value: 1
                """,
              false,
              "name",
              null,
              Before,
              "name: x",
              null
            )),
          yaml(
            """
              testing:
                # Comment untouched
                - name: x
                  value: 1
              """,
            """
              testing:
                - name: y
                  value: 1
                # Comment untouched
                - name: x
                  value: 1
              """
          )
        );
    }

    @Test
    void insertAfter() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A: a
              B:
                A: b
              """,
            null,
            null,
            null,
            After,
            "first",
            null
          )),
          yaml(
            """
              first:
              second: 2
              third: 3
              fourth: 4
              """,
            """
              first:
              A: a
              B:
                A: b
              second: 2
              third: 3
              fourth: 4
              """
          )
        );
    }

    @Test
    void insertAfterMultiple() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              first:
                key: one
              second:
                key: two
              """,
            null,
            null,
            null,
            After,
            "level-x",
            null
          )),
          yaml(
            """
              first:
                level-x: one
                level-y: one
              second:
                level-x: two
                level-y: two
              """,
            """
              first:
                level-x: one
                key: one
                level-y: one
              second:
                level-x: two
                key: two
                level-y: two
              """
          )
        );
    }

    @Test
    void insertAfterWithNoMatch() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A: a
              """,
            null,
            null,
            null,
            After,
            "no-key",
            null
          )),
          yaml(
            """
              first:
              """,
            """
              first:
              A: a
              """
          )
        );
    }

    @Test
    void insertAfterElementLastLine() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A: a
              """,
            null,
            null,
            null,
            After,
            "first",
            null
          )),
          yaml(
            """
              first: value
              """,
            """
              first: value
              A: a
              """
          )
        );
    }

    @Test
    void insertAfterElementWithCommentOnFirstLine() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A: a
              """,
            null,
            null,
            null,
            After,
            "first",
            null
          )),
          yaml(
            """
              # Comment untouched
              first: value
              second: value
              """,
            """
              # Comment untouched
              first: value
              A: a
              second: value
              """
          )
        );
    }

    @Test
    void insertAfterElementWithCommentsWithNesting() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.level",
            //language=yaml
            """
              A: a
              B: a
              """,
            null,
            null,
            null,
            After,
            "second",
            null
          )),
          yaml(
            """
              # Comment 1
              level:
                # Comment 2
                first: value
                # Comment 3
                second: value # Comment 4
                third: value # Comment 5
                fourth: value # Comment 6
              # Comment 7
              another: value
              """,
            """
              # Comment 1
              level:
                # Comment 2
                first: value
                # Comment 3
                second: value # Comment 4
                A: a
                B: a
                third: value # Comment 5
                fourth: value # Comment 6
              # Comment 7
              another: value
              """
          )
        );
    }

    @Test
    void insertAfterElementWithComments() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              A: a
              """,
            null,
            null,
            null,
            After,
            "second",
            null
          )),
          yaml(
            """
              # Some comment
              first: value
              # Some comment
              # with multilines
              second: value # Comment should be moved from root to previous element
              """,
            """
              # Some comment
              first: value
              # Some comment
              # with multilines
              second: value # Comment should be moved from root to previous element
              A: a
              """
          )
        );
    }

    @Test
    void insertAfterWithKey() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.some.very.deep.object",
            //language=yaml
            """
              and: B
              """,
            null,
            null,
            null,
            After,
            "with",
            null
          )),
          yaml(
            """
              some:
                very:
                  deep:
                    object:
                      with: A
                      some: C
              yet: another
              """,
            """
             some:
               very:
                 deep:
                   object:
                     with: A
                     and: B
                     some: C
             yet: another
             """
          )
        );
    }

    @Test
    void insertAfterMergeList() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$",
            //language=yaml
            """
              widget:
                list:
                  - item 2
                another:
                  prop: value
              """,
            null,
            null,
            null,
            After,
            "item 1",
            null
          )),
          yaml(
            """
              widget:
                list:
                  - item 1
                  # Comment 2
                  - item 3
              """,
            """
              widget:
                list:
                  - item 1
                  - item 2
                  # Comment 2
                  - item 3
                another:
                  prop: value
              """
          )
        );
    }

    @Test
    void insertAfterMergeSequenceMapAddAdditionalObject() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                - name: y
                  value: 1
                """,
              false,
              "name",
              null,
              After,
              "name: x",
              null
            )),
          yaml(
            """
              testing:
                # Comment untouched
                - name: x
                  value: 1
                # Comment untouched
                - name: z
                  value: 1
              """,
            """
              testing:
                # Comment untouched
                - name: x
                  value: 1
                - name: y
                  value: 1
                # Comment untouched
                - name: z
                  value: 1
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5031")
    @Test
    void preventKeysToBeAppendedToPreviousComment() {
        rewriteRun(spec -> spec
            .recipe(new MergeYaml(//language=jsonpath
              "$",
              // language=yaml
              """
                foo:
                  new-key: new-value
                """,
              false,
              null,
              null,
              null,
              null,
              null
            )),

          yaml(// language=yaml
            """
              #
              foo:
                existing-key: existing-value
              # A simple comment
              bar: bar-value
              """,
            // language=yaml
            """
              #
              foo:
                existing-key: existing-value
                new-key: new-value
              # A simple comment
              bar: bar-value
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5031")
    @Test
    void preventKeysToBeAppendedToPreviousCommentIfManyLineBreaks() {
        rewriteRun(spec -> spec
            .recipe(new MergeYaml(//language=jsonpath
              "$",
              // language=yaml
              """
                foo:
                  new-key: new-value
                """,
              false,
              null,
              null,
              null,
              null,
              null
            )),

          yaml(// language=yaml
            """
              #
              foo:
                existing-key: existing-value
              # A simple comment with trailing line breaks



              bar: bar-value
              """,
            // language=yaml
            """
              #
              foo:
                existing-key: existing-value
                new-key: new-value
              # A simple comment with trailing line breaks



              bar: bar-value
              """
          )
        );
    }

    @Test
    void invalidYaml() {
        assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.some.object",
              // language=yaml
              """
                script: |-ParseError
                """,
              false,
              "name",
              null,
              null,
              null,
              null
            ))
        ));
    }

    @Test
    void createNewKeysTrue() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                qux: quux
                s:
                         y: s
                         x: s
                """,
              false,
              null,
              null,
              null,
              null,
              true
            )),
          yaml(
            """
              foo: baz
              """,
            """
              foo: baz
              testing:
                qux: quux
                s:
                  y: s
                  x: s
              """
          )
        );
    }

    @Test
    void createNewKeysFalse() {
        rewriteRun(
          spec -> spec
            .recipe(new MergeYaml(
              "$.testing",
              //language=yaml
              """
                qux: quux
                s:
                         y: s
                         x: s
                """,
              false,
              null,
              null,
              null,
              null,
              false
            )),
          yaml(
            """
              foo: baz
              """
          )
        );
    }

    @Test
    void sourceNull() {
        assertThrows(AssertionError.class, () ->
            rewriteRun(
              spec -> spec
                .recipe(new MergeYaml(
                  "$.some.object",
                  null,
                  false,
                  "name",
                  null,
                  null,
                  null,
                  null
                ))
            ));
    }

    @Test
    void lastEntryShouldKeepItsComment() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.",
            "imagePullPolicy: Always",
            true,
            null,
            null,
            null,
            null,
            true
          )),
          yaml(
            """
            containers: ALEF # comment
            """,
            """
            containers: ALEF # comment
            imagePullPolicy: Always
            """
          )
        );
    }

    @Test
    void mergeListRespectIndentation() {
        rewriteRun(spec ->
            spec.recipe(new MergeYaml(
              "$",
              //language=yaml
              """
                widget:
                  list:
                  - item 2
                """,
              false,
              null,
              null,
              null,
              null,
              true
            )),
          yaml(
            """
              widget:
                list:
                - item 1
              """,
            """
              widget:
                list:
                - item 1
                - item 2
              """
          )
        );
    }

    @Test
    void bracketNotationAfterRoot() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$[\"a.b.c\"].d1",
            //language=yaml
            "new-key: 123",
            null,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              a.b.c:
               d1:
                e1: text
               d2:
                e2: text
              """,
            """
              a.b.c:
               d1:
                e1: text
                new-key: 123
               d2:
                e2: text
              """
          )
        );
    }

    @Test
    void bracketNotation() {
        rewriteRun(
          spec -> spec.recipe(new MergeYaml(
            "$.a[\"b.c.d1\"]",
            //language=yaml
            "new-key: 123",
            null,
            null,
            null,
            null,
            null,
            null
          )),
          yaml(
            """
              a:
               b.c.d1:
                e1: text
               d2:
                e2: text
              """,
            """
              a:
               b.c.d1:
                e1: text
                new-key: 123
               d2:
                e2: text
              """
          )
        );
    }
}
