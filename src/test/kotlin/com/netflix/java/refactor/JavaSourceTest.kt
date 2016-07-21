package com.netflix.java.refactor

import org.junit.Test

class JavaSourceTest : AbstractRefactorTest() {
    
    @Test
    fun multiPhaseRefactoring() {
        val (b, c) = listOf("B", "C").map {
            java("""
                |public class $it {
                |   public void foo(int i) {}
                |   public void bar(int i) {}
                |}
            """)
        }
        
        val a = java("""
            |public class A {
            |   public void test() {
            |      B local = new B();
            |      local.foo();
            |   }
            |}
        """)
        
        val refactorer = refactor(a, b, c)
        
        refactorer.changeType("B", "C")
        
        assertRefactored(a, """
            |public class A {
            |   public void test() {
            |      C local = new C();
            |      local.foo();
            |   }
            |}
        """)
        
        refactorer.changeMethod("C foo()")
            .refactorName("bar")
            .done()
        
        assertRefactored(a, """
            |public class A {
            |   public void test() {
            |      C local = new C();
            |      local.bar();
            |   }
            |}
        """)
    }
}