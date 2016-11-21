package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import com.netflix.java.refactor.refactor.Refactor
import org.junit.Test

abstract class AddImportTest(parser: Parser): Parser by parser {

    @Test
    fun addNamedImport() {
        val a = parse("class A {}")

        val fixed = a.refactor().addImport("java.util.List").fix()

        assertRefactored(fixed, """
            |import java.util.List;
            |
            |class A {}
        """)
    }

    @Test
    fun addNamedImportByClass() {
        val a = parse("class A {}")

        val fixed = a.refactor().addImport(List::class.java).fix()

        assertRefactored(fixed, """
            |import java.util.List;
            |
            |class A {}
        """)
    }

    @Test
    fun namedImportAddedAfterPackageDeclaration() {
        val a = parse("""
            |package a;
            |class A {}
        """)

        val fixed = a.refactor().addImport(List::class.java).fix()

        assertRefactored(fixed, """
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
            "package $pkg;\npublic class C$i {}"
        }

        listOf("b" to 0, "c.b" to 1, "c.c.b" to 2).forEach {
            val (pkg, order) = it

            val b = """
                |package $pkg;
                |public class B {}
            """

            val a = """
                |package a;
                |
                |import c.C0;
                |import c.c.C1;
                |import c.c.c.C2;
                |
                |class A {}
            """

            val cu = parse(a, otherImports.plus(b))
            val fixed = cu.refactor().addImport("$pkg.B").fix()

            val expectedImports = otherPackages.mapIndexed { i, otherPkg -> "$otherPkg.C$i" }.toMutableList()
            expectedImports.add(order, "$pkg.B")
            assertRefactored(fixed, "package a;\n\n${expectedImports.map { "import $it;" }.joinToString("\n")}\n\nclass A {}")

            reset()
        }
    }

    @Test
    fun doNotAddImportIfAlreadyExists() {
        val a = parse("""
            |package a;
            |
            |import java.util.List;
            |class A {}
        """)

        val fixed = a.refactor().addImport(List::class.java).fix()

        assertRefactored(fixed, """
            |package a;
            |
            |import java.util.List;
            |class A {}
        """)
    }

    @Test
    fun doNotAddImportIfCoveredByStarImport() {
        val a = parse("""
            |package a;
            |
            |import java.util.*;
            |class A {}
        """)

        val fixed = a.refactor().addImport(List::class.java).fix()

        assertRefactored(fixed, """
            |package a;
            |
            |import java.util.*;
            |class A {}
        """)
    }

    @Test
    fun addNamedImportIfStarStaticImportExists() {
        val a = parse("""
            |package a;
            |
            |import static java.util.List.*;
            |class A {}
        """)

        val fixed = a.refactor().addImport(List::class.java).fix()

        assertRefactored(fixed, """
            |package a;
            |
            |import java.util.List;
            |import static java.util.List.*;
            |class A {}
        """)
    }

    @Test
    fun addNamedStaticImport() {
        val a = parse("""
            |import java.util.*;
            |class A {}
        """)

        val fixed = a.refactor().addImport("java.util.Collections", "emptyList").fix()

        assertRefactored(fixed, """
            |import java.util.*;
            |import static java.util.Collections.emptyList;
            |class A {}
        """)
    }

    @Test
    fun dontAddImportWhenClassHasNoPackage() {
        val a = parse("class A {}")
        val fixed = a.refactor().addImport("C").fix()
        assertRefactored(fixed, "class A {}")
    }
}

class OracleJdkAddImportTest: AddImportTest(OracleJdkParser())