package com.netflix.java.refactor.find

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertTrue

class FindMethodTest: AbstractRefactorTest() {

    @Test
    fun findMethodCalls() {
        
        val a = java("""
            |import java.util.Collections;
            |public class A {
            |   Object o = Collections.emptyList();
            |}
        """)
        
        val m = parseJava(a).findMethodCalls("java.util.Collections emptyList()").first()
        
        assertEquals("Collections.emptyList", m.name)
        assertEquals("Collections.emptyList()", m.source)
    }

    @Test
    fun matchVarargs() {
        val a = java("""
            |public class A {
            |    public void foo(String s, Object... o) {}
            |}
        """)

        val b = java("""
            |public class B {
            |   public void test() {
            |       new A().foo("s", "a", 1);
            |   }
            |}
        """)

        assertTrue(parseJava(b, a).findMethodCalls("A foo(String, Object...)").isNotEmpty())
    }
}