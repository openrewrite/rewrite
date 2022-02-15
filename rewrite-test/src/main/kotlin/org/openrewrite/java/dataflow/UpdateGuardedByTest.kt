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
package org.openrewrite.java.dataflow

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.marker.Marker
import org.openrewrite.marker.SearchResult
import java.util.*

interface UpdateGuardedByTest : JavaRecipeTest {

    @Suppress("UNCHECKED_CAST")
    override val recipe: Recipe
        get() = UpdateGuardedBy().doNext(toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val declarations = mutableMapOf<UUID, String>()

                override fun <M : Marker?> visitMarker(marker: Marker, p: ExecutionContext): M {
                    return if (marker is GuardedBy) {
                        SearchResult(
                            marker.id, "guarded by '${
                                declarations.getOrPut(marker.guard) {
                                    var guard = ""
                                    object : JavaVisitor<Int>() {
                                        override fun visit(tree: Tree?, p: Int): J? {
                                            if (tree?.id == marker.guard) {
                                                guard = tree!!.print(cursor)
                                            }
                                            return super.visit(tree, p)
                                        }
                                    }.visit(cursor.firstEnclosing(J.CompilationUnit::class.java), 0)
                                    guard
                                }
                            }'"
                        ) as M
                    } else marker as M
                }
            }
        })

    @Suppress("ConstantConditions")
    @Test
    fun asDescribedInLearningToRepresentProgramsWithGraphs() = assertChanged(
        before = """
            class Test {
                void test() {
                    int x = 0, y = 0;
                    while(x > 3) {
                        x = x + y;
                    }
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    int x = 0, y = 0;
                    while(x > 3) {
                        /*~~(guarded by 'x > 3')~~>*/x = x + y;
                    }
                }
            }
        """,
        expectedCyclesThatMakeChanges = 1,
        cycles = 1
    )
}
