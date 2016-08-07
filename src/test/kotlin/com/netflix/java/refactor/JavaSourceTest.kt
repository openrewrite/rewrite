package com.netflix.java.refactor

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class JavaSourceTest : AbstractRefactorTest() {
    
    @Test
    fun listClasses() {
        val multipleClasses = java("""
            package mypackage;
            public class A { }
            class B {}
        """)
        
        assertArrayEquals(arrayOf("mypackage.A", "mypackage.B"), parseJava(multipleClasses).classes().toTypedArray())
    }
    
    @Test
    fun multiPhaseRefactoring() {
        val (b, c) = listOf("B", "C").map {
            java("""
                public class $it {
                    public void foo(int i) {}
                    public void bar(int i) {}
                }
            """)
        }
        
        val a = java("""
            |public class A {
            |   public void test() {
            |      B local = new B();
            |      local.foo(0);
            |   }
            |}
        """)
        
        val refactorer = parseJava(a, b, c)
        
        refactorer.refactor().changeType("B", "C").fix()
        
        assertRefactored(a, """
            |public class A {
            |   public void test() {
            |      C local = new C();
            |      local.foo(0);
            |   }
            |}
        """)
        
        refactorer.refactor()
                .findMethodCalls("C foo(int)")
                    .changeName("bar")
                    .done()
                .fix()
        
        assertRefactored(a, """
            |public class A {
            |   public void test() {
            |      C local = new C();
            |      local.bar(0);
            |   }
            |}
        """)
    }
}