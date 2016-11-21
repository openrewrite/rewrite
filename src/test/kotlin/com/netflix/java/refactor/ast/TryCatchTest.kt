package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class TryCatchTest(p: Parser): Parser by p {
    
    @Test
    fun tryFinally() {
        val a = parse("""
            public class A {
                public void test() {
                    try {
                    }
                    finally {
                    }
                }
            }
        """)
        
        val tryable = a.firstMethodStatement() as Tr.Try
        assertTrue(tryable.body is Tr.Block)
        assertEquals(0, tryable.catches.size)
        assertTrue(tryable.finally is Tr.Try.Finally)
    }
    
    @Test
    fun tryCatchNoFinally() {
        val a = parse("""
            public class A {
                public void test() {
                    try {
                    }
                    catch(Throwable t) {
                    }
                }
            }
        """)

        val tryable = a.firstMethodStatement() as Tr.Try
        assertEquals(1, tryable.catches.size)
    }
    
    @Test
    fun tryWithResources() {
        val a = parse("""
            import java.io.*;
            public class A {
                File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) {
                    }
                    catch(IOException e) {
                    }
                }
            }
        """)

        val tryable = a.firstMethodStatement() as Tr.Try
        assertEquals(1, tryable.resources?.decls?.size ?: -1)
    }

    @Test
    fun formatTryWithResources() {
        val a = parse("""
            import java.io.*;
            public class A {
                File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) { }
                }
            }
        """)

        val tryable = a.firstMethodStatement() as Tr.Try
        assertEquals("try(FileInputStream fis = new FileInputStream(f)) { }",
                tryable.printTrimmed())
    }

    @Test
    fun formatMultiCatch() {
        val a = parse("""
            import java.io.*;
            public class A {
                File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) {}
                    catch(FileNotFoundException | RuntimeException e) {}
                }
            }
        """)

        val multiCatch = (a.firstMethodStatement() as Tr.Try).catches[0].param.tree.typeExpr as Tr.MultiCatch
        assertEquals("FileNotFoundException | RuntimeException", multiCatch.printTrimmed())
    }

    @Test
    fun formatTryCatchFinally() {
        val a = parse("""
            public class A {
                public void test() {
                    try {}
                    catch(Exception e) {}
                    catch(RuntimeException e) {}
                    catch(Throwable t) {}
                    finally {}
                }
            }
        """)

        val tryable = a.firstMethodStatement() as Tr.Try
        assertEquals("try {}\ncatch(Exception e) {}\ncatch(RuntimeException e) {}\ncatch(Throwable t) {}\nfinally {}", tryable.printTrimmed())
    }
}