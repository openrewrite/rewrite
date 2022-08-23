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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.Tree
import org.openrewrite.marker.Markers
import java.nio.file.Paths

class CreateTextFileTest : RecipeTest<PlainText> {
    override val parser: Parser<PlainText>
        get() = PlainTextParser()

    override val recipe: Recipe
        get() = CreateTextFile("foo", ".github/CODEOWNERS", false)

    @Test
    fun hasCreatedFile() {
        val results = recipe.run(emptyList()).results
        assertThat(results).hasSize(1)
        assertThat(results[0].after!!.printAll()).isEqualTo("foo")
        assertThat(results[0].after!!.sourcePath).isEqualTo(Paths.get(".github/CODEOWNERS"))
    }

    @Test
    fun hasOverwrittenFile() {
        val overwriteRecipe: Recipe = CreateTextFile("foo", ".github/CODEOWNERS", true)
        val results = overwriteRecipe.run(listOf(PlainText(Tree.randomId(), Paths.get(".github/CODEOWNERS"), Markers.EMPTY,null, false, null, null, "hello"))).results

        assertThat(results).hasSize(1)
        assertThat(results[0].after!!.printAll()).isEqualTo("foo")
    }

    @Test
    fun shouldNotChangeExistingFile() {
        val overwriteRecipe: Recipe = CreateTextFile("foo", ".github/CODEOWNERS", false)
        val results = overwriteRecipe.run(listOf(PlainText(Tree.randomId(), Paths.get(".github/CODEOWNERS"), Markers.EMPTY, null, false, null, null, "hello"))).results

        assertThat(results).hasSize(0)
    }

    @Test
    fun shouldNotChangeExistingFileWhenOverwriteNull() {
        val overwriteRecipe: Recipe = CreateTextFile("foo", ".github/CODEOWNERS", null)
        val results = overwriteRecipe.run(listOf(PlainText(Tree.randomId(), Paths.get(".github/CODEOWNERS"), Markers.EMPTY,null, false, null, null, "hello"))).results

        assertThat(results).hasSize(0)
    }

    @Test
    fun shouldAddAnotherFile() {
        val overwriteRecipe: Recipe = CreateTextFile("foo", ".github/CODEOWNERSZ", false)
        val results = overwriteRecipe.run(listOf(PlainText(Tree.randomId(), Paths.get(".github/CODEOWNERS"), Markers.EMPTY,null, false, null,null, "hello"))).results

        assertThat(results).hasSize(1)
        assertThat(results[0].after!!.printAll()).isEqualTo("foo")
    }
}
