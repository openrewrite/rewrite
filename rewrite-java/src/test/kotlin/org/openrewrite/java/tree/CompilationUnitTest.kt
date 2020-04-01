package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored
import java.nio.file.Path

open class CompilationUnitTest : JavaParser() {

    @Test
    fun newClass() {
        val a = J.CompilationUnit.buildEmptyClass(Path.of("sourceSet"), "my.org", "MyClass")

        assertRefactored(a, """
            package my.org;
            
            public class MyClass {
            }
        """)
    }

    @Test
    fun imports() {
        val a = parse("""
            import java.util.List;
            import java.io.*;
            public class A {}
        """)

        assertEquals(2, a.imports.size)
    }

    @Test
    fun classes() {
        val a = parse("""
            public class A {}
            class B{}
        """)

        assertEquals(2, a.classes.size)
    }
    
    @Test
    fun format() {
        val a = """
            /* Comment */
            package a;
            import java.util.List;
            
            public class A { }
        """
        
        assertEquals(a.trimIndent(), parse(a).printTrimmed())
    }
}