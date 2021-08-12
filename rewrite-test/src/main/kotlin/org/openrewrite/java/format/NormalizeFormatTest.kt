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
package org.openrewrite.java.format

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.tree.J

interface NormalizeFormatTest : JavaRecipeTest {
    private val removeAnnotation: Recipe
        get() = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitAnnotation(annotation: J.Annotation, p: ExecutionContext): J.Annotation? = null
            }
        }

    @Test
    fun removeAnnotationFromMethod(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeAnnotation
            .doNext(NormalizeFormat())
            .doNext(RemoveTrailingWhitespace())
            .doNext(TabsAndIndents()),
        before = """
            class Test {
                @Deprecated
                public void method(Test t) {
                }
            }
        """,
        after = """
            class Test {
            
                public void method(Test t) {
                }
            }
        """
    )

    @Test
    fun removeAnnotationFromClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeAnnotation
            .doNext(NormalizeFormat())
            .doNext(RemoveTrailingWhitespace())
            .doNext(TabsAndIndents()),
        before = """
            class Test {
                @Deprecated
                class A {
                }
            }
        """,
        after = """
            class Test {
            
                class A {
                }
            }
        """
    )

    @Test
    fun removeAnnotationFromVariable(jp: JavaParser) = assertChanged(
        jp,
        recipe = removeAnnotation
            .doNext(NormalizeFormat())
            .doNext(RemoveTrailingWhitespace())
            .doNext(TabsAndIndents()),
        before = """
            class Test {
                @Deprecated
                public String s;
            }
        """,
        after = """
            class Test {
            
                public String s;
            }
        """
    )
}
