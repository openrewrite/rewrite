/*
 * Copyright 2022 the original author or authors.
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

import org.junit.jupiter.api.Test
import org.openrewrite.test.RewriteTest
import java.nio.file.Paths
import org.openrewrite.InMemoryExecutionContext
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.RenameFile
import org.openrewrite.test.RecipeSpec
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile


class RenameTextFileTest : RewriteTest {

/**
companion object {
    @TempDir
    lateinit var tempDir: Path

    //var tempFile: Path = createTempFile(tempDir,"foo.bar.spi", "Extension")


   @BeforeAll
    fun prepareFiles() {
       System.out.println("*****INITIALIZING******")
       var tempFile: Path = createTempFile(tempDir,"foo.bar.spi", "Extension")
    }
}*/



@Test
fun gitignoreFound() = rewriteRun(
        { spec ->
            spec.recipe(RenameFile("**/foo.gitignore","new.bar"))
                    .expectedCyclesThatMakeChanges(1)
                    .parser(PlainTextParser())
                    .afterRecipe { results ->
                        results.forEach {
                            assertTrue(it.after?.sourcePath?.contains(Paths.get("new.bar")) == true)
                            assertFalse(it.after?.sourcePath?.contains(Paths.get("foo.gitignore")) == true)
                        }
                    }
        },
        text("hi") {
            spec -> spec.path(Paths.get(  "/foo.gitignore"))
        }
    )

    @Test
    fun gitattributesFound() = rewriteRun(
            { spec ->
                spec.recipe(RenameFile("**/foo.gitattributes","new.bar"))
                        .expectedCyclesThatMakeChanges(1)
                        .parser(PlainTextParser())
                        .afterRecipe { results ->
                            results.forEach {
                                assertTrue(it.after?.sourcePath?.contains(Paths.get("new.bar")) == true)
                                assertFalse(it.after?.sourcePath?.contains(Paths.get("foo.gitattributes")) == true)
                            }
                        }
            },
            text("hi") {
                spec -> spec.path(Paths.get(  "/foo.gitattributes"))
            }
    )

    @Test
    fun javaVersionFound() = rewriteRun(
            { spec ->
                spec.recipe(RenameFile("**/foo.java-version","new.bar"))
                        .expectedCyclesThatMakeChanges(1)
                        .parser(PlainTextParser())
                        .afterRecipe { results ->
                            results.forEach {
                                assertTrue(it.after?.sourcePath?.contains(Paths.get("new.bar")) == true)
                                assertFalse(it.after?.sourcePath?.contains(Paths.get("foo.java-version")) == true)
                            }
                        }
            },
            text("hi") {
                spec -> spec.path(Paths.get(  "/foo.java-version"))
            }
    )

    @Test
    fun sdkmanrcFound() = rewriteRun(
            { spec ->
                spec.recipe(RenameFile("**/foo.sdkmanrc","new.bar"))
                        .expectedCyclesThatMakeChanges(1)
                        .parser(PlainTextParser())
                        .afterRecipe { results ->
                            results.forEach {
                                assertTrue(it.after?.sourcePath?.contains(Paths.get("new.bar")) == true)
                                assertFalse(it.after?.sourcePath?.contains(Paths.get("foo.sdkmanrc")) == true)
                            }
                        }
            },
            text("hi") {
                spec -> spec.path(Paths.get(  "/foo.sdkmanrc"))
            }
    )

    @Test
    fun spiConfigFolderFound() = rewriteRun(
            { spec ->
                spec.recipe(RenameFile("**/foo.bar.spi.Extension","new.bar"))
                        .expectedCyclesThatMakeChanges(1)
                        .parser(PlainTextParser())
                        .afterRecipe { results ->
                            results.forEach {
                                assertTrue(it.after?.sourcePath?.contains(Paths.get("new.bar")) == true)
                                assertFalse(it.after?.sourcePath?.contains(Paths.get("foo.bar.spi.Extension")) == true)
                            }
                        }
            },
            text("hi") {
                spec -> spec.path(Paths.get(  "/META-INF/services/foo.bar.spi.Extension"))
            }
    )

}
