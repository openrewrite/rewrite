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
import java.nio.file.Path

@Suppress("UnusedProperty")
class MergePropertyTest : PropertiesRecipeTest {

    @Test
    fun changeFirstKey() = assertChanged(
        recipe = MergeProperty(
            "app",
            "new-app",
            null
        ),
        before = """
            app.settings.version=1.0
            app.settings.environment=test
            app.settings.metrics=enabled
        """,
        after = """
            new-app.settings.version=1.0
            new-app.settings.environment=test
            new-app.settings.metrics=enabled
        """
    )

    @Test
    fun changeSubKey() = assertChanged(
        recipe = MergeProperty(
            "settings",
            "new.settings.name.here",
            null
        ),
        before = """
            app.settings.version=1.0
            app.settings.environment=test
            app.settings.metrics=enabled
        """,
        after = """
            app.new.settings.name.here.version=1.0
            app.new.settings.name.here.environment=test
            app.new.settings.name.here.metrics=enabled
        """
    )

    @Test
    fun changeEndKey() = assertChanged(
        recipe = MergeProperty(
            "metrics",
            "metrics.prometheus",
            null
        ),
        before = """
            app.settings.version=1.0
            app.settings.environment=test
            app.settings.metrics=enabled
        """,
        after = """
            app.settings.version=1.0
            app.settings.environment=test
            app.settings.metrics.prometheus=enabled
        """
    )

    @Test
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.properties").toFile().apply {
            writeText("management=true")
        }
        val nonMatchingFile = tempDir.resolve("b.properties").toFile().apply {
            writeText("management=true")
        }

        val recipe = MergeProperty(
            "management",
            "management.metrics.enable.process.files",
            "**/a.properties"
        )
        assertChanged(
            recipe = recipe,
            before = matchingFile,
            after = """
                management.metrics.enable.process.files=true
            """
        )
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = AddProperty(null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("property")
        assertThat(valid.failures()[1].property).isEqualTo("value")

        recipe = AddProperty(
            null,
            "management.metrics.enable.process.files",
            null
        )
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("property")

        recipe = AddProperty(
            "management.metrics.binders.files.enabled",
            null,
            null
        )
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("value")

        recipe =
            AddProperty(
                "management.metrics.binders.files.enabled",
                "true",
                null
            )
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }
}
