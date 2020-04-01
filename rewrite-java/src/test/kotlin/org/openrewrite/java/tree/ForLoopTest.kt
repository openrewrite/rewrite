package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

open class ForLoopTest : JavaParser() {
    
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
        
        val forLoop = a.firstMethodStatement() as J.ForLoop
        assertTrue(forLoop.control.init is J.VariableDecls)
        assertTrue(forLoop.control.condition is J.Binary)
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

        val forLoop = a.firstMethodStatement() as J.ForLoop
        assertTrue(forLoop.control.init is J.Empty)
        assertTrue(forLoop.control.condition is J.Empty)
        assertTrue(forLoop.control.update[0] is J.Empty)
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

        val forLoop = a.firstMethodStatement() as J.ForLoop
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

        val forLoop = a.firstMethodStatement() as J.ForLoop
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

        // FIXME are body statements printed out of order?
        val forLoop = a.classes[0].methods[0].body!!.statements[1] as J.ForLoop
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

        val forLoop = a.classes[0].methods[0].body!!.statements[1] as J.ForLoop
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

        val forLoop = a.classes[0].methods[0].body!!.statements[0] as J.ForLoop
        assertEquals("for(;;) test();", forLoop.printTrimmed())
    }

    @Test
    fun initializerIsAnAssignment() {
        val a = parse("""
            public class A {
                int[] a;
                public void test() {
                    int i=0;
                    for(i=0; i<a.length; i++) {}
                }
            }
        """)

        val forLoop = a.classes[0].methods[0].body!!.statements[1]
        assertEquals("for(i=0; i<a.length; i++) {}", forLoop.printTrimmed())
    }

    @Test
    fun multiVariableInitialization() {
        val a = parse("""
            public class A {
                public void test() {
                    for(int i, j = 0;;) {}
                }
            }
        """)

        assertEquals("for(int i, j = 0;;) {}", a.classes[0].methods[0].body!!.statements[0].printTrimmed())
    }
}