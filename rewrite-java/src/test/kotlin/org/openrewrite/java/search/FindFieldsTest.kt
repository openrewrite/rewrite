package org.openrewrite.java.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.hasElementType

open class FindFieldsTest : JavaParser() {

    @Test
    fun findPrivateNonInheritedField() {
        val a = parse("""
            import java.util.*;
            public class A {
               private List list;
               private Set set;
            }
        """)

        val fields = a.classes[0].findFields("java.util.List")

        assertEquals(1, fields.size)
        assertEquals("list", fields[0].vars[0].name.printTrimmed())
        assertTrue(fields[0].typeExpr?.type.hasElementType("java.util.List"))
    }
    
    @Test
    fun findArrayOfType() {
        val a = parse("""
            import java.util.*;
            public class A {
               private String[] s;
            }
        """)

        val fields = a.classes[0].findFields("java.lang.String")

        assertEquals(1, fields.size)
        assertEquals("s", fields[0].vars[0].name.printTrimmed())
        assertTrue(fields[0].typeExpr?.type.hasElementType("java.lang.String"))
    }

    @Test
    fun skipsMultiCatches() {
        val a = parse("""
            import java.io.*;
            public class A {
                File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) {}
                    catch(FileNotFoundException | RuntimeException e) {}
                }
            }
        """)

        assertEquals(1, a.classes[0].findFields("java.io.File").size)
    }
}