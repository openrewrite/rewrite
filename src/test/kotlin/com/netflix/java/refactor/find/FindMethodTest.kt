package com.netflix.java.refactor.find

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FindMethodTest: AbstractRefactorTest() {

    @Test
    fun findMethod() {
        
        val a = java("""
            |import java.util.Collections;
            |public class A {
            |   Object o = Collections.emptyList();
            |}
        """)
        
        val m = parseJava(a).findMethods("java.util.Collections emptyList()").first()
        
        assertEquals("Collections.emptyList", m.name)
    }
}