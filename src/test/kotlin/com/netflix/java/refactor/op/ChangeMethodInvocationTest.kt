package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChangeMethodInvocationTest {
    @JvmField @Rule
    val temp = TemporaryFolder()
    
    @Test
    fun matchesMethodTargetType() {
        val typeRegex = { signature: String -> ChangeMethodInvocation(signature, RefactorRule()).targetTypePattern }

        assertTrue(typeRegex("*..MyClass foo()").matches("com.bar.MyClass"))
        assertTrue(typeRegex("MyClass foo()").matches("MyClass"))
        assertTrue(typeRegex("com.bar.MyClass foo()").matches("com.bar.MyClass"))
        assertTrue(typeRegex("com.*.MyClass foo()").matches("com.bar.MyClass"))
    }

    @Test
    fun matchesMethodName() {
        val nameRegex = { signature: String -> ChangeMethodInvocation(signature, RefactorRule()).methodNamePattern }
        
        assertTrue(nameRegex("A foo()").matches("foo"))
        assertTrue(nameRegex("A *()").matches("foo"))
        assertTrue(nameRegex("A fo*()").matches("foo"))

        // FIXME see section 5.4 in the Definitive ANTLR4 Reference for why ambiguity in the grammar places the star with the type expression
        assertFalse(nameRegex("A *oo()").matches("foo"))
    }
    
    @Test
    fun matchesArguments() {
        val argRegex = { signature: String -> ChangeMethodInvocation(signature, RefactorRule()).argumentPattern }

        assertTrue(argRegex("A foo()").matches(""))
        assertTrue(argRegex("A foo(int)").matches("int"))
        assertTrue(argRegex("A foo(String)").matches("java.lang.String"))
        assertTrue(argRegex("A foo(java.util.Map)").matches("java.util.Map"))
        assertTrue(argRegex("A foo(java.util.*)").matches("java.util.Map"))
        assertTrue(argRegex("A foo(java..*)").matches("java.util.Map"))
        
        assertTrue(argRegex("A foo(.., int)").matches("int"))
        assertTrue(argRegex("A foo(.., int)").matches("int,int"))

        assertTrue(argRegex("A foo(int, ..)").matches("int"))
        assertTrue(argRegex("A foo(int, ..)").matches("int,int"))
    }
    
    @Test
    fun matchesMethodSymbolsWithVarargs() {
    }
    
    @Test
    fun matchesArrayArguments() {
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