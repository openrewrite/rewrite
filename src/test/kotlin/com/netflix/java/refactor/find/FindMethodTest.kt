package com.netflix.java.refactor.find

import com.netflix.java.refactor.RefactorTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FindMethodTest: RefactorTest() {

    @Test
    fun findMethod() {
        
        val a = java("""
            |import java.util.Collections;
            |public class A {
            |   Object o = Collections.emptyList();
            |}
        """)
        
        val m = refactor(a).findMethod("java.util.Collections emptyList()")
        
        assertEquals("Collections.emptyList", m.name)
    }
}