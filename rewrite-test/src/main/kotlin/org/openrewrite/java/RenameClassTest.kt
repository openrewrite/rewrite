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
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.java.tree.TypeUtils
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import java.nio.file.Path

interface RenameClassTest: JavaRecipeTest, RewriteTest {
    override val recipe: Recipe
        get() = RenameClass("a.b.Original", "x.y.Target")

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(recipe)
    }

    @Test
    fun filePathMatchWithNoMatchedClassFqn(@TempDir tempDir: Path) = assertUnchangedBase(
        before = tempDir.resolve("a/b/Original.java").apply {
            toFile().parentFile.mkdirs()
            // language=java
            toFile().writeText("""
                package a.b;
                public class NoMatch {
                }
            """.trimIndent())
        }.toFile()
    )

    @Test
    fun onlyRenameClassMissingPublicModifier(@TempDir tempDir: Path) = assertChanged(
        before = tempDir.resolve("a/b/Original.java").apply {
            toFile().parentFile.mkdirs()
            // language=java
            toFile().writeText("""
                package a.b;
                class Original {
                }
            """.trimIndent())
        }.toFile(),
        after = """
                package x.y;
                class Target {
                }
        """.trimIndent(),
        afterConditions = {
                cu -> assertThat(TypeUtils.isOfClassType(cu.classes[0].type, "x.y.Target")).isTrue
        }
    )

    @Test
    fun onlyRenameClassWithoutMatchedFilePath(@TempDir tempDir: Path) = assertChanged(
        before = tempDir.resolve("a/b/NoMatch.java").apply {
            toFile().parentFile.mkdirs()
            // language=java
            toFile().writeText("""
                package a.b;
                public class Original {
                }
            """.trimIndent())
        }.toFile(),
        after = """
                package x.y;
                public class Target {
                }
        """.trimIndent(),
        afterConditions = {
                cu -> assertThat(TypeUtils.isOfClassType(cu.classes[0].type, "x.y.Target")).isTrue
        }
    )

    @Test
    fun renameClassAndFilePath(@TempDir tempDir: Path) {
        val sources = parser.parse(
            listOf(tempDir.resolve("a/b/Original.java").apply {
                toFile().parentFile.mkdirs()
                // language=java
                toFile().writeText("""
                    package a.b;
                    public class Original {
                    }
                """.trimIndent())
            }),
            tempDir,
            InMemoryExecutionContext()
        )

        val results = recipe.run(sources)

        // similarity index doesn't matter
        // language=diff
        assertThat(results.joinToString("") { it.diff() }).isEqualTo("""
            diff --git a/a/b/Original.java b/x/y/Target.java
            similarity index 0%
            rename from a/b/Original.java
            rename to x/y/Target.java
            index 49dd697..65e689d 100644
            --- a/a/b/Original.java
            +++ b/x/y/Target.java
            @@ -1,3 +1,3 @@ org.openrewrite.java.RenameClass
            -package a.b;
            -public class Original {
            +package x.y;
            +public class Target {
             }
            \ No newline at end of file
        """.trimIndent() + "\n")
    }

    @Test
    fun renamePackageAndFilePath(@TempDir tempDir: Path) {
        val sources = parser.parse(
            listOf(tempDir.resolve("a/b/Original.java").apply {
                toFile().parentFile.mkdirs()
                // language=java
                toFile().writeText("""
                    package a.b;
                    public class Original {
                    }
                """.trimIndent())
            }),
            tempDir,
            InMemoryExecutionContext()
        )

        val results = RenameClass("a.b.Original", "x.y.Original").run(sources)

        // similarity index doesn't matter
        // language=diff
        assertThat(results.joinToString("") { it.diff() }).isEqualTo("""
            diff --git a/a/b/Original.java b/x/y/Original.java
            similarity index 0%
            rename from a/b/Original.java
            rename to x/y/Original.java
            index 49dd697..02e6a06 100644
            --- a/a/b/Original.java
            +++ b/x/y/Original.java
            @@ -1,3 +1,3 @@ org.openrewrite.java.RenameClass
            -package a.b;
            +package x.y;
             public class Original {
             }
            \ No newline at end of file
        """.trimIndent() + "\n")
    }

    @Test
    fun renamePackageAndInnerClassName(@TempDir tempDir: Path) {
        val sources = parser.parse(
            listOf(tempDir.resolve("a/b/C.java").apply {
                toFile().parentFile.mkdirs()
                // language=java
                toFile().writeText("""
                    package a.b;
                    public class C {
                        public static class Original {
                        }
                    }
                """.trimIndent())
            }),
            tempDir,
            InMemoryExecutionContext()
        )

        val results = RenameClass("a.b.C${'$'}Original", "x.y.C${'$'}Target").run(sources)

        // similarity index doesn't matter
        // language=diff
        assertThat(results.joinToString("") { it.diff() }).isEqualTo("""
            diff --git a/a/b/C.java b/x/y/C.java
            similarity index 0%
            rename from a/b/C.java
            rename to x/y/C.java
            index 7b428dc..56a5c53 100644
            --- a/a/b/C.java
            +++ b/x/y/C.java
            @@ -1,5 +1,5 @@ org.openrewrite.java.RenameClass
            -package a.b;
            +package x.y;
             public class C {
            -    public static class Original {
            +    public static class Target {
                 }
             }
            \ No newline at end of file
        """.trimIndent() + "\n")
    }

    @Test
    fun updateImportPrefixWithEmptyPackage(jp: JavaParser) = rewriteRun(
        { spec ->
            spec.parser(jp)
            spec.recipe(
                RenameClass("a.b.Original", "Target")
            )},
        java("""
            package a.b;
            
            import java.util.List;
            
            class Original {
            }
            """,
            """
            import java.util.List;
            
            class Target {
            }
            """
        )
    )

    @Test
    fun updateClassPrefixWithEmptyPackage(jp: JavaParser) = rewriteRun(
        { spec ->
            spec.parser(jp)
            spec.recipe(
                RenameClass("a.b.Original", "Target")
            )
        },
        java("""
            package a.b;
            
            class Original {
            }
            """,
            """
            class Target {
            }
            """
        )
    )

    @Test
    fun renameInnerClass(jp: JavaParser) = rewriteRun(
        { spec ->
            spec.parser(jp)
            spec.recipe(
                RenameClass("a.b.C${'$'}Original", "a.b.C${'$'}Target")
            )
        },
        java("""
            package a.b;
            public class C {
                public static class Original {
                }
            }
            """,
            """
            package a.b;
            public class C {
                public static class Target {
                }
            }
            """
        )
    )

    @Test
    fun multipleLevelsOfInnerClasses(@TempDir tempDir: Path) {
        val sources = parser.parse(
            listOf(tempDir.resolve("a/b/C.java").apply {
                toFile().parentFile.mkdirs()
                // language=java
                toFile().writeText("""
                    package a.b;
                    public class C {
                        public static class D {
                            public static class Original {
                            }
                        }
                    }
                """.trimIndent())
            }),
            tempDir,
            InMemoryExecutionContext()
        )

        val results = RenameClass("a.b.C${'$'}D${'$'}Original", "x.y.C${'$'}D${'$'}Target").run(sources)

        // similarity index doesn't matter
        // language=diff
        assertThat(results.joinToString("") { it.diff() }).isEqualTo("""
            diff --git a/a/b/C.java b/x/y/C.java
            similarity index 0%
            rename from a/b/C.java
            rename to x/y/C.java
            index 22bd92f..816d6b1 100644
            --- a/a/b/C.java
            +++ b/x/y/C.java
            @@ -1,7 +1,7 @@ org.openrewrite.java.RenameClass
            -package a.b;
            +package x.y;
             public class C {
                 public static class D {
            -        public static class Original {
            +        public static class Target {
                     }
                 }
             }
            \ No newline at end of file
        """.trimIndent() + "\n")
    }

    @Test
    fun renamePackage(jp: JavaParser) = rewriteRun(
        { spec ->
            spec.parser(jp)
            spec.recipe(
                RenameClass("a.b.Original", "x.y.Original")
            )
        },
        java("""
            package a.b;
            class Original {
            }
            """,
            """
            package x.y;
            class Original {
            }
            """
        )
    )

    @Test
    fun renameClass(jp: JavaParser) = rewriteRun(
        { spec ->
            spec.parser(jp)
            spec.recipe(
                RenameClass("a.b.Original", "a.b.Target")
            )
        },
        java("""
            package a.b;
            class Original {
            }
            """,
            """
            package a.b;
            class Target {
            }
            """
        )
    )

    @Test
    fun renameClassReference(jp: JavaParser) = rewriteRun(
        { spec -> spec.parser(jp) },
        java("""
            package a.b;
            
            public class Original {}
            """,
        """
            package x.y;
            
            public class Target {}
            """
        ),
        java("""
            import a.b.Original;
            
            class Test {
                Original name;
            }
            """,
            """
            import x.y.Target;
            
            class Test {
                Target name;
            }
            """
        )
    )
}
