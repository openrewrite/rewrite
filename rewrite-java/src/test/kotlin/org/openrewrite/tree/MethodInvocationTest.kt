/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.tree

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.*

open class MethodInvocationTest : JavaParser() {

    val a: J.CompilationUnit by lazy {
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

    private val allInvs by lazy { a.fields(0..4).map { it.vars[0].initializer as J.MethodInvocation } }

    private val inv by lazy { allInvs[0] }
    private val staticInv by lazy { allInvs[1] }
    private val genericInv by lazy { allInvs[2] }
    private val explicitGenericInv by lazy { allInvs[3] }
    private val parameterlessStaticInv by lazy { allInvs[4] }

    @Test
    fun methodInvocation() {
        // check assumptions about the call site
        assertEquals("foo", inv.name.printTrimmed())
        assertEquals("java.lang.Integer", inv.returnType.asClass()?.fullyQualifiedName)
        assertEquals(listOf(Type.Primitive.Int, Type.Primitive.Int, Type.Primitive.Int),
                inv.args.args.filterIsInstance<J.Literal>().map { it.type })

        val effectParams = inv.type!!.resolvedSignature!!.paramTypes
        assertEquals("java.lang.Integer", effectParams[0].asClass()?.fullyQualifiedName)
        assertTrue(effectParams[1].hasElementType("java.lang.Integer"))

        // for non-generic method signatures, resolvedSignature and genericSignature match
        assertEquals(inv.type!!.resolvedSignature, inv.type!!.genericSignature)

        assertEquals("A", inv.type?.declaringType?.fullyQualifiedName)
    }

    @Test
    fun genericMethodInvocation() {
        listOf(genericInv, explicitGenericInv).forEach { test: J.MethodInvocation ->
            // check assumptions about the call site
            assertEquals("java.lang.Integer", test.returnType.asClass()?.fullyQualifiedName)
            assertEquals(listOf(Type.Primitive.Int, Type.Primitive.Int, Type.Primitive.Int),
                    test.args.args.filterIsInstance<J.Literal>().map { it.type })

            val effectiveParams = test.type!!.resolvedSignature!!.paramTypes
            assertEquals("java.lang.Integer", effectiveParams[0].asClass()?.fullyQualifiedName)
            assertTrue(effectiveParams[1].hasElementType("java.lang.Integer"))

            // check assumptions about the target method
            // notice how, in the case of generic arguments, the generics are concretized to match the call site
            val methType = test.type!!.genericSignature!!
            assertEquals("T", methType.returnType.asGeneric()?.fullyQualifiedName)
            assertEquals("T", methType.paramTypes[0].asGeneric()?.fullyQualifiedName)
            assertTrue(methType.paramTypes[1].hasElementType("T"))
        }
    }

    @Test
    fun staticMethodInvocation() {
        assertEquals("staticFoo", staticInv.name.printTrimmed())
        assertEquals("A", staticInv.type?.declaringType?.fullyQualifiedName)
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

        val inv = a.classes[0].fields[0].vars[0].initializer as J.MethodInvocation
        assertNull(inv.type?.declaringType)
        assertNull(inv.type)
        assertNull(inv.type)
    }
}