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
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.TypeValidation
import org.openrewrite.java.tree.J
import org.openrewrite.test.RewriteTest

@Issue("https://github.com/openrewrite/rewrite/issues/466")
interface MethodNameCasingTest: JavaRecipeTest, RewriteTest {
    override val recipe: Recipe?
        get() = MethodNameCasing(false)

    override val parser: JavaParser
        get() {
            val jp = JavaParser.fromJavaVersion().build()
            jp.setSourceSet("main")
            return jp
        }

    val testParser: JavaParser
        get() {
            val jp = JavaParser.fromJavaVersion().build()
            jp.setSourceSet("test")
            return jp
        }

    @Issue("https://github.com/openrewrite/rewrite/issues/1741")
    @Test
    fun doNotApplyToTest() = assertUnchanged(
        testParser,
        before = """
            class Test {
                void MyMethod_with_über() {
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1741")
    @Test
    fun applyChangeToTest() = assertChanged(
        testParser,
        recipe = MethodNameCasing(true),
        before = """
            class Test {
                void MyMethod_with_über() {
                }
            }
        """,
        after = """
            class Test {
                void myMethodWithBer() {
                }
            }
        """
    )

    @Test
    fun changeMethodDeclaration() = assertChanged(
        
        before = """
            class Test {
                void MyMethod_with_über() {
                }
            }
        """,
        after = """
            class Test {
                void myMethodWithBer() {
                }
            }
        """
    )

    @Test
    fun changeMethodInvocations() = assertChanged(
        dependsOn = arrayOf("""
            class Test {
                void MyMethod_with_über() {
                }
            }
        """),
        before = """
            class A {
                void test() {
                    new Test().MyMethod_with_über();
                }
            }
        """,
        after = """
            class A {
                void test() {
                    new Test().myMethodWithBer();
                }
            }
        """
    )

    @Test
    fun dontChangeCorrectlyCasedMethods() = assertUnchanged(
        before = """
            class Test {
                void dontChange() {
                }
            }
        """
    )

    @Test
    fun changeMethodNameWhenOverride() = assertChanged(
        dependsOn = arrayOf(
            """
            class ParentClass {
                void _method() {
                }
            }
        """
        ),
        before = """
            class Test extends ParentClass {
                @Override
                void _method() {
                }
            }
        """,
        after = """
            class Test extends ParentClass {
                @Override
                void method() {
                }
            }
        """
    )

    // This test uses a recipe remove ClassDeclaration types information prior to running the MethodNameCasing recipe.
    // This results in a change with an empty diff, thus before and after sources are identical
    @Issue("https://github.com/openrewrite/rewrite/issues/2103")
    @Test
    fun `does not rename method invocations when the method declarations class type is null`() = rewriteRun(
        { spec ->
            spec.typeValidationOptions(TypeValidation.none()).recipe(
                RewriteTest.toRecipe {
                    object : JavaIsoVisitor<ExecutionContext>() {
                        override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                            return super.visitClassDeclaration(classDecl, p).withType(null)
                        }
                    }
                }.doNext(MethodNameCasing(true))
            )
        },
        java(
            """
            package abc;
            class T {
                public static int MyMethod() {return null;}
                public static void anotherMethod() {
                    int i = MyMethod();
                }
            }
            """,
            """
            package abc;
            class T {
                public static int MyMethod() {return null;}
                public static void anotherMethod() {
                    int i = MyMethod();
                }
            }
            """
        )
    )
}
