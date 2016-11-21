package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class InstanceOfTest(p: Parser): Parser by p {

    val a by lazy {
        parse("""
            |public class A {
            |    Object o;
            |    public void test() {
            |        boolean b = o instanceof String;
            |    }
            |}
        """)
    }

    val variable by lazy { a.firstMethodStatement() as Tr.VariableDecls }
    val instanceof by lazy { variable.vars[0].initializer as Tr.InstanceOf }

    @Test
    fun instanceOf() {
        assertEquals("java.lang.String", (instanceof.clazz as Tr.Ident).type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun format() {
        assertEquals("o instanceof String", instanceof.printTrimmed())
    }
}