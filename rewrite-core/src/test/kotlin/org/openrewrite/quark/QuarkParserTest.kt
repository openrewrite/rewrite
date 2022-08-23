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
package org.openrewrite.quark

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.ExecutionContext
import org.openrewrite.SourceFile
import org.openrewrite.TreeVisitor
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.RewriteTest.toRecipe
import org.openrewrite.test.SourceSpecs
import org.openrewrite.test.SourceSpecs.other
import org.openrewrite.test.SourceSpecs.text
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.io.path.writeText

class QuarkParserTest : RewriteTest {

    @Test
    fun allOthers() = rewriteRun(
        { spec ->
            spec.beforeRecipe { sources ->
                val quarks = QuarkParser.parseAllOtherFiles(Paths.get("../"), sources)
                assertThat(quarks).isNotEmpty
                assertThat(quarks.map { it.sourcePath }).doesNotContain(Paths.get("build.gradle.kts"))
            }
        },
        text("hi") { spec -> spec.path(Paths.get("build.gradle.kts")) },
    )

    @Test
    fun oneQuark() = rewriteRun(
        { spec ->
            spec.beforeRecipe { sources ->
                assertThat(sources.map { it::class.java }).containsOnlyOnce(Quark::class.java)
            }
        },
        text("hi"),
        other("jon")
    )

    @Test
    fun renameQuark(@TempDir tempDir: Path) = rewriteRun(
        { spec ->
            spec.expectedCyclesThatMakeChanges(1)
                .recipe(toRecipe {
                    object : TreeVisitor<SourceFile, ExecutionContext>() {
                        override fun visitSourceFile(sourceFile: SourceFile, p: ExecutionContext): SourceFile {
                            return if (sourceFile.sourcePath.toString().endsWith(".bak")) {
                                sourceFile
                            } else sourceFile.withSourcePath(Paths.get(sourceFile.sourcePath.toString() + ".bak"))
                        }
                    }
                })
                .afterRecipe { run ->
                    run.results.forEach {
                        val git = Git.init().setDirectory(tempDir.toFile()).call()
                        git.apply().setPatch(it.diff().byteInputStream()).call()
                    }
                    assertThat(tempDir.toFile().list())
                        .containsExactlyInAnyOrder("hi.txt.bak", "jon.bak", ".git")
                    assertThat(tempDir.resolve("jon.bak").readText().trim()).isEqualTo("jon")
                }
        },
        text("hi") { spec ->
            spec.path("hi.txt")
                .beforeRecipe { s -> tempDir.resolve(s.sourcePath).writeText("hi") }
                .afterRecipe { s -> assertThat(s.sourcePath).isEqualTo(Paths.get("hi.txt.bak")) }
        },
        other("jon") { spec ->
            spec.path("jon")
                .beforeRecipe { s -> tempDir.resolve(s.sourcePath).writeText("jon") }
                .afterRecipe { s -> assertThat(s.sourcePath).isEqualTo(Paths.get("jon.bak")) }
        }
    )
}
