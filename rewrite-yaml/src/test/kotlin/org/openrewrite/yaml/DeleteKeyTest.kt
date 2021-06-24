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

class DeleteKeyTest : YamlRecipeTest {

    @Test
    fun deleteNestedKey() = assertChanged(
        recipe = DeleteKey("/metadata/name", null),
        before = """
            apiVersion: v1
            metadata:
              name: monitoring-tools
              namespace: monitoring-tools
        """,
        after = """
            apiVersion: v1
            metadata:
              namespace: monitoring-tools
        """
    )

    @Test
    fun deleteRelativeKey() = assertChanged(
        recipe = DeleteKey("name", null),
        before = """
            apiVersion: v1
            metadata:
              name: monitoring-tools
              namespace: monitoring-tools
        """,
        after = """
            apiVersion: v1
            metadata:
              namespace: monitoring-tools
        """
    )

    @Test
    fun deleteSequenceKey() = assertChanged(
        recipe = DeleteKey("/subjects/kind", null),
        before = """
            subjects:
              - kind: ServiceAccount
                name: monitoring-tools
        """,
        after = """
            subjects:
              - name: monitoring-tools
        """
    )

    @Test
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("apiVersion: v1")
        }.toFile()
        val nonMatchingFile = tempDir.resolve("b.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("apiVersion: v1")
        }.toFile()
        val recipe = DeleteKey("/apiVersion", "**/a.yml")
        assertChanged(recipe = recipe, before = matchingFile, after = "")
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }
}
