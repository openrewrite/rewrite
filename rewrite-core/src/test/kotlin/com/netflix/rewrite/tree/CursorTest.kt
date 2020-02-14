package com.netflix.rewrite.tree

import com.netflix.rewrite.parse.OpenJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

open class CursorTest : Parser by OpenJdkParser() {
    @Test
    fun inSameNameScope() {
        val a = parse("""
            public class A {
                int n;
                
                public void foo(int n1) {
                    for(int n2 = 0;;) {
                    }
                }
            }
        """.trimIndent())

        fun Tree.cursor() = a.cursor(this)

        val fieldScope = a.classes[0].fields[0].cursor()!!
        val methodParamScope = a.classes[0].methods[0].params.params[0].cursor()!!
        val forInitScope = a.classes[0].methods[0].body!!.statements.filterIsInstance<Tr.ForLoop>()[0].control.init.cursor()!!

        assertTrue(fieldScope.isInSameNameScope(methodParamScope))
        assertFalse(methodParamScope.isInSameNameScope(fieldScope))

        assertTrue(fieldScope.isInSameNameScope(forInitScope))
    }
}