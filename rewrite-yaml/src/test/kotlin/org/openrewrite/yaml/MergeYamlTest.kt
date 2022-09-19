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
package org.openrewrite.yaml

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.Issue
import org.openrewrite.test.RewriteTest
import org.openrewrite.yaml.Assertions.yaml
import java.nio.file.Path

class MergeYamlTest : YamlRecipeTest, RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1469")
    @Test
    fun emptyDocument() = assertChanged(
        recipe = MergeYaml(
            "$",
            """
                spring:
                  application:
                    name: update
                    description: a description
            """.trimIndent(),
            false,
            null,
            null
        ),
        before = "",
        after = """
            spring:
              application:
                name: update
                description: a description
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/905")
    @Test
    fun existingMultipleEntryBlock() = assertChanged(
        recipe = MergeYaml(
            "$",
            """
                spring:
                  application:
                    name: update
                    description: a description
            """.trimIndent(),
            false,
            null,
            null
        ),
        before = """
            spring:
              application:
                name: main
        """,
        after = """
            spring:
              application:
                name: update
                description: a description
        """
    )

    @Test
    fun nonExistentBlock() = assertChanged(
        recipe = MergeYaml(
            "$.spec",
            """
              lifecycleRule:
                  - action:
                        type: Delete
                    condition:
                        age: 7
            """.trimIndent(),
            false,
            null,
            null
        ),
        before = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: true
        """,
        after = """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1598")
    @Test
    fun mergeList() = rewriteRun(
        { spec ->
            spec.recipe(
                MergeYaml(
                    "$",
                    """
                      widget:
                        list:
                          - item 2
                    """,
                    false, null, null
                )
            )
        },
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
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1598")
    @Test
    fun mergeListAcceptTheirs() = rewriteRun(
        { spec ->
            spec.recipe(
                MergeYaml(
                    "$",
                    """
                      widget:
                        list:
                          - item 2
                    """,
                    true, null, null
                )
            )
        },
        yaml(
            """
              widget:
                list:
                  - item 1
            """
        )
    )

    @Test
    fun scalar() = assertChanged(
        recipe = MergeYaml(
            "$.spec",
            """
              bucketPolicyOnly: true
            """.trimIndent(),
            false,
            null,
            null
        ),
        before = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: false
        """,
        after = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: true
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/418")
    @Test
    fun insertYaml() = assertChanged(
        recipe = MergeYaml(
            "$.spec",
            """
              lifecycleRule:
                  - action:
                        type: Delete
                    condition:
                        age: 7
            """.trimIndent(),
            false,
            null,
            null
        ),
        before = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: true
        """,
        after = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: true
                lifecycleRule:
                    - action:
                          type: Delete
                      condition:
                          age: 7
        """,
        cycles = 2
    )

    @Test
    fun insertAtRoot() = assertChanged(
        recipe = MergeYaml(
            "$",
            "spec: 0",
            true,
            null,
            null
        ),
        before = """
          apiVersion: policy/v1beta1
          kind: PodSecurityPolicy
        """,
        after = """
          apiVersion: policy/v1beta1
          kind: PodSecurityPolicy
          spec: 0
        """
    )

    @Test
    fun insertInSequenceEntries() = assertChanged(
        recipe = MergeYaml(
            "$.spec.containers",
            "imagePullPolicy: Always",
            true,
            null,
            null
        ),
        before = """
            kind: Pod
            spec:
              containers:
                - name: <container name>
        """,
        after = """
            kind: Pod
            spec:
              containers:
                - name: <container name>
                  imagePullPolicy: Always
        """
    )

    @Test
    @Disabled
    fun insertInSequenceEntriesMatchingPredicate() = assertChanged(
        recipe = MergeYaml(
            "$.spec.containers[?(@.name == 'pod-0')]",
            "imagePullPolicy: Always",
            true,
            null,
            null
        ),
        before = """
        kind: Pod
        spec:
          containers:
            - name: pod-0
            - name: pod-1
    """,
        after = """
        kind: Pod
        spec:
          containers:
            - name: pod-0
              imagePullPolicy: Always
            - name: pod-1
    """
    )

    @Test
    fun insertBlockInSequenceEntriesWithExistingBlock() = assertChanged(
        recipe = MergeYaml(
            "$.spec.containers",
            """
              securityContext:
                privileged: false
            """.trimIndent(),
            true,
            null,
            null
        ),
        before = """
            kind: Pod
            spec:
              containers:
                - name: pod-0
                  securityContext:
                    foo: bar
        """,
        after = """
            kind: Pod
            spec:
              containers:
                - name: pod-0
                  securityContext:
                    foo: bar
                    privileged: false
        """
    )

    @Test
    fun insertNestedBlockInSequenceEntries() = assertChanged(
        recipe = MergeYaml(
            "$.spec.containers",
            """
              securityContext:
                privileged: false
            """.trimIndent(),
            true,
            null,
            null
        ),
        before = """
            kind: Pod
            spec:
              containers:
                - name: pod-0
        """,
        after = """
            kind: Pod
            spec:
              containers:
                - name: pod-0
                  securityContext:
                    privileged: false
        """
    )

    @Test
    fun mergeMappingEntry() = assertChanged(
        recipe = MergeYaml(
            "$.steps[?(@.uses == 'actions/setup-java')]",
            """
              with:
                cache: 'gradle'
            """.trimIndent(),
            false,
            null,
            null
        ),
        before = """
            steps:
              - uses: actions/checkout
              - uses: actions/setup-java
        """,
        after = """
            steps:
              - uses: actions/checkout
              - uses: actions/setup-java
                with:
                  cache: 'gradle'
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1275")
    @Test
    fun maintainCorrectSequenceIndent() = assertChanged(
        recipe = MergeYaml(
            "$",
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
            """.trimIndent(),
            true,
            null,
            null
        ),
        before = """
            com:
              key1: value1
        """,
        after = """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1292")
    @Test
    fun mergeSequenceWithinMap() = assertChanged(
        recipe = MergeYaml(
            "$",
            """
               core:
                 - map2:
                     value:
                       - 1
                       - 2
            """.trimIndent(),
            true,
            null,
            null
        ),
        before = """
            noncore:
              key1: value01
        """,
        after = """
            noncore:
              key1: value01
            core:
              - map2:
                  value:
                    - 1
                    - 2
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1302")
    @Test
    fun mergeSequenceMapWithInMap() = assertChanged(
        recipe = MergeYaml(
            "$",
            """
            testing:
              mmap4:
                - mmmap1: v111
                  mmmap2: v222
                - nnmap1: v111
                  nnmap2: v222
            """.trimIndent(),
            true,
            null,
            null
        ),
        before = """
            com:
              key1: value1
              key3: value3
            testing:
              core:
                key1: value01
        """,
        after = """
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

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    fun mergeSequenceMapAddAdditionalObject() = assertChanged(
        recipe = MergeYaml(
            "$.testing",
            """
              table:
                - name: jdk_version
                  value: 18
            """.trimIndent(),
            false,
            null,
            "name"
        ),
        before = """
            testing:
              table:
                - name: build_tool
                  row2key2: maven
            """,
        after = """
            testing:
              table:
                - name: build_tool
                  row2key2: maven
                - name: jdk_version
                  value: 18
        """,
        expectedCyclesThatMakeChanges = 2
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    fun mergeSequenceMapAddObject() = assertChanged(
        recipe = MergeYaml(
            "$.testing",
            """
              table:
                - name: jdk_version
                  value: 18
            """.trimIndent(),
            false,
            null,
            "name"
        ),
        before = """
            testing:
              another: value
        """,
        after = """
            testing:
              another: value
              table:
                - name: jdk_version
                  value: 18
        """,
        expectedCyclesThatMakeChanges = 2
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    fun mergeSequenceMapAddObjectFromRoot() = assertChanged(
        recipe = MergeYaml(
            "$",
            """
            testing:
              table:
                - name: jdk_version
                  value: 18
            """.trimIndent(),
            false,
            null,
            null
        ),
        before = """
        """,
        after = """
            testing:
              table:
                - name: jdk_version
                  value: 18
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    fun mergeSequenceMapWhenOneIdenticalObjectExistsTheSecondIsAdded() = assertChanged(
        recipe = MergeYaml(
            "$.testing",
            """
              table:
                - name: jdk_version
                  value: 18
                - name: build_tool
                  row2key2: maven
            """.trimIndent(),
            false,
            null,
            "name"
        ),
        before = """
            testing:
              table:
                - name: jdk_version
                  value: 18
        """,
        after = """
            testing:
              table:
                - name: jdk_version
                  value: 18
                - name: build_tool
                  row2key2: maven
        """,
        expectedCyclesThatMakeChanges = 2
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    fun mergeSequenceMapWhenOneDifferentObjectExistsValuesAreChanged() = assertChanged(
        recipe = MergeYaml(
            "$.testing",
            """
              table:
                - name: jdk_version
                  value: 17
            """.trimIndent(),
            false,
            null,
            "name"
        ),
        before = """
            testing:
              table:
                - name: jdk_version
                  value: 18
        """,
        after = """
            testing:
              table:
                - name: jdk_version
                  value: 17
        """,
        expectedCyclesThatMakeChanges = 2
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    fun mergeSequenceMapAddComplexMapping() = assertChanged(
        recipe = MergeYaml(
            "$.spec",
            """
              serviceClaims:
                - name: db02
                  ref:
                    apiVersion: sql.tanzu.vmware.com/v1
                    kind: Postgres
                    name: customer-profile-database-02
            """.trimIndent(),
            false,
            null,
            "name"
        ),
        before = """
            spec:
              serviceClaims:
                - name: db
                  ref:
                    apiVersion: sql.tanzu.vmware.com/v1
                    kind: Postgres
                    name: customer-profile-database
        """,
        after = """
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
        """,
        expectedCyclesThatMakeChanges = 2
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2157")
    @Test
    fun mergeSequenceMapChangeComplexMapping() = assertChanged(
        recipe = MergeYaml(
            "$.spec",
            """
              serviceClaims:
                - name: db
                  ref:
                    apiVersion: sql.tanzu.vmware.com/v2
                    kind: MySQL
                    name: relation-profile-database
            """.trimIndent(),
            false,
            null,
            "name"
        ),
        before = """
            spec:
              serviceClaims:
                - name: db
                  ref:
                    apiVersion: sql.tanzu.vmware.com/v1
                    kind: Postgres
                    name: customer-profile-database
        """,
        after = """
            spec:
              serviceClaims:
                - name: db
                  ref:
                    apiVersion: sql.tanzu.vmware.com/v2
                    kind: MySQL
                    name: relation-profile-database
        """,
        expectedCyclesThatMakeChanges = 2
    )

    @Test
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.yml").toFile().apply {
            writeText("apiVersion: policy/v1beta1")
        }
        val nonMatchingFile = tempDir.resolve("b.yml").toFile().apply {
            writeText("apiVersion: policy/v1beta1")
        }

        val recipe = MergeYaml("$", "spec: 0", true, "**/a.yml", null)
        assertChanged(
            recipe = recipe,
            before = matchingFile,
            after = """
                apiVersion: policy/v1beta1
                spec: 0
            """.trimIndent()
        )
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }
}
