package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class UnaryTest(p: Parser): Parser by p {
    
    @Test
    fun negation() {
        val a = parse("""
            public class A {
                boolean b = !(1 == 2);
            }
        """)

        val unary = a.fields()[0].vars[0].initializer as Tr.Unary
        assertTrue(unary.operator is Tr.Unary.Operator.Not)
        assertTrue(unary.expr is Tr.Parentheses<*>)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                int i = 0;
                int j = ++i;
                int k = i++;
            }
        """)

        val (prefix, postfix) = a.classes[0].fields().subList(1, 3).map { it.vars[0].initializer as Tr.Unary }
        assertEquals("++i", prefix.printTrimmed())
        assertEquals("i++", postfix.printTrimmed())
    }
}