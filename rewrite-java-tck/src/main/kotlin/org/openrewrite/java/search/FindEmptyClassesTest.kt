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
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface FindEmptyClassesTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion().build()

    override val recipe: Recipe
        get() = FindEmptyClasses()

    @Test
    fun classNotEmpty() = assertUnchanged(
        before = """
            class IsNotEmpty {
                int x = 0;
            }
        """
    )

    @Test
    fun emptyInterface() = assertUnchanged(
        before = """
            interface IsEmpty {
            }
        """
    )

    @Test
    fun emptyEnum() = assertUnchanged(
        before = """
            enum IsEmpty {
            }
        """
    )

    @Test
    fun emptyClassWithAnnotation() = assertUnchanged(
        before = """
            @Deprecated
            class IsEmpty {
            }
        """
    )

    @Test
    fun emptyClassWithExtends() = assertUnchanged(
        dependsOn = arrayOf("""class A {}"""),
        before = """
            class IsEmpty extends A {
            }
        """
    )

    @Test
    fun emptyClassWithImplements() = assertUnchanged(
        dependsOn = arrayOf("""interface A {}"""),
        before = """
            class IsEmpty implements A {
            }
        """
    )

    @Test
    fun findEmptyClass() = assertChanged(
        before = """
            class IsEmpty {
            }
        """,
        after = """
            /*~~>*/class IsEmpty {
            }
        """
    )

}
