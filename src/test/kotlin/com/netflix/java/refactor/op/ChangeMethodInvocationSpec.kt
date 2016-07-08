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
        val rule = RefactorRule()
                .changeMethod("B foo(int)")
                    .refactorName("bar")
                    .done()

        val b = temp.newFile("B.java")
        b.writeText("""
            |class B {
            |   public void foo(int i) {}
            |   public void bar(int i) {}
            |}
        """.trimMargin())

        val a = temp.newFile("A.java")
        a.writeText("""
            |class A {
            |   public void test() {
            |       new B().foo(0);
            |   }
            |}
        """.trimMargin())

        rule.refactorAndFix(a, b)

        assertEquals("""
            |class A {
            |   public void test() {
            |       new B().bar(0);
            |   }
            |}
        """.trimMargin(), a.readText())
    }

    @Test
    fun transformStringArgument() {
        val rule = RefactorRule()
                .changeMethod("B foo(String)")
                    .refactorArgument(0)
                        .isType(String::class.java)
                        .mapLiterals { s -> s.toString().replace("%s", "{}") }
                        .done()
                .done()

        val b = temp.newFile("B.java")
        b.writeText("""
            |class B {
            |   public void foo(String s) {}
            |}
        """.trimMargin())

        val a = temp.newFile("A.java")
        a.writeText("""
            |class A {
            |   public void test() {
            |       String s = "bar";
            |       new B().foo("foo %s " + s + 0L);
            |   }
            |}
        """.trimMargin())

        rule.refactorAndFix(a, b)

        assertEquals("""
            |class A {
            |   public void test() {
            |       String s = "bar";
            |       new B().foo("foo {} " + s + 0L);
            |   }
            |}
        """.trimMargin(), a.readText())
    }
}