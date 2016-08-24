package com.netflix.java.refactor.find

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HasImportTest: AbstractRefactorTest() {
    
    @Test
    fun hasImport() {
        val a = java("""
            |import java.util.List;
            |class A {}
        """)
        
        assertTrue(parseJava(a).hasImport(List::class.java))
        assertFalse(parseJava(a).hasImport(Set::class.java))
    }

    @Test
    fun hasStarImport() {
        val a = java("""
            |import java.util.*;
            |class A {}
        """)

        assertTrue(parseJava(a).hasImport(List::class.java))
    }

    @Test
    fun hasStarImportOnInnerClass() {
        val a = java("""
            |package a;
            |public class A {
            |   public static class B { }
            |}
        """)
        
        val c = java("""
            |import a.*;
            |public class C {
            |    A.B b = new A.B();
            |}
        """)

        assertTrue(parseJava(c, a).hasImport("a.A.B"))
        assertTrue(parseJava(c, a).hasImport("a.A"))
    }
}