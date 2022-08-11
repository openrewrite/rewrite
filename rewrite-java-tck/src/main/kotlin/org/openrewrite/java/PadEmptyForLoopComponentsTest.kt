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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.cleanup.PadEmptyForLoopComponents
import org.openrewrite.java.format.SpacesVisitor
import org.openrewrite.java.style.EmptyForInitializerPadStyle
import org.openrewrite.java.style.EmptyForIteratorPadStyle
import org.openrewrite.java.style.IntelliJ
import org.openrewrite.style.NamedStyles
import org.openrewrite.style.Style

/**
 * It's important that these tests validate both that PadEmptyForLoopComponents does the right thing and that
 * SpacesVisitor does not undo that change. Since AutoFormat is used frequently if there were a disagreement over
 * this formatting they would fight back and forth until max cycles was reached.
 */
interface PadEmptyForLoopComponentsTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = PadEmptyForLoopComponents()

    fun namedStyles(styles: Collection<Style>) : Iterable<NamedStyles> {
        return listOf(NamedStyles(Tree.randomId(), "Test", "test", "test", emptySet(), styles))
    }

    @Test
    fun addSpaceToEmptyInitializer(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            EmptyForInitializerPadStyle(
                true
            )
        ))).build(),
        before = """
            public class A {
                {
                    int i = 0;
                    int j = 10;
                    for (; i < j; i++, j--) { }
                }
            }
        """,
        after = """
            public class A {
                {
                    int i = 0;
                    int j = 10;
                    for ( ; i < j; i++, j--) { }
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = SpacesVisitor<ExecutionContext>(IntelliJ.spaces(),
                EmptyForInitializerPadStyle(true), null)
                    .visit(cu, InMemoryExecutionContext{})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun removeSpaceFromEmptyInitializer(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            EmptyForInitializerPadStyle(
                false
            )
        ))).build(),
        before = """
            public class A {
                {
                    int i = 0;
                    int j = 10;
                    for ( ; i < j; i++, j--) { }
                }
            }
        """,
        after = """
            public class A {
                {
                    int i = 0;
                    int j = 10;
                    for (; i < j; i++, j--) { }
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = SpacesVisitor<ExecutionContext>(IntelliJ.spaces(),
                EmptyForInitializerPadStyle(false), null)
                    .visit(cu, InMemoryExecutionContext{})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun addSpaceToEmptyIterator(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            EmptyForIteratorPadStyle(
                true
            )
        ))).build(),
        before = """
            public class A {
                {
                    int i = 0;
                    for (int i = 0; i < 10;) { i++; }
                }
            }
        """,
        after = """
            public class A {
                {
                    int i = 0;
                    for (int i = 0; i < 10; ) { i++; }
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = SpacesVisitor<ExecutionContext>(IntelliJ.spaces(), null,
                EmptyForIteratorPadStyle(true)
            )
                    .visit(cu, InMemoryExecutionContext{})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun removeSpaceFromEmptyIterator(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            EmptyForIteratorPadStyle(
                false
            )
        ))).build(),
        before = """
            public class A {
                {
                    int i = 0;
                    for (int i = 0; i < 10; ) { i++; }
                }
            }
        """,
        after = """
            public class A {
                {
                    int i = 0;
                    for (int i = 0; i < 10;) { i++; }
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = SpacesVisitor<ExecutionContext>(IntelliJ.spaces(), null,
                EmptyForIteratorPadStyle(false)
            )
                    .visit(cu, InMemoryExecutionContext{})
            assertThat(nucu).isEqualTo(cu)
        }
    )
}
