package com.netflix.java.refactor.find

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FindFieldTest: AbstractRefactorTest() {
    
    @Test
    fun findField() {
        val a = java("""
            |import java.util.List;
            |public class A {
            |   List list;
            |}
        """)
        
        assertEquals("list", refactor(a).findFields(List::class.java).first().name)
    }
}