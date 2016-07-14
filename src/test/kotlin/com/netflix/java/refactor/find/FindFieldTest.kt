package com.netflix.java.refactor.find

import com.netflix.java.refactor.RefactorTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FindFieldTest: RefactorTest() {
    
    @Test
    fun findField() {
        val a = java("""
            |import java.util.List;
            |public class A {
            |   List list;
            |}
        """)
        
        assertEquals("list", refactor(a).findField(List::class.java).name)
    }
}