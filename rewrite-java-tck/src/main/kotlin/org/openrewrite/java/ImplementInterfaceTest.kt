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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType.ShallowClass
import org.openrewrite.java.tree.Space
import org.openrewrite.marker.Markers
import java.util.*

interface ImplementInterfaceTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = object : TestRecipe() {
            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object : JavaVisitor<ExecutionContext>() {
                    override fun visitClassDeclaration(classDecl: J.ClassDeclaration, ctx: ExecutionContext): J {
                        doAfterVisit(ImplementInterface(classDecl, "b.B"))
                        return classDecl
                    }
                }
            }
        }

    val recipeTyped: Recipe
        get() = object : TestRecipe() {
            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object : JavaVisitor<ExecutionContext>() {
                    override fun visitClassDeclaration(classDecl: J.ClassDeclaration, ctx: ExecutionContext): J {
                        doAfterVisit(
                            ImplementInterface(
                                classDecl,
                                "b.B",
                                listOf(
                                    J.Identifier(
                                        UUID.randomUUID(),
                                        Space.EMPTY,
                                        Markers.EMPTY,
                                        "String",
                                        ShallowClass.build("java.lang.String"),
                                        null
                                    ),
                                    J.Identifier(
                                        UUID.randomUUID(),
                                        Space.build(" ", emptyList()),
                                        Markers.EMPTY,
                                        "LocalDate",
                                        ShallowClass.build("java.time.LocalDate"),
                                        null
                                    )
                                )
                            )
                        )
                        return classDecl
                    }
                }
            }
        }

    companion object {
        private const val b = "package b;\npublic interface B {}"
        private const val c = "package c;\npublic interface C {}"
    }

    @Test
    fun firstImplementedInterface(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(b),
        before = """
            class A {
            }
        """,
        after = """
            import b.B;
            
            class A implements B {
            }
        """
    )

    @Test
    fun addAnImplementedInterface(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(b, c),
        before = """
            import c.C;
            
            class A implements C {
            }
        """,
        after = """
            import b.B;
            import c.C;
            
            class A implements C, B {
            }
        """
    )

    @Test
    fun addAnImplementedInterfaceWithTypeParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = recipeTyped,
        dependsOn = arrayOf(b, c),
        before = """
            import c.C;
            
            class A implements C {
            }
        """,
        after = """
            import b.B;
            import c.C;
            
            import java.time.LocalDate;
            
            class A implements C, B<String, LocalDate> {
            }
        """
    )
}
