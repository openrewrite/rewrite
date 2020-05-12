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
package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored

open class AddImportTest : JavaParser() {
    @Test
    fun addMultipleImports() {
        val a = parse("class A {}")

        val fixed = a.refactor()
                .visit(AddImport("java.util.List", null, false))
                .visit(AddImport("java.util.Set", null, false))
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            import java.util.Set;
            
            class A {}
        """)
    }

    @Test
    fun addNamedImport() {
        val a = parse("class A {}")

        val fixed = a.refactor().visit(AddImport("java.util.List", null, false)).fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            class A {}
        """)
    }

    @Test
    fun addNamedImportByClass() {
        val a = parse("class A {}")

        val fixed = a.refactor().visit(AddImport("java.util.List", null, false)).fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            
            class A {}
        """)
    }

    @Test
    fun namedImportAddedAfterPackageDeclaration() {
        val a = parse("""
            package a;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(AddImport("java.util.List", null, false)).fix().fixed

        assertRefactored(fixed, """
            package a;
            
            import java.util.List;
            
            class A {}
        """)
    }

    @Test
    fun importsAddedInAlphabeticalOrder() {
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

            val cu = parse("""
                package a;
                
                import c.C0;
                import c.c.C1;
                import c.c.c.C2;
                
                class A {}
            """.trimIndent(), otherImports.plus(b))
            val fixed = cu.refactor().visit(AddImport("$pkg.B", null, false)).fix().fixed

            val expectedImports = otherPackages.mapIndexed { i, otherPkg -> "$otherPkg.C$i" }.toMutableList()
            expectedImports.add(order, "$pkg.B")
            assertRefactored(fixed, "package a;\n\n${expectedImports.joinToString("\n") { fqn -> "import $fqn;" }}\n\nclass A {}")

            reset()
        }
    }

    @Test
    fun doNotAddImportIfAlreadyExists() {
        val a = parse("""
            package a;
            
            import java.util.List;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(AddImport("java.util.List", null, false)).fix().fixed

        assertRefactored(fixed, """
            package a;
            
            import java.util.List;
            class A {}
        """)
    }

    @Test
    fun doNotAddImportIfCoveredByStarImport() {
        val a = parse("""
            package a;
            
            import java.util.*;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(AddImport("java.util.List", null, false)).fix().fixed

        assertRefactored(fixed, """
            package a;
            
            import java.util.*;
            class A {}
        """)
    }

    @Test
    fun addNamedImportIfStarStaticImportExists() {
        val a = parse("""
            package a;
            
            import static java.util.List.*;
            class A {}
        """.trimIndent())

        val fixed = a.refactor().visit(AddImport("java.util.List", null, false)).fix().fixed

        assertRefactored(fixed, """
            package a;
            
            import java.util.List;
            
            import static java.util.List.*;
            
            class A {}
        """)
    }

    @Test
    fun addNamedStaticImport() {
        val a = parse("""
            import java.util.*;
            class A {}
        """.trimIndent())

        val fixed = a.refactor()
                .visit(AddImport("java.util.Collections", "emptyList", false))
                .fix().fixed

        assertRefactored(fixed, """
            import java.util.*;
            
            import static java.util.Collections.emptyList;

            class A {}
        """)
    }

    @Test
    fun dontAddImportWhenClassHasNoPackage() {
        val a = parse("class A {}")
        val fixed = a.refactor().visit(AddImport("C", null, false)).fix().fixed
        assertRefactored(fixed, "class A {}")
    }
}
