package org.openrewrite.java

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.tree.JavaType

class MethodMatcherTest {
    @Test
    fun matchesSuperclassType() {
        assertTrue(MethodMatcher("Object equals(Object)").matchesTargetType(JavaType.Class.build("java.lang.String")))
        assertFalse(MethodMatcher("String equals(String)").matchesTargetType(JavaType.Class.build("java.lang.Object")))
    }

    @Test
    fun matchesMethodTargetType() {
        val typeRegex = { signature: String -> MethodMatcher(signature).targetTypePattern.toRegex() }

        assertTrue(typeRegex("*..MyClass foo()").matches("com.bar.MyClass"))
        assertTrue(typeRegex("MyClass foo()").matches("MyClass"))
        assertTrue(typeRegex("com.bar.MyClass foo()").matches("com.bar.MyClass"))
        assertTrue(typeRegex("com.*.MyClass foo()").matches("com.bar.MyClass"))
    }

    @Test
    fun matchesMethodName() {
        val nameRegex = { signature: String -> MethodMatcher(signature).methodNamePattern.toRegex() }

        assertTrue(nameRegex("A foo()").matches("foo"))
        assertTrue(nameRegex("A *()").matches("foo"))
        assertTrue(nameRegex("A fo*()").matches("foo"))
        assertTrue(nameRegex("A *oo()").matches("foo"))
    }

    val argRegex = { signature: String -> MethodMatcher(signature).argumentPattern.toRegex() }

    @Test
    fun matchesArguments() {
        assertTrue(argRegex("A foo()").matches(""))
        assertTrue(argRegex("A foo(int)").matches("int"))
        assertTrue(argRegex("A foo(java.util.Map)").matches("java.util.Map"))
    }

    @Test
    fun matchesUnqualifiedJavaLangArguments() {
        assertTrue(argRegex("A foo(String)").matches("java.lang.String"))
    }

    @Test
    fun matchesArgumentsWithWildcards() {
        assertTrue(argRegex("A foo(java.util.*)").matches("java.util.Map"))
        assertTrue(argRegex("A foo(java..*)").matches("java.util.Map"))
    }

    @Test
    fun matchesArgumentsWithDotDot() {
        assertTrue(argRegex("A foo(.., int)").matches("int"))
        assertTrue(argRegex("A foo(.., int)").matches("int,int"))

        assertTrue(argRegex("A foo(int, ..)").matches("int"))
        assertTrue(argRegex("A foo(int, ..)").matches("int,int"))

        assertTrue(argRegex("A foo(..)").matches(""))
        assertTrue(argRegex("A foo(..)").matches("int"))
        assertTrue(argRegex("A foo(..)").matches("int,int"))
    }

    @Test
    fun matchesSuperclassArgumentTypes() {
        assertTrue(MethodMatcher("Object equals(Object)").matchesTargetType(JavaType.Class.build("java.lang.String")))
        assertFalse(MethodMatcher("String equals(String)").matchesTargetType(JavaType.Class.build("java.lang.Object")))
    }

    @Test
    fun matchesMethodSymbolsWithVarargs() {
        argRegex("A foo(String, Object...)").matches("String,Object[]")
    }

    @Test
    fun dotDotMatchesArrayArgs() {
        argRegex("A foo(..)").matches("String,Object[]")
    }

    @Test
    fun matchesArrayArguments() {
        assertTrue(argRegex("A foo(String[])").matches("java.lang.String[]"))
    }
}