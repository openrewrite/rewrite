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
import org.openrewrite.Issue
import java.nio.file.Path

@Suppress("UnusedProperty")
class ChangePropertyValueTest : PropertiesRecipeTest {

    override val recipe = ChangePropertyValue(
        "management.metrics.binders.files.enabled",
        "false",
        null,
        null
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/575")
    @Test
    fun preserveComment() = assertChanged(
        parser = PropertiesParser(),
        before = """
            management.metrics.binders.files.enabled=true
            # comment
        """,
        after = """
            management.metrics.binders.files.enabled=false
            # comment
        """
    )

    @Test
    fun changeValue() = assertChanged(
        before = "management.metrics.binders.files.enabled=true",
        after = "management.metrics.binders.files.enabled=false"
    )

    @Test
    fun conditionallyChangeValue() = assertChanged(
        parser = PropertiesParser(),
        recipe = ChangePropertyValue("quarkus.quartz.store-type", "jdbc-cmt", "db", null),
        before = "quarkus.quartz.store-type=db",
        after = "quarkus.quartz.store-type=jdbc-cmt"
    )

    @Test
    fun conditionallyChangeValueNoChange() = assertUnchanged(
        parser = PropertiesParser(),
        recipe = ChangePropertyValue("quarkus.quartz.store-type", "jdbc-cmt", "cache", null),
        before = "quarkus.quartz.store-type=db"
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
        val recipe = ChangePropertyValue("management.metrics", "false", "true", "**/a.properties")
        assertChanged(recipe = recipe, before = matchingFile, after = "management.metrics=false")
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangePropertyValue(null, null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("newValue")
        assertThat(valid.failures()[1].property).isEqualTo("propertyKey")

        recipe = ChangePropertyValue(null, "false", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("propertyKey")

        recipe = ChangePropertyValue("management.metrics.binders.files.enabled", null, null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("newValue")

        recipe = ChangePropertyValue("management.metrics.binders.files.enabled", "false", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue

        recipe = ChangePropertyValue("management.metrics.binders.files.enabled", "false", "true", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }
}
