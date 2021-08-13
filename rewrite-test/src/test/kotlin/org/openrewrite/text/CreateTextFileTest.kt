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
package org.openrewrite.text

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.*
import org.openrewrite.marker.Markers
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class CreateTextFileTest : RecipeTest<PlainText> {
    override val parser: Parser<PlainText>
        get() = PlainTextParser()

    override val recipe: Recipe
        get() = CreateTextFile("foo", ".github/CODEOWNERS", false)

    @Test
    fun hasCreatedFile(@TempDir tempDir: Path) {
        val results = recipe.run(Collections.emptyList());
        Assertions.assertThat(results).hasSize(1);
        Assertions.assertThat(results[0].after.print()).isEqualTo("foo");

    }

    @Test
    fun hasOverwrittenFile(@TempDir tempDir: Path) {
        val overwriteRecipe: Recipe = CreateTextFile("foo", ".github/CODEOWNERS", true)
        val results = overwriteRecipe.run(listOf(PlainText(Tree.randomId(), Paths.get(".github/CODEOWNERS"), Markers.EMPTY, "hello")));

        Assertions.assertThat(results).hasSize(1);
        Assertions.assertThat(results[0].after.print()).isEqualTo("foo");
    }

    @Test
    fun shouldNotChangeExistingFile(@TempDir tempDir: Path) {
        val overwriteRecipe: Recipe = CreateTextFile("foo", ".github/CODEOWNERS", false)
        val results = overwriteRecipe.run(listOf(PlainText(Tree.randomId(), Paths.get(".github/CODEOWNERS"), Markers.EMPTY, "hello")));

        Assertions.assertThat(results).hasSize(0);
    }

    @Test
    fun shouldAddAnotherFile(@TempDir tempDir: Path) {
        val overwriteRecipe: Recipe = CreateTextFile("foo", ".github/CODEOWNERSZ", false)
        val results = overwriteRecipe.run(listOf(PlainText(Tree.randomId(), Paths.get(".github/CODEOWNERS"), Markers.EMPTY, "hello")));

        Assertions.assertThat(results).hasSize(1);
        Assertions.assertThat(results[0].after.print()).isEqualTo("foo");

    }
}