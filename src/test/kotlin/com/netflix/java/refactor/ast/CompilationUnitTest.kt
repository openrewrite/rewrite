package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class CompilationUnitTest(p: Parser): Parser by p {
    
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
            |/* Comment */
            |package a;
            |import java.util.List;
            |
            |public class A { }
        """
        
        assertEquals(a.trimMargin(), parse(a).printTrimmed())
    }
}