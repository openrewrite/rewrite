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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Space

interface LineCounterTest: JavaRecipeTest {

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun countLines() = assertChanged(
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val lineCount = LineCounter()

                override fun visitSpace(space: Space, loc: Space.Location, p: ExecutionContext): Space {
                    lineCount.count(space)
                    return super.visitSpace(space, loc, p)
                }

                override fun preVisit(tree: J, p: ExecutionContext): J? {
                    if (lineCount.line == 3) {
                        return tree.withMarkers(tree.markers.searchResult())
                    }
                    return super.preVisit(tree, p)
                }
            }
        },
        before = """
            class Test {
                void test() {
                    int n = 0;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    /*~~>*/int /*~~>*//*~~>*/n = /*~~>*/0;
                }
            }
        """
    )
}
