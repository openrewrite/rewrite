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

class CopyValueTest : YamlRecipeTest {
    @Test
    fun copyValueAndItsFormatting() = assertChanged(
        recipe = CopyValue(".source", ".destination", null),
        before = """
            id: something
            source:   password
            destination: whatever
        """,
        after = """
            id: something
            source:   password
            destination:   password
        """
    )

    @Test
    fun onlyCopiesScalars() = assertUnchanged(
        recipe = CopyValue(".source", ".destination", null),
        before = """
            id: something
            source:
                complex:
                    structure: 1
            destination: whatever
        """
    )

    @Test
    fun insertCopyValueAndRemoveSource() = assertChanged(
        recipe = MergeYaml("$", "destination: TEMP", true, null, null)
            .doNext(CopyValue(".source", ".destination", null))
            .doNext(DeleteKey(".source", null)),
        before = """
            id: something
            source:   password
        """,
        after = """
            id: something
            destination:   password
        """
    )

    @Test
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText(//language=yml
                """
                source: password
                destination: whatever
        """.trimIndent()
            )
        }.toFile()
        val nonMatchingFile = tempDir.resolve("b.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText(//language=yml
                """
                source: password
                destination: whatever
        """.trimIndent()
            )
        }.toFile()

        val recipe = CopyValue(".source", ".destination", "**/a.yml")
        assertChanged(
            recipe = recipe,
            before = matchingFile,
            after = """
                source: password
                destination: password
        """
        )
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }
}
