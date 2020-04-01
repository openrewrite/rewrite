package org.openrewrite.java.search

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class HasImportTest : JavaParser() {
    
    @Test
    fun hasImport() {
        val a = parse("""
            import java.util.List;
            class A {}
        """)
        
        assertTrue(a.hasImport("java.util.List"))
        assertFalse(a.hasImport("java.util.Set"))
    }

    @Test
    fun hasStarImport() {
        val a = parse("""
            import java.util.*;
            class A {}
        """)

        assertTrue(a.hasImport("java.util.List"))
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

        assertTrue(parse(c, a).hasImport("a.A.B"))
        assertTrue(parse(c, a).hasImport("a.A"))
    }
}
