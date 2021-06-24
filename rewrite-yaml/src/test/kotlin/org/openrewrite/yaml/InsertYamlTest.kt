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
package org.openrewrite.yaml

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.Issue
import org.openrewrite.config.Environment
import java.nio.file.Path

class InsertYamlTest : YamlRecipeTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/418")
    @Test
    fun insertYaml() = assertChanged(
        recipe = InsertYaml(
            "/spec",
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
    fun insertAtRoot() = assertChanged(
        recipe = InsertYaml(
            "/",
            "spec: 0",
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
        recipe = InsertYaml(
            "/spec/containers",
            "imagePullPolicy: Always",
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
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("apiVersion: policy/v1beta1")
        }.toFile()
        val nonMatchingFile = tempDir.resolve("b.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("apiVersion: policy/v1beta1")
        }.toFile()

        val recipe = InsertYaml("/", "spec: 0", "**/a.yml")
        assertChanged(
                recipe = recipe,
                before = matchingFile,
                after = """
                    apiVersion: policy/v1beta1
                    spec: 0
        """.trimIndent())
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }
}
