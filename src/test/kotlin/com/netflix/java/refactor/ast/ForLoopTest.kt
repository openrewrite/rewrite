package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class ForLoopTest(p: Parser): Parser by p {
    
    @Test
    fun forLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    for(int i = 0; i < 10; i++) {
                    }
                }
            }
        """)
        
        val forLoop = a.firstMethodStatement() as Tr.ForLoop
        assertTrue(forLoop.control.init is Tr.VariableDecls)
        assertTrue(forLoop.control.condition is Tr.Binary)
        assertEquals(1, forLoop.control.update.size)
    }

    @Test
    fun infiniteLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    for(;;) {
                    }
                }
            }
        """)

        val forLoop = a.firstMethodStatement() as Tr.ForLoop
        assertTrue(forLoop.control.init is Tr.Empty)
        assertTrue(forLoop.control.condition is Tr.Empty)
        assertTrue(forLoop.control.update[0] is Tr.Empty)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                public void test() {
                    for ( int i = 0 ; i < 10 ; i++ ) {
                    }
                }
            }
        """)

        val forLoop = a.firstMethodStatement() as Tr.ForLoop
        assertEquals("for ( int i = 0 ; i < 10 ; i++ ) {\n}", forLoop.printTrimmed())
    }

    @Test
    fun formatInfiniteLoop() {
        val a = parse("""
            public class A {
                public void test() {
                    for ( ; ; ) {}
                }
            }
        """)

        val forLoop = a.firstMethodStatement() as Tr.ForLoop
        assertEquals("for ( ; ; ) {}", forLoop.printTrimmed())
    }

    @Test
    fun formatLoopNoInit() {
        val a = parse("""
            public class A {
                public void test() {
                    int i = 0;
                    for ( ; i < 10 ; i++ ) {}
                }
            }
        """)

        val forLoop = a.classes[0].methods()[0].body!!.statements[1] as Tr.ForLoop
        assertEquals("for ( ; i < 10 ; i++ ) {}", forLoop.printTrimmed())
    }

    @Test
    fun formatLoopNoCondition() {
        val a = parse("""
            public class A {
                public void test() {
                    int i = 0;
                    for(; i < 10; i++) {}
                }
            }
        """)

        val forLoop = a.classes[0].methods()[0].body!!.statements[1] as Tr.ForLoop
        assertEquals("for(; i < 10; i++) {}", forLoop.printTrimmed())
    }

    @Test
    fun statementTerminatorForSingleLineForLoops() {
        val a = parse("""
            public class A {
                public void test() {
                    for(;;) test();
                }
            }
        """)

        val forLoop = a.classes[0].methods()[0].body!!.statements[0] as Tr.ForLoop
        assertEquals("for(;;) test();", forLoop.printTrimmed())
    }
}