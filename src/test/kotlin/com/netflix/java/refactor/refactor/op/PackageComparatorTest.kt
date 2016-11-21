package com.netflix.java.refactor.refactor.op

import org.junit.Assert.assertEquals
import org.junit.Test

class PackageComparatorTest {
    
    @Test
    fun comparePackages() {
        val comp = PackageComparator()

        assertEquals(-1, comp.compare("a", "b"))
        assertEquals(-1, comp.compare("a.a", "a.b"))
        assertEquals(1, comp.compare("b", "a"))
        assertEquals(1, comp.compare("a.b", "a.a"))
        assertEquals(0, comp.compare("a", "a"))
        assertEquals(1, comp.compare("a.a", "a"))
        assertEquals(-1, comp.compare("a", "a.a"))
    }
}