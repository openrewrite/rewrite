/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass

interface NewClassTest {
    companion object {
        val a = """
            package a;
            public class A {
               public static class B { }
            }
        """
    }

    @Test
    fun innerClassNew(jp: JavaParser) {
        val a = """
            class A {
                void bar(B b, C c) {}
                void foo() {
                    bar(this . new B(), this.new C());
                }
                class B { }
                class C { }
            }
        """.trimIndent()

        assertEquals(a, jp.parse(a).first().printTrimmed())
    }


    @Test
    fun anonymousInnerClass(jp: JavaParser) {
        val c = """
            import a.*;
            public class C {
                A.B anonB = new A.B() {};
            }
        """
        
        val b = jp.parse(c, a)[0].classes[0].fields[0].vars[0]
        assertEquals("a.A.B", b.type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun concreteInnerClass(jp: JavaParser) {
        val c = """
            import a.*;
            public class C {
                A.B anonB = new A.B();
            }
        """

        val cu = jp.parse(c, a)[0]
        val b = cu.classes[0].fields[0].vars[0]
        assertEquals("a.A.B", b.type.asClass()?.fullyQualifiedName)
        assertEquals("A.B", (b.initializer as J.NewClass).clazz?.printTrimmed())
    }
    
    @Test
    fun concreteClassWithParams(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            public class A {
                Object l = new ArrayList<String>(0);
            }
        """)[0]

        val newClass = a.classes[0].fields[0].vars[0].initializer as J.NewClass
        assertEquals(1, newClass.args?.args?.size)
    }

    @Test
    fun format(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            public class A {
                Object l = new ArrayList< String > ( 0 ) { };
            }
        """)[0]

        val newClass = a.classes[0].fields[0].vars[0].initializer as J.NewClass
        assertEquals("new ArrayList< String > ( 0 ) { }", newClass.printTrimmed())
    }

    @Test
    fun formatRawType(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.*;
            public class A {
                List<String> l = new ArrayList < > ();
            }
        """)[0]

        val newClass = a.classes[0].fields[0].vars[0].initializer as J.NewClass
        assertEquals("new ArrayList < > ()", newClass.printTrimmed())
    }
}
