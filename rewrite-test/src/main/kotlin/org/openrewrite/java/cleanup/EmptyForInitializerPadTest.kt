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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.style.PadPolicy
import org.openrewrite.style.NamedStyles

interface EmptyForInitializerPadTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = EmptyForInitializerPad()

    @Test
    fun noSpaceInitializerPadding(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                {
                    for (; i < j; i++, j--);
                }
            }
        """
    )

    @Test
    fun spaceInitializerPadding(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.styles(
            listOf(
                NamedStyles(
                    Tree.randomId(), "test", "test", "test", emptySet(),
                    listOf(EmptyForInitializerStyle(PadPolicy.SPACE))
                )
            )
        ).build(),
        before = """
            public class A {
                {
                    for (; i < j; i++, j--);
                }
            }
        """,
        after = """
            public class A {
                {
                    for ( ; i < j; i++, j--);
                }
            }
        """
    )

    @Test
    fun noCheckIfInitializerStartsWithLineTerminator(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class A {
                {
                    for (
                          ; i < j; i++, j--);
                }
            }
        """
    )
}
