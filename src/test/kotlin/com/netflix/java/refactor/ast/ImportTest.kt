package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class ImportTest(p: Parser): Parser by p {
    
    @Test
    fun matchImport() {
        val a = parse("""
            import java.util.List;
            public class A {}
        """)

        assertTrue(a.imports.first().matches("java.util.List"))
    }

    @Test
    fun matchStarImport() {
        val a = parse("""
            import java.util.*;
            public class A {}
        """)

        assertTrue(a.imports.first().matches("java.util.List"))
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

        val cu = parse(c, whichDependsOn = a)
        val import = cu.imports.first()
        assertTrue(import.matches("a.A.B"))
        assertTrue(import.matches("a.A"))
    }
    
    @Test
    fun format() {
        val a = parse("""
            |import java.util.List;
            |import static java.util.Collections.*;
            |public class A {}
        """)
        
        assertEquals("import java.util.List", a.imports[0].printTrimmed())
        assertEquals("import static java.util.Collections.*", a.imports[1].printTrimmed())
    }
}