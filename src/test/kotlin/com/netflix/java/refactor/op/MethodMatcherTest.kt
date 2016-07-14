package com.netflix.java.refactor.op

import org.junit.Assert
import org.junit.Test

class MethodMatcherTest {
    @Test
    fun matchesMethodTargetType() {
        val typeRegex = { signature: String -> MethodMatcher(signature).targetTypePattern }

        Assert.assertTrue(typeRegex("*..MyClass foo()").matches("com.bar.MyClass"))
        Assert.assertTrue(typeRegex("MyClass foo()").matches("MyClass"))
        Assert.assertTrue(typeRegex("com.bar.MyClass foo()").matches("com.bar.MyClass"))
        Assert.assertTrue(typeRegex("com.*.MyClass foo()").matches("com.bar.MyClass"))
    }

    @Test
    fun matchesMethodName() {
        val nameRegex = { signature: String -> MethodMatcher(signature).methodNamePattern }

        Assert.assertTrue(nameRegex("A foo()").matches("foo"))
        Assert.assertTrue(nameRegex("A *()").matches("foo"))
        Assert.assertTrue(nameRegex("A fo*()").matches("foo"))

        // FIXME see section 5.4 in the Definitive ANTLR4 Reference for why ambiguity in the grammar places the star with the type expression
        Assert.assertFalse(nameRegex("A *oo()").matches("foo"))
    }

    val argRegex = { signature: String -> MethodMatcher(signature).argumentPattern }

    @Test
    fun matchesArguments() {
        Assert.assertTrue(argRegex("A foo()").matches(""))
        Assert.assertTrue(argRegex("A foo(int)").matches("int"))
        Assert.assertTrue(argRegex("A foo(java.util.Map)").matches("java.util.Map"))
    }

    @Test
    fun matchesUnqualifiedJavaLangArguments() {
        Assert.assertTrue(argRegex("A foo(String)").matches("java.lang.String"))
    }

    @Test
    fun matchesArgumentsWithWildcards() {
        Assert.assertTrue(argRegex("A foo(java.util.*)").matches("java.util.Map"))
        Assert.assertTrue(argRegex("A foo(java..*)").matches("java.util.Map"))
    }

    @Test
    fun matchesArgumentsWithDotDot() {
        Assert.assertTrue(argRegex("A foo(.., int)").matches("int"))
        Assert.assertTrue(argRegex("A foo(.., int)").matches("int,int"))

        Assert.assertTrue(argRegex("A foo(int, ..)").matches("int"))
        Assert.assertTrue(argRegex("A foo(int, ..)").matches("int,int"))
    }

    @Test
    fun matchesMethodSymbolsWithVarargs() {
        // TODO implement me
    }

    @Test
    fun matchesArrayArguments() {
        Assert.assertTrue(argRegex("A foo(String[])").matches("java.lang.String[]"))
    }
}