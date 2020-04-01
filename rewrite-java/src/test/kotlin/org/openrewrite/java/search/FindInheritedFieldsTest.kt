package org.openrewrite.java.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class FindInheritedFieldsTest : JavaParser() {

    @Test
    fun findInheritedField() {
        val a = """
            import java.util.*;
            public class A {
               protected List list;
               private Set set;
            }
        """

        val b = parse("public class B extends A { }", a)

        assertEquals("list", b.classes[0].findInheritedFields("java.util.List").firstOrNull()?.name)

        // the Set field is not considered to be inherited because it is private
        val fields = b.classes[0].findInheritedFields("java.util.Set")
        assertTrue(fields.isEmpty())
    }

    @Test
    fun findArrayOfType() {
        val a = """
            public class A {
               String[] s;
            }
        """

        val b = parse("public class B extends A { }", a)

        val fields = b.classes[0].findInheritedFields("java.lang.String")
        assertEquals(1, fields.size)
        assertEquals("s", fields[0].name)

        assertTrue(b.classes[0].findInheritedFields("java.util.Set").isEmpty())
    }
}
