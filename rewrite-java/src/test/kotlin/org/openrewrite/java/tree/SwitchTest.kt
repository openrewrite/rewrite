package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class SwitchTest : JavaParser() {
    
    @Test
    fun switch() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {
                    case 0: break;
                    }
                }
            }
        """)
        
        val switch = a.firstMethodStatement() as J.Switch
        assertTrue(switch.selector.tree is J.Ident)
        assertEquals(1, switch.cases.statements.size)
        
        val case0 = switch.cases.statements[0]
        assertTrue(case0.pattern is J.Literal)
        assertTrue(case0.statements[0] is J.Break)
    }
    
    @Test
    fun switchWithDefault() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {
                    default: System.out.println("default!");
                    }
                }
            }
        """)

        val switch = a.firstMethodStatement() as J.Switch
        assertTrue(switch.selector.tree is J.Ident)
        assertEquals(1, switch.cases.statements.size)

        val default = switch.cases.statements[0]
        assertEquals("default", (default.pattern as J.Ident).simpleName)
        assertTrue(default.statements[0] is J.MethodInvocation)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {
                    default: break;
                    }
                }
            }
        """)

        val switch = a.firstMethodStatement() as J.Switch
        assertEquals("""
            switch(n) {
            default: break;
            }
        """.trimIndent(), switch.printTrimmed())
    }
    
    @Test
    fun switchWithNoCases() {
        val a = parse("""
            public class A {
                int n;
                public void test() {
                    switch(n) {}
                }
            }
        """)

        val switch = a.firstMethodStatement() as J.Switch
        assertTrue(switch.selector.tree is J.Ident)
        assertEquals(0, switch.cases.statements.size)
    }

    @Test
    fun multipleCases() {
        val aSrc = """
            public class A {
                int n;
                public void test() {
                    switch(n) {
                    case 0: {
                       break;
                    }
                    case 1: {
                       break;
                    }
                    }
                }
            }
        """.trimIndent()

        assertEquals(aSrc, parse(aSrc).printTrimmed())
    }
}