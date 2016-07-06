package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AddImportSpec {
    @JvmField @Rule
    val temp = TemporaryFolder()

    val addImportRule = RefactorRule("add-list-import")
            .addImport("java.util", "List")
    
    @Test
    fun addNamedImport() {
        val a = temp.newFile("A.java")
        a.writeText("class A {}")

        addImportRule.refactorAndFix(a)

        assertEquals("""
            |import java.util.List;
            |class A {}
        """.trimMargin(), a.readText())
    }
    
    @Test
    fun namedImportAddedAfterPackageDeclaration() {
        val a = temp.newFile("A.java")
        a.writeText("""
            |package a;
            |class A {}
        """.trimMargin())

        addImportRule.refactorAndFix(a)

        assertEquals("""
            |package a;
            |
            |import java.util.List;
            |class A {}
        """.trimMargin(), a.readText())
    }
    
    @Test
    fun comparePackages() {
        val comp = AddImportScanner(AddImport("doesnotmatter", "doesnotmatter")).packageComparator
        
        assertEquals(-1, comp.compare("a", "b"))
        assertEquals(-1, comp.compare("a.a", "a.b"))
        assertEquals(1, comp.compare("b", "a"))
        assertEquals(1, comp.compare("a.b", "a.a"))
        assertEquals(0, comp.compare("a", "a"))
        assertEquals(1, comp.compare("a.a", "a"))
        assertEquals(-1, comp.compare("a", "a.a"))
    }
    
    @Test
    fun importsAddedInAlphabeticalOrder() {
        val otherPackages = listOf("c", "c.c", "c.c.c")
        val otherImports = otherPackages.mapIndexed { i, pkg ->
            val otherImport = temp.newFile("C$i.java")
            otherImport.writeText("package $pkg;\npublic class C$i {}")
            otherImport
        }
        
        val a = temp.newFile("A.java")
        val b = temp.newFile("B.java")

        listOf("b" to 0, "c.b" to 1, "c.c.b" to 2).forEach {
            val (pkg, order) = it

            val addImportRule = RefactorRule("add-list-import")
                    .addImport(pkg, "B")
            
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

            addImportRule.refactorAndFixWhen({ f, cu -> f === a }, *otherImports.plus(b).plus(a).toTypedArray())

            val expectedImports = otherPackages.mapIndexed { i, otherPkg -> "$otherPkg.C$i" }.toMutableList()
            expectedImports.add(order, "$pkg.B")
            assertEquals("package a;\n\n${expectedImports.map { "import $it;" }.joinToString("\n")}\n\nclass A {}", a.readText())
        }
    }
    
    @Test
    fun doNotAddImportIfAlreadyExists() {
        val a = temp.newFile("A.java")
        a.writeText("""
            |package a;
            |
            |import java.util.List;
            |class A {}
        """.trimMargin())

        addImportRule.refactorAndFix(a)

        assertEquals("""
            |package a;
            |
            |import java.util.List;
            |class A {}
        """.trimMargin(), a.readText())
    }
    
    @Test
    fun doNotAddImportIfCoveredByStarImport() {
        val a = temp.newFile("A.java")
        a.writeText("""
            |package a;
            |
            |import java.util.*;
            |class A {}
        """.trimMargin())

        addImportRule.refactorAndFix(a)

        assertEquals("""
            |package a;
            |
            |import java.util.*;
            |class A {}
        """.trimMargin(), a.readText())
    }
    
    @Test
    fun addNamedImportIfStarStaticImportExists() {
        val a = temp.newFile("A.java")
        a.writeText("""
            |package a;
            |
            |import static java.util.List.*;
            |class A {}
        """.trimMargin())

        addImportRule.refactorAndFix(a)

        assertEquals("""
            |package a;
            |
            |import java.util.List;
            |import static java.util.List.*;
            |class A {}
        """.trimMargin(), a.readText())
    }
}
