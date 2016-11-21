package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class SwitchTest(p: Parser): Parser by p {
    
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
        
        val switch = a.firstMethodStatement() as Tr.Switch
        assertTrue(switch.selector.tree is Tr.Ident)
        assertEquals(1, switch.cases.statements.size)
        
        val case0 = switch.cases.statements[0]
        assertTrue(case0.pattern is Tr.Literal)
        assertTrue(case0.statements[0] is Tr.Break)
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

        val switch = a.firstMethodStatement() as Tr.Switch
        assertTrue(switch.selector.tree is Tr.Ident)
        assertEquals(1, switch.cases.statements.size)

        val default = switch.cases.statements[0]
        assertEquals("default", (default.pattern as Tr.Ident).name)
        assertTrue(default.statements[0] is Tr.MethodInvocation)
    }

    @Test
    fun format() {
        val a = parse("""
            |public class A {
            |    int n;
            |    public void test() {
            |        switch(n) {
            |        default: break;
            |        }
            |    }
            |}
        """)

        val switch = a.firstMethodStatement() as Tr.Switch
        assertEquals("""
            |switch(n) {
            |default: break;
            |}
        """.trimMargin(), switch.printTrimmed())
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

        val switch = a.firstMethodStatement() as Tr.Switch
        assertTrue(switch.selector.tree is Tr.Ident)
        assertEquals(0, switch.cases.statements.size)
    }

    @Test
    fun multipleCases() {
        val aSrc = """
            |public class A {
            |    int n;
            |    public void test() {
            |        switch(n) {
            |        case 0: {
            |           break;
            |        }
            |        case 1: {
            |           break;
            |        }
            |        }
            |    }
            |}
        """.trimMargin()

        assertEquals(aSrc, parse(aSrc).printTrimmed())
    }
}