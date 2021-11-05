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
package org.openrewrite.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.Issue
import java.nio.file.Path

@Suppress("UnusedProperty")
class ChangePropertyKeyTest : PropertiesRecipeTest {
    override val recipe = ChangePropertyKey(
        "management.metrics.binders.files.enabled",
        "management.metrics.enable.process.files",
        null,
        null
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/575")
    @Test
    fun preserveComment() = assertChanged(
        before = """
            # comment
            management.metrics.binders.files.enabled=true
        """,
        after = """
            # comment
            management.metrics.enable.process.files=true
        """
    )

    @Test
    fun changeKey() = assertChanged(
        before = "management.metrics.binders.files.enabled=true",
        after = "management.metrics.enable.process.files=true"
    )

    @ParameterizedTest
    @ValueSource(
        strings = [
            "acme.my-project.person.first-name",
            "acme.myProject.person.firstName",
            "acme.my_project.person.first_name",
        ]
    )
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    fun relaxedBinding(propertyKey: String) = assertChanged(
        recipe = ChangePropertyKey(propertyKey, "acme.my-project.person.change-to", true, null),
        before = """
            acme.my-project.person.first-name=example
            management.contextPath=/manage
        """,
        after = """
            acme.my-project.person.change-to=example
            management.contextPath=/manage
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    fun exactMatch() = assertChanged(
        recipe = ChangePropertyKey(
            "acme.my-project.person.first-name",
            "acme.my-project.person.change-to",
            false,
            null
        ),
        before = """
            acme.my-project.person.first-name=example
            acme.myProject.person.firstName=example
            acme.my_project.person.first_name=example
        """,
        after = """
            acme.my-project.person.change-to=example
            acme.myProject.person.firstName=example
            acme.my_project.person.first_name=example
        """
    )

    @Test
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.properties").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("management.metrics=true")
        }.toFile()
        val nonMatchingFile = tempDir.resolve("b.properties").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("management.metrics=true")
        }.toFile()
        val recipe = ChangePropertyKey("management.metrics", "management.stats", null, "**/a.properties")
        assertChanged(recipe = recipe, before = matchingFile, after = "management.stats=true")
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangePropertyKey(null, null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("newPropertyKey")
        assertThat(valid.failures()[1].property).isEqualTo("oldPropertyKey")

        recipe = ChangePropertyKey(null, "management.metrics.enable.process.files", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("oldPropertyKey")

        recipe = ChangePropertyKey("management.metrics.binders.files.enabled", null, null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("newPropertyKey")

        recipe =
            ChangePropertyKey(
                "management.metrics.binders.files.enabled",
                "management.metrics.enable.process.files",
                null,
                null
            )
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }
}
