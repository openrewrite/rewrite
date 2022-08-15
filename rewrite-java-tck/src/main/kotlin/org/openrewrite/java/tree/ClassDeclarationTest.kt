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
package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.JavaTreeTest.NestingLevel.CompilationUnit

interface ClassDeclarationTest : JavaTreeTest {

    @Issue("#70")
    @Test
    fun singleLineCommentBeforeModifier(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            @Deprecated
            // Some comment
            public final class A {}
        """
    )

    @Test
    fun multipleClassDeclarationsInOneCompilationUnit(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public class A {}
            class B {}
        """
    )

    @Test
    fun implements(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public interface B {}
            class A implements B {}
        """
    )

    @Test
    fun extends(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public interface B {}
            class A extends B {}
        """
    )

    @Test
    fun typeArgumentsAndAnnotation(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public class B<T> {}
            @Deprecated public class A < T > extends B < T > {}
        """
    )

    /**
     * OpenJDK does NOT preserve the order of modifiers in its AST representation
     */
    @Test
    fun modifierOrdering(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public /* abstract */ final abstract class A {}
        """
    )

    @Test
    fun innerClass(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public class A {
                public enum B {
                    ONE,
                    TWO
                }
            
                private B b;
            }
        """
    )

    @Test
    fun strictfp(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public strictfp class A {}
        """
    )

    @Test
    fun hasModifier(jp: JavaParser) {
        val a = jp.parse(
            """
            public strictfp class A {}
        """
        )[0]

        val inv = a.classes[0]
        assertThat(inv.modifiers).hasSize(2)
        assertTrue(inv.hasModifier(J.Modifier.Type.Public))
        assertTrue(inv.hasModifier(J.Modifier.Type.Strictfp))
    }

}
