/*
 * Copyright 2020 the original author or authors.
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Tree.randomId
import org.openrewrite.internal.lang.Nullable
import org.openrewrite.marker.Markers
import org.openrewrite.text.ChangeText
import org.openrewrite.text.PlainText
import org.openrewrite.text.PlainTextVisitor
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class RecipeLifecycleTest {

    @Test
    fun panic() {
        val visited = AtomicInteger(0)
        val ctx = InMemoryExecutionContext()
        ctx.putMessage(Recipe.PANIC, true)

        object : Recipe() {
            override fun getDisplayName(): String = "Slow"

            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object : TreeVisitor<Tree, ExecutionContext>() {
                    override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                        visited.incrementAndGet()
                        return tree
                    }
                }
            }
        }.run(listOf(PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null,"hello world")), ctx)

        assertThat(visited.get()).isEqualTo(0)
    }

    @Test
    fun notApplicableRecipe() {
        val results = object : Recipe() {
            override fun getName() = "test.NotApplicable"
            override fun getDisplayName(): String {
                return name
            }

            override fun getApplicableTest(): TreeVisitor<*, ExecutionContext>? {
                return NOOP // never going to be applicable
            }

            override fun visit(before: List<SourceFile>, ctx: ExecutionContext) =
                before + PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "test")
        }.run(emptyList()).results

        assertThat(results).isEmpty()
    }

    @Test
    fun generateFile() {
        val results = object : Recipe() {
            override fun getName() = "test.GeneratingRecipe"
            override fun getDisplayName(): String {
                return name
            }

            override fun visit(before: List<SourceFile>, ctx: ExecutionContext) =
                before + PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "test")
        }.run(emptyList()).results

        assertThat(results.map { it.recipeDescriptorsThatMadeChanges.map { r -> r.name }.first() }
            .distinct()).containsExactly("test.GeneratingRecipe")
    }

    @Test
    fun deleteFile() {
        val results = object : Recipe() {
            override fun getName() = "test.DeletingRecipe"

            override fun getDisplayName(): String {
                return name
            }

            override fun visit(before: List<SourceFile>, ctx: ExecutionContext) =
                emptyList<SourceFile>()
        }.run(listOf(PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "test"))).results

        assertThat(results.map {
            it.recipeDescriptorsThatMadeChanges.map { r -> r.name }.first()
        }).containsExactly("test.DeletingRecipe")
    }

    @Test
    fun deleteFileByReturningNullFromVisit() {
        val results = object : Recipe() {
            override fun getName() = "test.DeletingRecipe"

            override fun getDisplayName(): String {
                return name
            }

            override fun getVisitor(): PlainTextVisitor<ExecutionContext> {
                return object : PlainTextVisitor<ExecutionContext>() {
                    override fun visit(tree: Tree?, p: ExecutionContext): PlainText? = null
                }

            }

        }.run(listOf(PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "test"))).results

        assertThat(results.map {
            it.recipeDescriptorsThatMadeChanges.map { r -> r.name }.first()
        }).containsExactly("test.DeletingRecipe")
    }

    @Suppress("USELESS_IS_CHECK")
    class FooVisitor<P> : TreeVisitor<FooSource, P>() {
        override fun preVisit(tree: FooSource, p: P): FooSource {
            if (tree !is FooSource) {
                throw RuntimeException("tree is not a FooSource")
            }
            return tree
        }

        override fun postVisit(tree: FooSource, p: P): FooSource {
            if (tree !is FooSource) {
                throw RuntimeException("tree is not a FooSource")
            }
            return tree
        }
    }

    class FooSource : SourceFile {
        override fun <P : Any?> isAcceptable(v: TreeVisitor<*, P>, p: P) = v.isAdaptableTo(FooVisitor::class.java)

        override fun getMarkers(): Markers = throw NotImplementedError()
        override fun <T : Tree?> withMarkers(markers: Markers): T = throw NotImplementedError()
        override fun getId(): UUID = throw NotImplementedError()
        override fun <T : Tree?> withId(id: UUID): T = throw NotImplementedError()
        override fun getSourcePath() = throw NotImplementedError()
        override fun <T : SourceFile?> withSourcePath(path: Path): T = throw NotImplementedError()
        override fun getCharset() = throw NotImplementedError()
        override fun <T : SourceFile?> withCharset(charset: Charset): T = throw NotImplementedError()
        override fun isCharsetBomMarked() = throw NotImplementedError()
        override fun <T : SourceFile?> withCharsetBomMarked(marked: Boolean): T = throw NotImplementedError()
        override fun getChecksum(): Checksum = throw NotImplementedError()
        override fun <T : SourceFile?> withChecksum(checksum: Checksum?): T = throw NotImplementedError()
        override fun getFileAttributes(): FileAttributes = throw NotImplementedError()
        override fun <T : SourceFile?> withFileAttributes(fileAttributes: FileAttributes?): T = throw NotImplementedError()
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/389")
    @Test
    fun sourceFilesAcceptOnlyApplicableVisitors() {
        val sources = listOf(FooSource(), PlainText(randomId(), Paths.get("test.txt"), Markers.build(listOf()), null, false, null, null, "Hello"))
        val fooVisitor = FooVisitor<ExecutionContext>()
        val textVisitor = PlainTextVisitor<ExecutionContext>()
        val ctx = InMemoryExecutionContext {
            throw it
        }
        sources.forEach {
            fooVisitor.visit(it, ctx)
            textVisitor.visit(it, ctx)
        }
    }

    @Test
    fun accurateReportingOfRecipesMakingChanges() {
        val sources = listOf(PlainText(randomId(), Paths.get("test.txt"), Markers.build(listOf()), null, false, null, null, "Hello"))
        // Set up a composite recipe which prepends "Change1" and appends "Change2" to the input text
        val recipe = object : Recipe() {
            override fun getDisplayName() = "root"
        }.apply {
            doNext(testRecipe("Change1"))
            doNext(noChangeRecipe())
            doNext(testRecipe("Change2"))
        }
        val results = recipe.run(sources, InMemoryExecutionContext { throw it }).results
        assertThat(results.size)
            .isEqualTo(1)
        assertThat(results.first().recipeDescriptorsThatMadeChanges.map { it.name })
            .containsExactlyInAnyOrder("Change1", "Change2")
    }

    @Test
    fun recipeDescriptorsReturnCorrectStructure() {
        val sources = listOf(
            PlainText(randomId(), Paths.get("test.txt"), Markers.build(listOf()), null, false, null, null,"Hello")
        )
        // Set up a composite recipe which with a nested structure of recipes
        val recipe = object : Recipe() {
            override fun getDisplayName() = "Environment.Composite"
            override fun getName() = displayName
            override fun toString() = displayName
        }.apply {
            doNext(testRecipe("A")
                .doNext(testRecipe("B")
                    .doNext(testRecipe("D")
                        .doNext(testRecipe("C"))))
                    .doNext(noChangeRecipe()))
            doNext(testRecipe("A")
                .doNext(testRecipe("B")
                    .doNext(testRecipe("E"))
                    .doNext(ChangeText("E1"))
                    .doNext(ChangeText( "E2"))))
            doNext(testRecipe("E")
                .doNext(testRecipe("F")))
            doNext(noChangeRecipe())
        }
        val results = recipe.run(sources, InMemoryExecutionContext { throw it }).results
        assertThat(results.size).isEqualTo(1)

        val recipeDescriptors = results[0].recipeDescriptorsThatMadeChanges
        assertThat(recipeDescriptors.size).isEqualTo(2)

        val aDescriptor = recipeDescriptors[0]
        val bDescriptor = aDescriptor?.recipeList?.get(0)
        // B (2 test recipes, 2 ChangeText with different options and 1 noChangeRecipe) resulting in 4 changes
        assertThat(bDescriptor?.name).isEqualTo("B")
        assertThat(bDescriptor?.recipeList?.size).isEqualTo(4)
    }

    private fun testRecipe(name: String): Recipe {
        return object : Recipe() {
            override fun getDisplayName() = name
            override fun getName() = displayName
            override fun toString() = displayName
            override fun getVisitor(): PlainTextVisitor<ExecutionContext> {
                return object : PlainTextVisitor<ExecutionContext>() {
                    override fun visit(@Nullable tree: Tree?, p: ExecutionContext): PlainText {
                        var pt = tree as PlainText
                        if (!pt.printAll().contains(displayName)) {
                            pt = pt.withText(displayName + pt.printAll())
                        }
                        return pt
                    }
                }
            }
        }
    }
    private fun noChangeRecipe(): Recipe {
        return object : Recipe() {
            override fun getDisplayName() = "NoChange"
            override fun getName() = displayName
            override fun toString() = displayName
        }
    }
}
