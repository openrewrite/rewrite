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
import java.nio.file.Path

class ChangeValueTest : YamlRecipeTest {
    @Test
    fun changeNestedKeyValue() = assertChanged(
        recipe = ChangeValue(
            "$.metadata.name",
            "monitoring",
            null
        ),
        before = """
            apiVersion: v1
            metadata:
              name: monitoring-tools
              namespace: monitoring-tools
        """,
        after = """
            apiVersion: v1
            metadata:
              name: monitoring
              namespace: monitoring-tools
        """
    )

    @Test
    fun changeAliasedKeyValue() = assertChanged(
        recipe = ChangeValue(
            "$.*.yo",
            "howdy",
            null
        ),
        before = """
            bar:
              &abc yo: friend
            baz:
              *abc: friendly
        """,
        after = """
            bar:
              &abc yo: howdy
            baz:
              *abc: howdy
        """
    )

    @Test
    fun changeSequenceValue() = assertChanged(
        recipe = ChangeValue(
            "$.metadata.name",
            "monitoring",
            null
        ),
        before = """
            apiVersion: v1
            metadata:
              name: [monitoring-tools]
              namespace: monitoring-tools
        """,
        after = """
            apiVersion: v1
            metadata:
              name: monitoring
              namespace: monitoring-tools
        """
    )

    @Test
    fun changeRelativeKey() = assertChanged(
        recipe = ChangeValue(
            ".name",
            "monitoring",
            null
        ),
        before = """
            apiVersion: v1
            metadata:
              name: monitoring-tools
              namespace: monitoring-tools
        """,
        after = """
            apiVersion: v1
            metadata:
              name: monitoring
              namespace: monitoring-tools
        """
    )

    @Test
    fun changeSequenceKeyByWildcard() = assertChanged(
        recipe = ChangeValue(
            "$.subjects[*].kind",
            "Deployment",
            null
        ),
        before = """
            subjects:
              - kind: User
                name: some-user
              - kind: ServiceAccount
                name: monitoring-tools
        """,
        after = """
            subjects:
              - kind: Deployment
                name: some-user
              - kind: Deployment
                name: monitoring-tools
        """
    )

    @Test
    fun changeSequenceKeyByExactMatch() = assertChanged(
        recipe = ChangeValue(
            "$.subjects[?(@.kind == 'ServiceAccount')].kind",
            "Deployment",
            null
        ),
        before = """
            subjects:
              - kind: User
                name: some-user
              - kind: ServiceAccount
                name: monitoring-tools
        """,
        after = """
            subjects:
              - kind: User
                name: some-user
              - kind: Deployment
                name: monitoring-tools
        """
    )

    @Test
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("metadata: monitoring-tools")
        }.toFile()
        val nonMatchingFile = tempDir.resolve("b.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("metadata: monitoring-tools")
        }.toFile()
        val recipe = ChangeValue(".metadata", "monitoring", "**/a.yml")
        assertChanged(recipe = recipe, before = matchingFile, after = "metadata: monitoring")
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }
}
