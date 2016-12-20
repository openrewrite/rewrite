package com.netflix.rewrite.ast.visitor

import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RetrieveTreeVisitorTest : Parser by OracleJdkParser() {

    @Test
    fun retrieveTreeById() {
        val a = parse("""
            public class A {
                public void test() {
                    String s;
                }
            }
        """)

        val s = a.classes[0].methods()[0].body!!.statements[0]

        val sRetrieved = RetrieveTreeVisitor(s.id).visit(a)
        assertNotNull(sRetrieved)
        assertEquals(s, sRetrieved)
    }
}