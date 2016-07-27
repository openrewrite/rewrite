package com.netflix.java.refactor.find

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FindFieldTest: AbstractRefactorTest() {
    
    @Test
    fun findField() {
        val a = java("""
            import java.util.List;
            public class A {
               List list;
            }
        """)
        
        assertEquals("list", parseJava(a).findFields(List::class.java).firstOrNull()?.name)
    }
    
    @Test
    fun findFieldOnParentClass() {
        val a = java("""
            import java.util.List;
            public class A {
               List list;
            }
        """)
        
        val b = java("public class B extends A { }")

        assertEquals("list", parseJava(b, a).findFields(List::class.java).firstOrNull()?.name)
    }
}