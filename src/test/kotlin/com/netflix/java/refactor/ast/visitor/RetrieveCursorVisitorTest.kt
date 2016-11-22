package com.netflix.java.refactor.ast.visitor

import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RetrieveCursorVisitorTest: Parser by OracleJdkParser()  {

    @Test
    fun retrieveCursor() {
        val a = parse("""
            public class A {
                public void test() {
                    String s;
                }
            }
        """)

        val s = a.classes[0].methods()[0].body!!.statements[0]

        val cursor = a.cursor(s)
        assertNotNull(cursor)
        assertEquals("CompilationUnit,ClassDecl,Block,MethodDecl,Block,VariableDecls",
                cursor!!.path.map { it.javaClass.simpleName }.joinToString(","))
    }
}