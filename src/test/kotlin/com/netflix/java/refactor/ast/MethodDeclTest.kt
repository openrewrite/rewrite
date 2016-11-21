package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

abstract class MethodDeclTest(p: Parser): Parser by p {

    @Test
    fun constructor() {
        val a = parse("""
            package a;
            public class A {
                public A() { }
            }
        """)

        assertNull(a.classes[0].methods()[0].returnTypeExpr)
    }

    @Test
    fun methodDecl() {
        val a = parse("""
            public class A {
                public <P, R> R foo(P p, String s, String... args) {
                    return null;
                }
            }
        """)
        
        val meth = a.classes[0].methods()[0]
        assertEquals("foo", meth.name.name)
        assertEquals(3, meth.params.params.size)
        assertEquals(1, meth.body!!.statements.size)
        assertEquals("R", ((meth.returnTypeExpr as Tr.Ident).type as Type.GenericTypeVariable).fullyQualifiedName)
    }

    @Test
    fun interfaceMethodDecl() {
        val aSrc = """
            |public interface A {
            |    String getName() ;
            |}
        """.trimMargin()

        assertEquals(aSrc, parse(aSrc).printTrimmed())
    }
    
    @Test
    fun format() {
        val a = parse("""
            public class A {
                public < P > P foo(P p, String s, String ... args)  throws Exception { return p; }
            }
        """)

        val meth = a.classes[0].methods()[0]
        assertEquals("public < P > P foo(P p, String s, String ... args)  throws Exception { return p; }", meth.printTrimmed())
    }

    @Test
    fun formatDefaultMethod() {
        val a = parse("""
            public interface A {
                default int foo() { return 0; }
            }
        """)

        val meth = a.classes[0].methods()[0]
        assertEquals("default int foo() { return 0; }", meth.printTrimmed())
    }

    @Test
    fun formatConstructor() {
        val a = parse("""
            package a;
            public class A {
                public A() { }
            }
        """)

        val meth = a.classes[0].methods()[0]
        assertEquals("public A() { }", meth.printTrimmed())
    }
}
