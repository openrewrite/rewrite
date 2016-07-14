package com.netflix.java.refactor.find

import com.netflix.java.refactor.RefactorTest
import org.junit.Test
import java.util.*
import kotlin.test.assertTrue

class HasTypeTest: RefactorTest() {
    
    @Test
    fun hasType() {
        val a = java("""
            |import java.util.List;
            |class A {
            |   List list;
            |}
        """)
        
        assertTrue(refactor(a).hasType(List::class.java))
    }

    @Test
    fun hasTypeBasedOnStaticImport() {
        val a = java("""
            |import static java.util.Collections.emptyList;
            |class A {
            |   Object o = emptyList();
            |}
        """)

        assertTrue(refactor(a).hasType(Collections::class.java))
    }
}