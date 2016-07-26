package com.netflix.java.refactor.fix

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Test

class AddImportTest: AbstractRefactorTest() {
    @Test
    fun addNamedImport() {
        val a = java("class A {}")
        
        parseJava(a).refactor().addImport("java.util.List").fix()

        assertRefactored(a, """
            |import java.util.List;
            |class A {}
        """)
    }

    @Test
    fun addNamedImportByClass() {
        val a = java("class A {}")
        
        parseJava(a).refactor().addImport(List::class.java).fix()
        
        assertRefactored(a, """
            |import java.util.List;
            |class A {}
        """)
    }
    
    @Test
    fun namedImportAddedAfterPackageDeclaration() {
        val a = java("""
            |package a;
            |class A {}
        """)
        parseJava(a).refactor().addImport(List::class.java).fix()

        assertRefactored(a, """
            |package a;
            |
            |import java.util.List;
            |class A {}
        """)
    }
    
    @Test
    fun importsAddedInAlphabeticalOrder() {
        val otherPackages = listOf("c", "c.c", "c.c.c")
        val otherImports = otherPackages.mapIndexed { i, pkg ->
            java("package $pkg;\npublic class C$i {}")
        }
        
        val a = temp.newFile("A.java")
        val b = temp.newFile("B.java")
        
        listOf("b" to 0, "c.b" to 1, "c.c.b" to 2).forEach {
            val (pkg, order) = it
            
            b.writeText("""
                |package $pkg;
                |public class B {}
            """.trimMargin())
            
            a.writeText("""
                |package a;
                |
                |import c.C0;
                |import c.c.C1;
                |import c.c.c.C2;
                |
                |class A {}
            """.trimMargin())

            parseJava(a, otherImports.plus(b)).refactor().addImport("$pkg.B").fix()

            val expectedImports = otherPackages.mapIndexed { i, otherPkg -> "$otherPkg.C$i" }.toMutableList()
            expectedImports.add(order, "$pkg.B")
            assertRefactored(a, "package a;\n\n${expectedImports.map { "import $it;" }.joinToString("\n")}\n\nclass A {}")
        }
    }
    
    @Test
    fun doNotAddImportIfAlreadyExists() {
        val a = java("""
            |package a;
            |
            |import java.util.List;
            |class A {}
        """)

        parseJava(a).refactor().addImport(List::class.java).fix()

        assertRefactored(a, """
            |package a;
            |
            |import java.util.List;
            |class A {}
        """)
    }
    
    @Test
    fun doNotAddImportIfCoveredByStarImport() {
        val a = java("""
            |package a;
            |
            |import java.util.*;
            |class A {}
        """)

        parseJava(a).refactor().addImport(List::class.java).fix()

        assertRefactored(a, """
            |package a;
            |
            |import java.util.*;
            |class A {}
        """)
    }
    
    @Test
    fun addNamedImportIfStarStaticImportExists() {
        val a = java("""
            |package a;
            |
            |import static java.util.List.*;
            |class A {}
        """)

        parseJava(a).refactor().addImport(List::class.java).fix()

        assertRefactored(a, """
            |package a;
            |
            |import java.util.List;
            |import static java.util.List.*;
            |class A {}
        """)
    }

    @Test
    fun addNamedStaticImport() {
        val a = java("""
            |import java.util.*;
            |class A {}
        """)

        parseJava(a).refactor().addStaticImport("java.util.Collections", "emptyList").fix()

        assertRefactored(a, """
            |import java.util.*;
            |import static java.util.Collections.emptyList;
            |class A {}
        """)
    }
    
    @Test
    fun dontAddImportWhenClassHasNoPackage() {
        val a = java("class A {}")
        
        parseJava(a).refactor().addImport("C").fix()
        
        assertRefactored(a, "class A {}")
    }
}
