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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class MethodDeclTest : JavaParser() {

    @Test
    fun constructor() {
        val a = parse("""
            package a;
            public class A {
                public A() { }
            }
        """)

        assertNull(a.classes[0].methods[0].returnTypeExpr)
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
        
        val meth = a.classes[0].methods[0]
        assertEquals("foo", meth.simpleName)
        assertEquals(3, meth.params.params.size)
        assertEquals(1, meth.body!!.statements.size)
        assertEquals("R", ((meth.returnTypeExpr as J.Ident).type as JavaType.GenericTypeVariable).fullyQualifiedName)

        assertTrue(meth.hasModifier("public"))
    }

    @Test
    fun interfaceMethodDecl() {
        val aSrc = """
            public interface A {
                String getName() ;
            }
        """.trimIndent()

        assertEquals(aSrc, parse(aSrc).printTrimmed())
    }
    
    @Test
    fun format() {
        val a = parse("""
            public class A {
                public < P > P foo(P p, String s, String ... args)  throws Exception { return p; }
            }
        """)

        val meth = a.classes[0].methods[0]
        assertEquals("public < P > P foo(P p, String s, String ... args)  throws Exception { return p; }", meth.printTrimmed())
    }

    @Test
    fun formatDefaultMethod() {
        val a = parse("""
            public interface A {
                default int foo() { return 0; }
            }
        """)

        val meth = a.classes[0].methods[0]
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

        val meth = a.classes[0].methods[0]
        assertEquals("public A() { }", meth.printTrimmed())
    }

    @Test
    fun nativeModifier() {
        val a = parse("""
            public class A {
                public native void foo();
            }
        """)

        val meth = a.classes[0].methods[0]
        assertEquals("public native void foo()", meth.printTrimmed())
    }
}
