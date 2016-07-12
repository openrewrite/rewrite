package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorRule
import com.netflix.java.refactor.aspectj.AspectJLexer
import com.netflix.java.refactor.aspectj.RefactorMethodSignatureParser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChangeMethodInvocationTest {
    @JvmField @Rule
    val temp = TemporaryFolder()
    
    @Test
    fun typeVisitorMatchesTargetType() {
        val typeVisitor = TypeVisitor()
        
        val typeRegex = { signature: String ->
            val methodPattern = RefactorMethodSignatureParser(CommonTokenStream(AspectJLexer(ANTLRInputStream(signature))))
                .methodPattern()
            typeVisitor.visitTypePattern(methodPattern.typePattern())
        }

        assertTrue(typeRegex("..MyClass foo()").matches("com.bar.MyClass"))
        assertTrue(typeRegex("MyClass foo()").matches("MyClass"))
        assertTrue(typeRegex("com.bar.MyClass foo()").matches("com.bar.MyClass"))
        assertTrue(typeRegex("com.*.MyClass foo()").matches("com.bar.MyClass"))
    }
    
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

        rule.refactorAndFix(listOf(a, b))

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

        rule.refactorAndFix(listOf(a, b))

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