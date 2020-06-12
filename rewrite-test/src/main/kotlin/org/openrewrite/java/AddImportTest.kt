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

interface AddImportTest {
    @Test
    fun addMultipleImports(jp: JavaParser) {
        val a = jp.parse("class A {}")

        val fixed = a.refactor()
                .visit(AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) })
                .visit(AddImport().apply { setType("java.util.Set"); setOnlyIfReferenced(false) })
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            import java.util.Set;
            
            class A {}
        """)
    }

    @Test
    fun addNamedImport(jp: JavaParser) {
        val a = jp.parse("class A {}")

        val fixed = a.refactor().visit(AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) })
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            class A {}
        """)
    }

    @Test
    fun namedImportAddedAfterPackageDeclaration(jp: JavaParser) {
        val a = jp.parse("""
            package a;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) })
                .fix().fixed

        assertRefactored(fixed, """
            package a;
            
            import java.util.List;
            
            class A {}
        """)
    }

    @Test
    fun importsAddedInAlphabeticalOrder(jp: JavaParser) {
        val otherPackages = listOf("c", "c.c", "c.c.c")
        val otherImports = otherPackages.mapIndexed { i, pkg ->
            "package $pkg;\npublic class C$i {}"
        }

        listOf("b" to 0, "c.b" to 1, "c.c.b" to 2).forEach {
            val (pkg, order) = it

            val b = """
                package $pkg;
                public class B {}
            """

            val cu = jp.parse("""
                package a;
                
                import c.C0;
                import c.c.C1;
                import c.c.c.C2;
                
                class A {}
            """.trimIndent(), otherImports.plus(b))
            val fixed = cu.refactor().visit(AddImport().apply { setType("$pkg.B"); setOnlyIfReferenced(false) })
                    .fix().fixed

            val expectedImports = otherPackages.mapIndexed { i, otherPkg -> "$otherPkg.C$i" }.toMutableList()
            expectedImports.add(order, "$pkg.B")
            assertRefactored(fixed, "package a;\n\n${expectedImports.joinToString("\n") { fqn -> "import $fqn;" }}\n\nclass A {}")

            jp.reset()
        }
    }

    @Test
    fun doNotAddImportIfAlreadyExists(jp: JavaParser) {
        val a = jp.parse("""
            package a;
            
            import java.util.List;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) })
                .fix().fixed

        assertRefactored(fixed, """
            package a;
            
            import java.util.List;
            class A {}
        """)
    }

    @Test
    fun doNotAddImportIfCoveredByStarImport(jp: JavaParser) {
        val a = jp.parse("""
            package a;
            
            import java.util.*;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) })
                .fix().fixed

        assertRefactored(fixed, """
            package a;
            
            import java.util.*;
            class A {}
        """)
    }

    @Test
    fun addNamedImportIfStarStaticImportExists(jp: JavaParser) {
        val a = jp.parse("""
            package a;
            
            import static java.util.List.*;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(AddImport().apply { setType("java.util.List"); setOnlyIfReferenced(false) })
                .fix().fixed

        assertRefactored(fixed, """
            package a;
            
            import java.util.List;
            
            import static java.util.List.*;

            class A {}
        """)
    }

    @Test
    fun addNamedStaticImport(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            class A {}
        """.trimIndent())

        val fixed = a.refactor()
                .visit(AddImport().apply {
                    setType("java.util.Collections")
                    setStaticMethod("emptyList")
                    setOnlyIfReferenced(false)
                })
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.*;
            
            import static java.util.Collections.emptyList;

            class A {}
        """)
    }

    @Test
    fun dontAddImportWhenClassHasNoPackage(jp: JavaParser) {
        val a = jp.parse("class A {}")

        val fixed = a.refactor().visit(AddImport().apply {
            setType("C")
            setOnlyIfReferenced(false)
        }).fix().fixed

        assertRefactored(fixed, "class A {}")
    }
}
