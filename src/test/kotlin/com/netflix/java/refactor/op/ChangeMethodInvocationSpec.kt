package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChangeMethodInvocationSpec {
    @JvmField @Rule
    val temp = TemporaryFolder()
    
    @Test
    fun refactorMethodName() {
        val rule = RefactorRule("foo-to-bar")
                .changeMethod("b.foo()")
                    .refactorName("bar")
                    .done()

        val b = temp.newFile("B.java")
        b.writeText("""
            |class B {
            |   public void foo() {}
            |   public void bar() {}
            |}
        """.trimMargin())

        val a = temp.newFile("A.java")
        a.writeText("""
            |class A {
            |   public void test() {
            |       new B().foo();
            |   }
            |}
        """.trimMargin())

        rule.refactorAndFix(a, b)

        assertEquals("""
            |class A {
            |   public void test() {
            |       new B().bar();
            |   }
            |}
        """.trimMargin(), a.readText())
    }
    
    @Test
    fun matchOnTargetType() {
        val rule = RefactorRule("foo-to-bar")
            .changeMethod("b.foo()")
                .whereTargetIsType("B")
                .done()
        
        val fooBars = listOf("B, C").map { clazz ->
            val fooBar = temp.newFile("B.java")
            fooBar.writeText("""
                |class $clazz {
                |   public void foo() {}
                |   public void bar() {}
                |}
            """.trimMargin())            
            fooBar
        }

        val a = temp.newFile("A.java")
        a.writeText("""
            |class A {
            |   public void test() {
            |       new B().foo();
            |       new C().foo();
            |   }
            |}
        """.trimMargin())
        
        rule.refactorAndFix(*fooBars.plus(a).toTypedArray())
        
        assertEquals("""
            |class A {
            |   public void test() {
            |       new B().bar();
            |       new C().foo();
            |   }
            |}
        """.trimMargin(), a.readText())
    }
}