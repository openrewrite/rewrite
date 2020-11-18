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
import org.openrewrite.RefactorVisitorTest
import org.openrewrite.java.tree.Flag
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

interface AddImportTest: RefactorVisitorTest {
    @Test
    fun addMultipleImports(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                    AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) },
                    AddImport().apply { setType("java.util.Set"); setOnlyIfReferenced(false) }
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
    fun addNamedImport(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) }
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
            visitors = listOf(
                    AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(true) }
            ),
            before = """
                package a;
                
                class A {}
            """
    )

    @Test
    fun doNotAddWildcardImportIfNotReferenced(jp: JavaParser) = assertUnchanged(
            jp,
            visitors = listOf(
                    AddImport().apply { setType("java.util.*"); setOnlyIfReferenced(true) }
            ),
            before = """
                package a;
                
                class A {}
            """
    )

    @Test
    fun lastImportWhenFirstClassDeclarationHasJavadoc(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                AddImport().apply {
                    setType("java.util.Collections")
                    setStaticMethod("*")
                    setOnlyIfReferenced(false)
                }
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
    fun namedImportAddedAfterPackageDeclaration(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) }
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

            assertRefactored(
                    jp,
                    dependencies = listOf(
                        *otherImports.toTypedArray(),
                        """
                            package $pkg;
                            public class B {}
                        """
                    ),
                    visitors = listOf(
                        AddImport().apply { setType("$pkg.B"); setOnlyIfReferenced(false) }
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
            visitors = listOf(
                AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) }
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
            visitors = listOf(
                AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) }
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
            visitors = listOf(
                    AddImport().apply {
                        setType("C")
                        setOnlyIfReferenced(false)
                    }
            ),
            before = "class A {}"
    )

    @Test
    fun dontAddImportForPrimitive(jp: JavaParser) = assertUnchanged(
            jp,
            visitors = listOf(AddImport().apply {
                setType("int")
            }),
            before = "class A {}"
    )

    @Test
    fun addNamedImportIfStarStaticImportExists(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) }
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
    fun addNamedStaticImport(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                AddImport().apply {
                    setType("java.util.Collections")
                    setStaticMethod("emptyList")
                    setOnlyIfReferenced(false)
                }
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

    @Test
    fun dontAddStaticWildcardImportIfNotReferenced(jp: JavaParser) = assertUnchanged(
            jp,
            visitors = listOf(
                    AddImport().apply {
                        setType("java.util.Collections")
                        setStaticMethod("*")
                        setOnlyIfReferenced(true)
                    }
            ),
            before = """
                package a;
                
                class A {}
            """
    )

    @Test
    fun addNamedStaticImportWhenReferenced(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                    FixEmptyListMethodType(),
                    AddImport().apply {
                        setType("java.util.Collections")
                        setStaticMethod("emptyList")
                        setOnlyIfReferenced(true)
                    }
            ),
            before = """
                package a;
                
                import java.util.List;
                
                class A {
                    public A() {
                        List<String> list = emptyList();
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
            visitors = listOf(
                    AddImport().apply {
                        setType("java.util.Collections")
                        setStaticMethod("emptyList")
                        setOnlyIfReferenced(true)
                    }
            ),
            before = """
                package a;
                
                class A {}
            """
    )

    @Test
    fun addStaticWildcardImportWhenReferenced(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                    FixEmptyListMethodType(),
                    AddImport().apply {
                        setType("java.util.Collections")
                        setStaticMethod("*")
                        setOnlyIfReferenced(true)
                    }
            ),
            before = """
                package a;
                
                import java.util.List;
                
                class A {
                    public A() {
                        List<String> list = emptyList();
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
     * This visitor is used to set the method type of a statically referenced call to java.util.Collections.emptyList().
     * This allows us to leave an unqualified "emptyList()" method invocation in our "before" snippets and to ensure
     * the static import is correctly added afterwards.
     */
    private class FixEmptyListMethodType : JavaIsoRefactorVisitor() {
        override fun visitMethodInvocation(method: J.MethodInvocation?): J.MethodInvocation {

            val original: J.MethodInvocation = super.visitMethodInvocation(method)

            if (original.name.simpleName == "emptyList") {
                val signature = JavaType.Method.Signature(JavaType.Class.build("java.util.List"), emptyList())
                val emptyListMethod: JavaType.Method = JavaType.Method.build(
                        JavaType.Class.build("java.util.Collections"),
                        "emptyList",
                        signature,
                        signature,
                        Collections.emptyList(),
                        Stream.of(Flag.Public, Flag.Static).collect(Collectors.toSet()))
                return original.withType(emptyListMethod)
            }
            return original
        }
    }

}
