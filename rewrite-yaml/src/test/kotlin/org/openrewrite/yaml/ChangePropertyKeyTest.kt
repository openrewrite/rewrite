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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.Issue
import java.nio.file.Path

class ChangePropertyKeyTest : YamlRecipeTest {
    private val changeProp = ChangePropertyKey(
        "management.metrics.binders.files.enabled",
        "management.metrics.enable.process.files",
        null
    )

    @Test
    fun singleEntry() = assertChanged(
        recipe = changeProp,
        before = "management.metrics.binders.files.enabled: true",
        after = "management.metrics.enable.process.files: true"
    )

    @Test
    fun nestedEntry() = assertChanged(
        recipe = changeProp,
        before = """
            unrelated.property: true
            management.metrics:
                binders:
                    jvm.enabled: true
                    files.enabled: true
        """,
        after = """
            unrelated.property: true
            management.metrics:
                binders.jvm.enabled: true
                enable.process.files: true
        """
    )

    @Test
    fun nestedEntryEmptyPartialPathRemoved() = assertChanged(
        recipe = changeProp,
        before = """
            unrelated.property: true
            management.metrics:
                binders:
                    files.enabled: true
        """,
        after = """
            unrelated.property: true
            management.metrics.enable.process.files: true
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1114")
    @Disabled
    fun singleEntryChangingSubpathToOneMoreNestedKey() = assertChanged(
        recipe = ChangePropertyKey(
            "a.b.c",
            "a.b.c.d",
            null
        ),
        before = "a.b.c: true",
        after = "a.b.c.d: true"
    )

    @Test
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("management.metrics.binders.files.enabled: true")
        }.toFile()
        val nonMatchingFile = tempDir.resolve("b.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("management.metrics.binders.files.enabled: true")
        }.toFile()
        val recipe = ChangePropertyKey(
            "management.metrics.binders.files.enabled",
            "management.metrics.enable.process.files",
            "**/a.yml"
        )
        assertChanged(recipe = recipe, before = matchingFile, after = "management.metrics.enable.process.files: true")
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangePropertyKey(null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("newPropertyKey")
        assertThat(valid.failures()[1].property).isEqualTo("oldPropertyKey")

        recipe = ChangePropertyKey(null, "management.metrics.enable.process.files", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("oldPropertyKey")

        recipe = ChangePropertyKey("management.metrics.binders.files.enabled", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("newPropertyKey")

        recipe =
            ChangePropertyKey(
                "management.metrics.binders.files.enabled",
                "management.metrics.enable.process.files",
                null
            )
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }
}
