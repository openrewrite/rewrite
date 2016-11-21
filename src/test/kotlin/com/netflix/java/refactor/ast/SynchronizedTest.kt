package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class SynchronizedTest(p: Parser): Parser by p {

    val a by lazy {
        parse("""
            public class A {
                Integer n = 0;
                public void test() {
                    synchronized(n) {
                    }
                }
            }
        """)
    }

    val sync by lazy { a.firstMethodStatement() as Tr.Synchronized }

    @Test
    fun synchronized() {
        assertTrue(sync.lock.tree is Tr.Ident)
    }

    @Test
    fun format() {
        assertEquals("synchronized(n) {\n}", sync.printTrimmed())
    }
}