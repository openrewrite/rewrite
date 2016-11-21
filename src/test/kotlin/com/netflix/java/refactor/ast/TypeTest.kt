package com.netflix.java.refactor.ast

import org.junit.Assert.assertEquals
import org.junit.Test

class TypeTest {
    val cache = TypeCache.new()

    @Test
    fun innerClassType() {
        val t = Type.Class.build(cache, "com.foo.Foo.Bar")
        assertEquals("com.foo.Foo", t.owner.asClass()?.fullyQualifiedName)
        assertEquals("com.foo", t.owner.asClass()?.owner.asPackage()?.fullName)
    }

    @Test
    fun classType() {
        val t = Type.Class.build(cache, "com.foo.Foo")
        assertEquals("com.foo", t.owner.asPackage()?.fullName)
    }
}