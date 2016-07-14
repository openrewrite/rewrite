package com.netflix.java.refactor

import org.junit.Test
import kotlin.test.assertEquals

class TypeUtilsTest {
    
    @Test
    fun packageOwner() {
        assertEquals("com.foo", packageOwner("com.foo.Foo"))
        assertEquals("com.foo", packageOwner("com.foo.Foo.Bar"))
    }

    @Test
    fun className() {
        assertEquals("Foo", className("com.foo.Foo"))
        assertEquals("Foo.Bar", className("com.foo.Foo.Bar"))
    }
}