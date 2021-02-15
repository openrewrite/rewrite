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
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J

interface AddImportTest : JavaRecipeTest {
    fun addImports(vararg adds: AddImport<ExecutionContext>): Recipe = adds
        .map { add -> add.toRecipe() }
        .reduce { r1, r2 -> return r1.doNext(r2) }

    @Test
    fun addMultipleImports(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            AddImport("java.util.List", null, false),
            AddImport("java.util.Set", null, false)
        ),
        before = """
            class A {}
        """,
        after = """
            import java.util.List;
            import java.util.Set;

            class A {}
        """
    )

    @Test
    fun addNamedImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            AddImport("java.util.List", null, false)
        ),
        before = "class A {}",
        after = """
            import java.util.List;
            
            class A {}
        """
    )

    @Test
    fun doNotAddImportIfNotReferenced(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            AddImport("java.util.List", null, true)
        ),
        before = """
            package a;
            
            class A {}
        """
    )

    @Test
    fun doNotAddWildcardImportIfNotReferenced(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            AddImport("java.util.*", null, true)
        ),
        before = """
            package a;
            
            class A {}
        """
    )

    @Test
    fun lastImportWhenFirstClassDeclarationHasJavadoc(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            AddImport("java.util.Collections", "*", false)
        ),
        before = """
            import java.util.List;
            
            /**
             * My type
             */
            class A {}
        """,
        after = """
            import java.util.List;
            
            import static java.util.Collections.*;
            
            /**
             * My type
             */
            class A {}
        """
    )

    @Test
    fun namedImportAddedAfterPackageDeclaration(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            AddImport("java.util.List", null, false)
        ),
        before = """
            package a;
            class A {}
        """,
        after = """
            package a;
            
            import java.util.List;
            
            class A {}
        """
    )

    @Test
    fun importsAddedInAlphabeticalOrder(jp: JavaParser) {
        val otherPackages = listOf("c", "c.c", "c.c.c")
        val otherImports = otherPackages.mapIndexed { i, pkg ->
            "package $pkg;\npublic class C$i {}"
        }

        listOf("b" to 0, "c.b" to 1, "c.c.b" to 2).forEach {
            val (pkg, order) = it

            val expectedImports = otherPackages.mapIndexed { i, otherPkg -> "$otherPkg.C$i" }.toMutableList()
            expectedImports.add(order, "$pkg.B")

            assertChanged(
                jp,
                dependsOn = arrayOf(
                    *otherImports.toTypedArray(),
                    """
                            package $pkg;
                            public class B {}
                        """
                ),
                recipe = addImports(
                    AddImport("$pkg.B", null, false)
                ),
                before = """
                    package a;
        
                    import c.C0;
                    import c.c.C1;
                    import c.c.c.C2;
        
                    class A {}
                """,
                after = "package a;\n\n${expectedImports.joinToString("\n") { fqn -> "import $fqn;" }}\n\nclass A {}"
            )

            jp.reset()
        }
    }

    @Test
    fun doNotAddImportIfAlreadyExists(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            AddImport("java.util.List", null, false)
        ),
        before = """
            package a;
            
            import java.util.List;
            class A {}
        """
    )

    @Test
    fun doNotAddImportIfCoveredByStarImport(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            AddImport("java.util.List", null, false)
        ),
        before = """
            package a;
            
            import java.util.*;
            class A {}
        """
    )

    @Test
    fun dontAddImportWhenClassHasNoPackage(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            AddImport("C", null, false)
        ),
        before = "class A {}"
    )

    @Test
    fun dontAddImportForPrimitive(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            AddImport("int", null, false)
        ),
        before = "class A {}"
    )

    @Test
    fun addNamedImportIfStarStaticImportExists(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            AddImport("java.util.List", null, false)
        ),
        before = """
            package a;
            
            import static java.util.List.*;
            class A {}
        """,
        after = """
            package a;
            
            import java.util.List;
            
            import static java.util.List.*;
            
            class A {}
        """
    )

    @Test
    fun addNamedStaticImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            AddImport("java.util.Collections", "emptyList", false)
        ),
        before = """
            import java.util.*;
            class A {}
        """,
        after = """
            import java.util.*;
            
            import static java.util.Collections.emptyList;
            
            class A {}
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/108")
    @Test
    fun addStaticImportField(jp: JavaParser) = assertChanged(
        jp,
        recipe = addImports(
            AddImport("mycompany.Type", "FIELD", false)
        ),
        dependsOn = arrayOf(
            """
                package mycompany;
                public class Type {
                    public static String FIELD;
                }
            """
        ),
        before = "class A {}",
        after = """
            import static mycompany.Type.FIELD;
            
            class A {}
        """
    )

    @Test
    fun dontAddStaticWildcardImportIfNotReferenced(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            AddImport("java.util.Collections", "*", true)
        ),
        before = """
            package a;
            
            class A {}
        """
    )

    @Test
    fun addNamedStaticImportWhenReferenced(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : Recipe() {
            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object : JavaIsoVisitor<ExecutionContext>() {
                    override fun visitMethodInvocation(m: J.MethodInvocation, ctx: ExecutionContext) =
                        m.withSelect(null)
                }
            }

        }.doNext(
            addImports(AddImport("java.util.Collections", "emptyList", true))
        ),
        before = """
            package a;
            
            import java.util.List;
            
            class A {
                public A() {
                    List<String> list = java.util.Collections.emptyList();
                }
            }
        """,
        after = """
            package a;
            
            import java.util.List;
            
            import static java.util.Collections.emptyList;
            
            class A {
                public A() {
                    List<String> list = emptyList();
                }
            }
        """
    )

    @Test
    fun doNotAddNamedStaticImportIfNotReferenced(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = addImports(
            AddImport("java.util.Collections", "emptyList", true)
        ),
        before = """
            package a;
            
            class A {}
        """
    )

    @Test
    fun addStaticWildcardImportWhenReferenced(jp: JavaParser) = assertChanged(
        jp,
        recipe = FixEmptyListMethodType().doNext(
            addImports(
                AddImport("java.util.Collections", "*", true)
            )
        ),
        before = """
            package a;
            
            import java.util.List;
            
            class A {
                public A() {
                    List<String> list = java.util.Collections.emptyList();
                }
            }
        """,
        after = """
            package a;
            
            import java.util.List;
            
            import static java.util.Collections.*;
            
            class A {
                public A() {
                    List<String> list = emptyList();
                }
            }
        """
    )

    /**
     * This visitor removes the "java.util.Collections" receiver from method invocations of "java.util.Collections.emptyList()".
     * This allows us to test that AddImport with setOnlyIfReferenced = true will add a static import when an applicable static method call is present
     */
    private class FixEmptyListMethodType : Recipe() {
        override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
            return object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitMethodInvocation(
                    method: J.MethodInvocation,
                    ctx: ExecutionContext
                ): J.MethodInvocation {
                    val original: J.MethodInvocation = super.visitMethodInvocation(method, ctx)
                    if (original.name.simpleName == "emptyList") {
                        return original.withSelect(null)
                    }
                    return original
                }
            }
        }
    }
}
