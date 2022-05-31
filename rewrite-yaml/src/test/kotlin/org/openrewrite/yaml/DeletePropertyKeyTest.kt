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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.Issue
import org.openrewrite.Recipe
import java.nio.file.Path

class DeletePropertyKeyTest : YamlRecipeTest {
    override val recipe: Recipe
        get() = DeleteProperty("management.metrics.binders.files.enabled", null, null, null)

    @Test
    fun singleEntry() = assertChanged(
        before = "management.metrics.binders.files.enabled: true",
        after = ""
    )

    @Test
    fun downDeeper() = assertChanged(
        before = """
          management.metrics:
            enabled: true
            binders.files.enabled: true
          server.port: 8080
        """,
        after = """
          management.metrics:
            enabled: true
          server.port: 8080
        """
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
        recipe = DeleteProperty(propertyKey, false, true, null),
        before = "acme.my-project.person.first-name: example",
        after = ""
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    fun exactMatch() = assertChanged(
        recipe = DeleteProperty("acme.my-project.person.first-name", false, false, null),
        before = """
            acme.myProject.person.firstName: example
            acme.my_project.person.first_name: example
            acme.my-project.person.first-name: example
        """,
        after = """
            acme.myProject.person.firstName: example
            acme.my_project.person.first_name: example
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
        val recipe = DeleteProperty("apiVersion", true, null, "**/a.yml")
        assertChanged(recipe = recipe, before = matchingFile, after = "")
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1769")
    @Test
    fun `DeleteProperty preserves original indent structure of existing hierarchy`() = assertChanged(
        recipe = DeleteProperty("my.old.key", false, null, null),
        before = """
          my:
            old:
              key:
                color: blue
                style: retro
            other:
              key: qwe
        """,
        after = """
          my:
            other:
              key: qwe
        """
    )
}
