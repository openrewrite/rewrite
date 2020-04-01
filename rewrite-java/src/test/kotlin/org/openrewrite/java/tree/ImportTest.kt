package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class ImportTest : JavaParser() {

    @Test
    fun matchImport() {
        val a = parse("""
            import java.util.List;
            public class A {}
        """)

        assertTrue(a.imports.first().isFromType("java.util.List"))
    }

    @Test
    fun matchStarImport() {
        val a = parse("""
            import java.util.*;
            public class A {}
        """)

        assertTrue(a.imports.first().isFromType("java.util.List"))
    }

    @Test
    fun hasStarImportOnInnerClass() {
        val a = """
            package a;
            public class A {
               public static class B { }
            }
        """

        val c = """
            import a.*;
            public class C {
                A.B b = new A.B();
            }
        """

        val cu = parse(c, a)
        val import = cu.imports.first()
        assertTrue(import.isFromType("a.A.B"))
        assertTrue(import.isFromType("a.A"))
    }
    
    @Test
    fun format() {
        val a = parse("""
            import java.util.List;
            import static java.util.Collections.*;
            public class A {}
        """)
        
        assertEquals("import java.util.List", a.imports[0].printTrimmed())
        assertEquals("import static java.util.Collections.*", a.imports[1].printTrimmed())
    }

    @Test
    fun compare() {
        val a = parse("""
            import b.B;
            import c.c.C;
        """.trimIndent())

        val (b, c) = a.imports

        assertTrue(b < c)
        assertTrue(c > b)
    }

    @Test
    fun compareSamePackageDifferentNameLengths() {
        val a = parse("""
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
        """.trimIndent())

        val (b, c) = a.imports

        assertTrue(b < c)
        assertTrue(c > b)
    }
}