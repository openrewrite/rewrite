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
import org.junit.jupiter.api.Test
import org.openrewrite.Tree.randomId
import org.openrewrite.marker.Markers
import org.openrewrite.text.PlainText
import org.openrewrite.text.PlainTextVisitor
import java.util.*

class RecipeLifecycleTest {
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
                before + PlainText(randomId(), Markers.EMPTY, "test")
        }.run(emptyList())

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
                before + PlainText(randomId(), Markers.EMPTY, "test")
        }.run(emptyList())

        assertThat(results.map { it.recipesThatMadeChanges.map { r -> r.name }.first() }.distinct()).containsExactly("test.GeneratingRecipe")
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
        }.run(listOf(PlainText(randomId(), Markers.EMPTY, "test")))

        assertThat(results.map { it.recipesThatMadeChanges.map { r -> r.name }.first() }).containsExactly("test.DeletingRecipe")
    }

    @Test
    fun deleteFileByReturningNullFromVisit() {
        val results = object : Recipe() {
            override fun getName() = "test.DeletingRecipe"

            override fun getDisplayName(): String {
                return name
            }

            override fun getVisitor(): PlainTextVisitor<ExecutionContext> {
                return object: PlainTextVisitor<ExecutionContext>() {
                    override fun visit(tree: Tree?, p: ExecutionContext): PlainText? = null
                }

            }

        }.run(listOf(PlainText(randomId(), Markers.EMPTY, "test")))

        assertThat(results.map { it.recipesThatMadeChanges.map { r -> r.name }.first() }).containsExactly("test.DeletingRecipe")
    }

    @Suppress("USELESS_IS_CHECK")
    class FooVisitor<P> : TreeVisitor<FooSource, P>() {

        override fun preVisit(tree: FooSource, p: P): FooSource {
            if(tree !is FooSource) {
                throw RuntimeException("tree is not a FooSource")
            }
            return tree;
        }

        override fun postVisit(tree: FooSource, p: P): FooSource {
            if(tree !is FooSource) {
                throw RuntimeException("tree is not a FooSource")
            }
            return tree;
        }
    }

    class FooSource : SourceFile {
        override fun <P : Any?> isAcceptable(v: TreeVisitor<*, P>, p: P) = v is FooVisitor

        override fun getMarkers(): Markers = throw NotImplementedError()
        override fun <T : SourceFile?> withMarkers(markers: Markers): T = throw NotImplementedError()
        override fun getId(): UUID = throw NotImplementedError()
        override fun <P : Any?> print(printer: TreePrinter<P>, p: P) = throw NotImplementedError()
        override fun getSourcePath() = throw NotImplementedError()
    }

    // https://github.com/openrewrite/rewrite/issues/389
    @Test
    fun sourceFilesAcceptOnlyApplicableVisitors() {
        val sources = listOf(FooSource(), PlainText(randomId(), Markers.build(listOf()), "Hello"))
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
}
