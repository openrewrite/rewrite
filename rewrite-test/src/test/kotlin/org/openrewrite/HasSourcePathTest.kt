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
package org.openrewrite

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.text.ChangeText
import org.openrewrite.text.PlainText
import org.openrewrite.text.PlainTextParser
import java.nio.file.Path

class HasSourcePathTest : RecipeTest<PlainText> {
    override val parser: Parser<PlainText>
        get() = PlainTextParser()

    override val recipe: Recipe
        get() = object : ChangeText("hello jon") {
            override fun getSingleSourceApplicableTest() =
                HasSourcePath<ExecutionContext>("**/hello.txt")
        }

    @Example
    @Test
    fun hasFileMatch(@TempDir tempDir: Path) = assertChangedBase(
        before = tempDir.resolve("a/b/hello.txt").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("hello world")
        }.toFile(),
        after = "hello jon"
    )

    @Test
    fun hasNoFileMatch(@TempDir tempDir: Path) = assertUnchangedBase(
        before = tempDir.resolve("a/b/goodbye.txt").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("hello world")
        }.toFile()
    )
}
