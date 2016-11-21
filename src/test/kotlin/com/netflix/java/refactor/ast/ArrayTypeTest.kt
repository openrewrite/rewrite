package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert
import org.junit.Test

abstract class ArrayTypeTest(p: Parser): Parser by p {

    @Test
    fun formatArrayReturnType() {
        val a = parse("""
            package a;
            public class A {
                public String[][] foo() { return null; }
            }
        """)

        val meth = a.classes[0].methods()[0]
        Assert.assertEquals("public String[][] foo() { return null; }", meth.printTrimmed())
    }
}