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
package org.openrewrite.java.search

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface FindEmptyMethodsTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = FindEmptyMethods(false)

    @Test
    fun methodNotEmpty() = assertUnchanged(
        before = """
            class Test {
                void method() {
                    int x = 0;
                }
            }
        """
    )

    @Test
    fun abstractClass() = assertUnchanged(
        before = """
            abstract class Test {
                void method() {
                }
            }
        """
    )

    @Test
    fun containsComment() = assertUnchanged(
        before = """
            class Test {
                void method() {
                    // comment
                }
            }
        """
    )

    @Test
    fun doNotMatchOverride() = assertUnchanged(
        before = """
            import java.util.Collection;
            
            class Test implements Collection<String> {
                @Override
                public boolean isEmpty() {
                }
            }
        """
    )

    @Test
    fun matchOverride() = assertChanged(
        recipe = FindEmptyMethods(true),
        before = """
            import java.util.Collection;
            
            class Test implements Collection<String> {
                @Override
                public boolean isEmpty() {
                }
            }
        """,
        after = """
            import java.util.Collection;
            
            class Test implements Collection<String> {
                /*~~>*/@Override
                public boolean isEmpty() {
                }
            }
        """
    )

    @Test
    fun singleNoArgConstructor() = assertChanged(
        before = """
            class Test {
                public Test() {
                }
            }
        """,
        after = """
            class Test {
                /*~~>*/public Test() {
                }
            }
        """
    )

    @Test
    fun emptyMethod() = assertChanged(
        before = """
            class Test {
                void method() {
                }
            }
        """,
        after = """
            class Test {
                /*~~>*/void method() {
                }
            }
        """
    )
}
