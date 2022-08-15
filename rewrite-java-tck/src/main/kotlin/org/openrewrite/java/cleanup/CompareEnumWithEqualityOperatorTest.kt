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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface CompareEnumWithEqualityOperatorTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = CompareEnumsWithEqualityOperator()

    companion object {
        private val enum = """
            package a;
            public enum A {
                FOO, BAR, BUZ
            }
        """.trimIndent()
    }

    @Suppress("StatementWithEmptyBody")
    @Test
    fun changeEnumEquals() = assertChanged(
        dependsOn = arrayOf(enum),
        before = """
            import a.A;
            class Test {
                void method(A arg0) {
                    if (A.FOO.equals(arg0)) {
                    }
                    if (arg0.equals(A.FOO)) {
                    }
                }
            }
        """,
        after = """
            import a.A;
            class Test {
                void method(A arg0) {
                    if (A.FOO == arg0) {
                    }
                    if (arg0 == A.FOO) {
                    }
                }
            }
        """
    )

    @Suppress("StatementWithEmptyBody")
    @Test
    fun changeEnumNotEquals() = assertChanged(
        dependsOn = arrayOf(enum),
        before = """
            import a.A;
            class Test {
                void method(A arg0) {
                    if (!A.FOO.equals(arg0)) {
                    }
                    if (!arg0.equals(A.FOO)) {
                    }
                }
            }
        """,
        after = """
            import a.A;
            class Test {
                void method(A arg0) {
                    if (A.FOO != arg0) {
                    }
                    if (arg0 != A.FOO) {
                    }
                }
            }
        """
    )
}
