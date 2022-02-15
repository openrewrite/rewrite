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
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.marker.Marker
import org.openrewrite.marker.SearchResult
import java.util.*

interface UpdateLastReadTest : JavaRecipeTest {
    @Suppress("UNCHECKED_CAST")
    override val recipe: Recipe
        get() = UpdateLastRead().doNext(toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                var n = 0
                val declarations = mutableMapOf<UUID, Int>()

                override fun <M : Marker?> visitMarker(marker: Marker, p: ExecutionContext): M {
                    return if (marker is LastRead) {
                        SearchResult(marker.id, "read of ${declarations.getOrPut(marker.declaration) { ++n }}") as M
                    }
                    else marker as M
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
                    while(/*~~(read of 1)~~>*/x > 3) {
                        x = /*~~(read of 1)~~>*/x + /*~~(read of 2)~~>*/y;
                    }
                }
            }
        """,
        expectedCyclesThatMakeChanges = 1,
        cycles = 1
    )

    @Suppress("ConstantConditions", "UnusedAssignment")
    @Test
    fun localVariablesWithShadowing() = assertChanged(
        before = """
            class Test {
                void test() {
                    int n = 0;
                    if((n = 1) == 1) {
                        n = 2;
                        int n;
                        n = 3;
                        n += 1;
                        n++;
                    }
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    int n = 0;
                    if((/*~~(read of 1)~~>*/n = 1) == 1) {
                        n = 2;
                        int n;
                        n = 3;
                        /*~~(read of 2)~~>*/n += 1;
                        /*~~(read of 2)~~>*/n++;
                    }
                }
            }
        """,
        expectedCyclesThatMakeChanges = 1,
        cycles = 1
    )

    @Suppress("ConstantConditions", "UnusedAssignment")
    @Test
    fun fields() = assertChanged(
        before = """
            class Test {
                int n;
                void test() {
                    if((n = 1) == 1) {
                        n = 2;
                    }
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    if((/*~~(read of 1)~~>*/n = 1) == 1) {
                        n = 2;
                    }
                }
            }
        """,
        expectedCyclesThatMakeChanges = 1,
        cycles = 1
    )
}
