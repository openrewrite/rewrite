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
import java.nio.file.Path

class MergeYamlTest : YamlRecipeTest {
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
    fun existingBlock() = assertChanged(
        recipe = MergeYaml(
            "$.spec",
            """
            lifecycleRule:
                - condition:
                    age: 7
            """.trimIndent(),
            false,
            null
        ),
        before = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: true
                lifecycleRule:
                    - action:
                          type: Delete
                      condition:
                          age: 1
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

    @Test
    fun scalar() = assertChanged(
        recipe = MergeYaml(
            "$.spec",
            """
              bucketPolicyOnly: true
            """.trimIndent(),
            false,
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

    @Test
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.yml").toFile().apply {
            writeText("apiVersion: policy/v1beta1")
        }
        val nonMatchingFile = tempDir.resolve("b.yml").toFile().apply {
            writeText("apiVersion: policy/v1beta1")
        }

        val recipe = MergeYaml("$", "spec: 0", true, "**/a.yml")
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
