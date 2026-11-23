/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin

import org.junit.jupiter.api.Test
import java.util.function.Supplier
import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.marker.SearchResult
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.RewriteTest.toRecipe

class HotPathTest : RewriteTest {

    /** Visitor that marks every method invocation whose cursor satisfies [predicate]. */
    private fun markingVisitor(predicate: (Cursor) -> Boolean) =
        object : KotlinVisitor<ExecutionContext>() {
            override fun visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): J {
                val m = super.visitMethodInvocation(method, ctx) as J.MethodInvocation
                return if (predicate(cursor)) SearchResult.found(m)!! else m
            }
        }

    @Test
    fun `isInsideLoop fires inside for body but not outside`() = rewriteRun(
        { spec: RecipeSpec -> spec.recipe(toRecipe(Supplier<TreeVisitor<*, ExecutionContext>> { markingVisitor { c -> c.isInsideLoop() } }) ) },
        kotlin(
            """
            fun outer() {
                println("before")
                for (i in 1..10) {
                    println("inside")
                }
                println("after")
            }
            """,
            """
            fun outer() {
                println("before")
                for (i in 1..10) {
                    /*~~>*/println("inside")
                }
                println("after")
            }
            """
        )
    )

    @Test
    fun `isInsideLoop fires inside while body`() = rewriteRun(
        { spec: RecipeSpec -> spec.recipe(toRecipe(Supplier<TreeVisitor<*, ExecutionContext>> { markingVisitor { c -> c.isInsideLoop() } }) ) },
        kotlin(
            """
            fun outer(n: Int) {
                var i = 0
                while (i < n) {
                    println(i)
                    i++
                }
            }
            """,
            """
            fun outer(n: Int) {
                var i = 0
                while (i < n) {
                    /*~~>*/println(i)
                    i++
                }
            }
            """
        )
    )

    @Test
    fun `isInsideHotCollectionLambda fires inside forEach`() = rewriteRun(
        { spec: RecipeSpec -> spec.recipe(toRecipe(Supplier<TreeVisitor<*, ExecutionContext>> { markingVisitor { c -> c.isInsideHotCollectionLambda() } }) ) },
        kotlin(
            """
            fun outer() {
                listOf(1, 2, 3).forEach { i ->
                    println(i)
                }
            }
            """,
            """
            fun outer() {
                listOf(1, 2, 3).forEach { i ->
                    /*~~>*/println(i)
                }
            }
            """
        )
    )

    @Test
    fun `isInsideHotCollectionLambda fires inside map`() = rewriteRun(
        { spec: RecipeSpec -> spec.recipe(toRecipe(Supplier<TreeVisitor<*, ExecutionContext>> { markingVisitor { c -> c.isInsideHotCollectionLambda() } }) ) },
        kotlin(
            """
            fun outer(): List<String> {
                return listOf(1, 2, 3).map { i ->
                    i.toString()
                }
            }
            """,
            """
            fun outer(): List<String> {
                return listOf(1, 2, 3).map { i ->
                    /*~~>*/i.toString()
                }
            }
            """
        )
    )

    @Test
    fun `isInsideComposable fires inside annotated function`() = rewriteRun(
        { spec: RecipeSpec -> spec.recipe(toRecipe(Supplier<TreeVisitor<*, ExecutionContext>> { markingVisitor { c -> c.isInsideComposable() } }) ) },
        kotlin(
            """
            annotation class Composable

            @Composable
            fun Greeting() {
                println("hello")
            }

            fun nonComposable() {
                println("plain")
            }
            """,
            """
            annotation class Composable

            @Composable
            fun Greeting() {
                /*~~>*/println("hello")
            }

            fun nonComposable() {
                println("plain")
            }
            """
        )
    )

    @Test
    fun `isInsideHotPathAnnotated fires inside opt-in marker`() = rewriteRun(
        { spec: RecipeSpec -> spec.recipe(toRecipe(Supplier<TreeVisitor<*, ExecutionContext>> { markingVisitor { c -> c.isInsideHotPathAnnotated() } }) ) },
        kotlin(
            """
            annotation class HotPath

            @HotPath
            fun work() {
                println("hot")
            }

            fun cold() {
                println("cold")
            }
            """,
            """
            annotation class HotPath

            @HotPath
            fun work() {
                /*~~>*/println("hot")
            }

            fun cold() {
                println("cold")
            }
            """
        )
    )

    @Test
    fun `isInsideHotPath composite fires in any of the recognized positions`() = rewriteRun(
        { spec: RecipeSpec -> spec.recipe(toRecipe(Supplier<TreeVisitor<*, ExecutionContext>> { markingVisitor { c -> c.isInsideHotPath() } }) ) },
        kotlin(
            """
            annotation class Composable

            fun coldPath() {
                println("cold")
            }

            @Composable
            fun hotComposable() {
                println("composable")
            }

            fun hotLoop() {
                for (i in 1..5) {
                    println(i)
                }
            }

            fun hotLambda() {
                listOf(1, 2, 3).forEach { println(it) }
            }
            """,
            """
            annotation class Composable

            fun coldPath() {
                println("cold")
            }

            @Composable
            fun hotComposable() {
                /*~~>*/println("composable")
            }

            fun hotLoop() {
                for (i in 1..5) {
                    /*~~>*/println(i)
                }
            }

            fun hotLambda() {
                listOf(1, 2, 3).forEach { /*~~>*/println(it) }
            }
            """
        )
    )
}
