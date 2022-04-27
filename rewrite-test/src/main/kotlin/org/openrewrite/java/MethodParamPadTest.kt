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
import org.openrewrite.java.cleanup.MethodParamPad
import org.openrewrite.java.format.AutoFormatVisitor
import org.openrewrite.java.style.MethodParamPadStyle
import org.openrewrite.style.NamedStyles
import org.openrewrite.style.Style

@Suppress("InfiniteRecursion")
interface MethodParamPadTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = MethodParamPad()

    fun namedStyles(styles: Collection<Style>): Iterable<NamedStyles> {
        return listOf(NamedStyles(Tree.randomId(), "Test", "test", "test", emptySet(), styles))
    }

    @Test
    fun addSpacePadding(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            MethodParamPadStyle(
                true,
                false
            )
        ))).build(),
        before = """
            enum E {
                E1()
            }

            class B {
            }

            class A extends B {
                A() {
                    super();
                }

                static void method(int x, int y) {
                    A a = new A();
                    method(0, 1);
                }
            }
        """,
        after = """
            enum E {
                E1 ()
            }

            class B {
            }

            class A extends B {
                A () {
                    super ();
                }

                static void method (int x, int y) {
                    A a = new A ();
                    method (0, 1);
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun removeSpacePadding(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            MethodParamPadStyle(
                false,
                false
            )
        ))).build(),
        before = """
            enum E {
                E1 ()
            }

            class B {
            }

            class A extends B {
                A () {
                    super ();
                }

                static void method (int x, int y) {
                    A a = new A ();
                    method (0, 1);
                }
            }
        """,
        after = """
            enum E {
                E1()
            }

            class B {
            }

            class A extends B {
                A() {
                    super();
                }

                static void method(int x, int y) {
                    A a = new A();
                    method(0, 1);
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun allowLineBreaks(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(namedStyles(listOf(
            MethodParamPadStyle(
                true,
                true
            )
        ))).build(),
        before = """
            enum E {
                E1
                        ()
            }
            
            class B {
            }
            
            class A extends B {
                A
                        () {
                    super
                            ();
                }
            
                static void method
                        (int x, int y) {
                    A a = new A
                            ();
                    method
                            (0, 1);
                }
            }
        """
    )

    @Test
    fun removeLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            MethodParamPadStyle(
                false,
                false
            )
        ))).build(),
        before = """
            enum E {
                E1
                        ()
            }
            
            class B {
            }
            
            class A extends B {
                A
                        () {
                    super
                            ();
                }
            
                static void method
                        (int x, int y) {
                    A a = new A
                            ();
                    method
                            (0, 1);
                }
            }
        """,
        after = """
            enum E {
                E1()
            }

            class B {
            }

            class A extends B {
                A() {
                    super();
                }

                static void method(int x, int y) {
                    A a = new A();
                    method(0, 1);
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun removeLineBreaksAndAddSpaces(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(namedStyles(listOf(
            MethodParamPadStyle(
                true,
                false
            )
        ))).build(),
        before = """
            enum E {
                E1
                        ()
            }

            class B {
            }

            class A extends B {
                A
                        () {
                    super
                            ();
                }

                static void method
                        (int x, int y) {
                    A a = new A
                            ();
                    method
                            (0, 1);
                }
            }
        """,
        after = """
            enum E {
                E1 ()
            }

            class B {
            }

            class A extends B {
                A () {
                    super ();
                }

                static void method (int x, int y) {
                    A a = new A ();
                    method (0, 1);
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun initializeStyleWhenOtherwiseNotProvided(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        before = """
            enum E {
                E1()
            }
        """
    )

}
