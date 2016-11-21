package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.hasElementType
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class FindFieldsTest(p: Parser): Parser by p {

    @Test
    fun findPrivateNonInheritedField() {
        val a = parse("""
            import java.util.*;
            public class A {
               private List list;
               private Set set;
            }
        """)

        val fields = a.classes[0].findFields(List::class.java)

        assertEquals(1, fields.size)
        assertEquals("list", fields[0].vars[0].name.printTrimmed())
        assertTrue(fields[0].typeExpr.type.hasElementType("java.util.List"))
    }
    
    @Test
    fun findArrayOfType() {
        val a = parse("""
            import java.util.*;
            public class A {
               private String[] s;
            }
        """)

        val fields = a.classes[0].findFields(String::class.java)

        assertEquals(1, fields.size)
        assertEquals("s", fields[0].vars[0].name.printTrimmed())
        assertTrue(fields[0].typeExpr.type.hasElementType("java.lang.String"))
    }
}

class OracleJdkFindFieldsTest: FindFieldsTest(OracleJdkParser())