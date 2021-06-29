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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.yaml.MergeYaml
import org.openrewrite.yaml.YamlRecipeTest

class MergeYamlTest : YamlRecipeTest {

    @Test
    fun mustMergeYamlWhenBlockExists() = assertChanged(
        recipe = MergeYaml(
            "/spec/lifecycleRule",
            """
            lifecycleRule:
                - condition:
                    age: 7
            """.trimIndent(),
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
        """,
        cycles = 2
    )

    @Test
    fun mustMergeMultipleYamlDocuments() = assertChanged(
        recipe = MergeYaml(
            "/spec/lifecycleRule",
            """
            lifecycleRule:
                - condition:
                    age: 7
            """.trimIndent(),
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
            ---
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
            ---
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
    fun mustMergeYamlWhenBlockDoesntExist() = assertChanged(
        recipe = MergeYaml(
            "/spec/lifecycleRule",
            """
              lifecycleRule:
                  - action:
                        type: Delete
                    condition:
                        age: 7
            """.trimIndent(),
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
    fun mustMergeYamlForScalar() = assertChanged(
        recipe = MergeYaml(
            "/spec/bucketPolicyOnly",
            """
              bucketPolicyOnly: true
            """.trimIndent(),
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
        """,
        cycles = 2
    )

    @Test
    fun mustMergeYamlWhenApplicable() = assertChanged(
        recipe = MergeYaml(
            "/",
            """
            spec:
              containers:
              - env:
                - name: POD_NAMESPACE
                - name: POD_NAME
                  valueFrom:
                    fieldRef:
                      apiVersion: v1
                      fieldPath: metadata.name
            """.trimIndent(),
            """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: gatekeeper
            spec:
              containers:
              - name: manager
            """.trimIndent(),
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: gatekeeper
            spec:
              containers:            
              - name: manager
                env:
                - name: POD_NAMESPACE
                  valueFrom:
                    fieldRef:
                      apiVersion: v1
                      fieldPath: metadata.namespace
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: gatekeeper
            spec:
              containers:            
              - name: manager
                env:
                - name: POD_NAMESPACE
                  valueFrom:
                    fieldRef:
                      apiVersion: v1
                      fieldPath: metadata.namespace
                - name: POD_NAME
                  valueFrom:
                    fieldRef:
                      apiVersion: v1
                      fieldPath: metadata.name
        """,
        cycles = 2
    )

    @Test
    fun mustNotMergeYamlWhenNotApplicable() = assertUnchanged(
        recipe = MergeYaml(
            "/",
            """
            spec:
              containers:
              - env:
                - name: POD_NAMESPACE
                - name: POD_NAME
                  valueFrom:
                    fieldRef:
                      apiVersion: v1
                      fieldPath: metadata.name
            """.trimIndent(),
            """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: not-gatekeeper
            """.trimIndent(),
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: gatekeeper
            spec:
              containers:            
              - name: manager
                env:
                - name: POD_NAMESPACE
                  valueFrom:
                    fieldRef:
                      apiVersion: v1
                      fieldPath: metadata.namespace
        """
    )

    @Test
    fun mustMergeYamlFragmentWhenApplicable() = assertChanged(
        recipe = MergeYaml(
            "/spec/containers",
            """
            containers:
                - env:
                    - name: POD_NAMESPACE
                    - name: POD_NAME
                      valueFrom:
                        fieldRef:
                          apiVersion: v1
                          fieldPath: metadata.name
            """.trimIndent(),
            """
            containers:
                - name: manager
            """.trimIndent(),
        ),
        before = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: gatekeeper
            spec:
              containers:            
              - name: manager
                env:
                - name: POD_NAMESPACE
                  valueFrom:
                    fieldRef:
                      apiVersion: v1
                      fieldPath: metadata.namespace
        """,
        after = """
            apiVersion: v1
            kind: Pod
            metadata:
              labels:
                app: gatekeeper
            spec:
              containers:            
              - name: manager
                env:
                - name: POD_NAMESPACE
                  valueFrom:
                    fieldRef:
                      apiVersion: v1
                      fieldPath: metadata.namespace
                - name: POD_NAME
                  valueFrom:
                    fieldRef:
                      apiVersion: v1
                      fieldPath: metadata.name
        """,
        cycles = 2
    )

}
