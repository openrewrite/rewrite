package com.netflix.java.refactor.find

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertTrue

class FindFieldTest: AbstractRefactorTest() {
    
    @Test
    fun findField() {
        val a = java("""
            import java.util.*;
            public class A {
               List list = new ArrayList<>();
            }
        """)

        val field = parseJava(a).findFields(List::class.java).first()
        assertEquals("list", field.name)
        assertEquals("java.util.List", field.type)
    }
    
    @Test
    fun findPrivateNonInheritedField() {
        val a = java("""
            import java.util.List;
            public class A {
               private List list;
            }
        """)

        assertEquals("list", parseJava(a).findFields(List::class.java).firstOrNull()?.name)
    }
    
    @Test
    fun findInheritedField() {
        val a = java("""
            import java.util.*;
            public class A {
               List list;
               private Set set;
            }
        """)
        
        val b = java("public class B extends A { }")

        assertTrue(parseJava(b, a).findFields(List::class.java).isEmpty())

        assertEquals("list", parseJava(b, a).findFieldsIncludingInherited(List::class.java).firstOrNull()?.name)
        assertTrue(parseJava(b, a).findFieldsIncludingInherited(Set::class.java).isEmpty())
    }
}