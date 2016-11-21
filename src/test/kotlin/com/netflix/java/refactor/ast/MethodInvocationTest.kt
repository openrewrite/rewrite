package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.*
import org.junit.Test

abstract class MethodInvocationTest(p: Parser) : Parser by p {

    val a by lazy {
        parse("""
            public class A {
                Integer m = foo ( 0, 1, 2 );
                Integer n = staticFoo ( 0 );
                Integer o = generic ( 0, 1, 2 );
                Integer p = this. < Integer > generic ( 0, 1, 2 );
                Integer q = staticFoo ( );

                public static int staticFoo(int... args) { return 0; }
                public Integer foo(Integer n, Integer... ns) { return n; }
                public <T> T generic(T n, T... ns) { return n; }
            }
        """)
    }

    val allInvs by lazy { a.fields(0..4).map { it.vars[0].initializer as Tr.MethodInvocation } }

    val inv by lazy { allInvs[0] }
    val staticInv by lazy { allInvs[1] }
    val genericInv by lazy { allInvs[2] }
    val explicitGenericInv by lazy { allInvs[3] }
    val parameterlessStaticInv by lazy { allInvs[4] }

    @Test
    fun methodInvocation() {
        // check assumptions about the call site
        assertEquals("foo", inv.name.printTrimmed())
        assertEquals("java.lang.Integer", inv.returnType().asClass()?.fullyQualifiedName)
        assertEquals(listOf(Type.Tag.Int, Type.Tag.Int, Type.Tag.Int),
                inv.args.args.filterIsInstance<Tr.Literal>().map { it.typeTag })

        val effectParams = inv.type!!.resolvedSignature.paramTypes
        assertEquals("java.lang.Integer", effectParams[0].asClass()?.fullyQualifiedName)
        assertTrue(effectParams[1].hasElementType("java.lang.Integer"))

        // for non-generic method signatures, resolvedSignature and genericSignature match
        assertEquals(inv.type!!.resolvedSignature, inv.type!!.genericSignature)

        assertEquals("A", inv.declaringType?.fullyQualifiedName)
    }

    @Test
    fun genericMethodInvocation() {
        listOf(genericInv, explicitGenericInv).forEach { test ->
            // check assumptions about the call site
            assertEquals("java.lang.Integer", test.returnType().asClass()?.fullyQualifiedName)
            assertEquals(listOf(Type.Tag.Int, Type.Tag.Int, Type.Tag.Int),
                    test.args.args.filterIsInstance<Tr.Literal>().map { it.typeTag })

            val effectiveParams = test.type!!.resolvedSignature.paramTypes
            assertEquals("java.lang.Integer", effectiveParams[0].asClass()?.fullyQualifiedName)
            assertTrue(effectiveParams[1].hasElementType("java.lang.Integer"))

            // check assumptions about the target method
            // notice how, in the case of generic arguments, the generics are concretized to match the call site
            val methType = test.type!!.genericSignature
            assertEquals("T", methType.returnType.asGeneric()?.fullyQualifiedName)
            assertEquals("T", methType.paramTypes[0].asGeneric()?.fullyQualifiedName)
            assertTrue(methType.paramTypes[1].hasElementType("T"))
        }
    }

    @Test
    fun staticMethodInvocation() {
        assertEquals("staticFoo", staticInv.name.printTrimmed())
        assertEquals("A", staticInv.declaringType?.fullyQualifiedName)
    }

    @Test
    fun format() {
        assertEquals("foo ( 0, 1, 2 )", inv.printTrimmed())
        assertEquals("staticFoo ( 0 )", staticInv.printTrimmed())
        assertEquals("this. < Integer > generic ( 0, 1, 2 )", explicitGenericInv.printTrimmed())
        assertEquals("staticFoo ( )", parameterlessStaticInv.printTrimmed())
    }

    @Test
    fun methodThatDoesNotExist() {
        val a = parse("""
            public class A {
                Integer n = doesNotExist();
            }
        """)

        val inv = a.fields()[0].vars[0].initializer as Tr.MethodInvocation
        assertNull(inv.declaringType)
        assertNull(inv.type)
        assertNull(inv.type)
    }
}