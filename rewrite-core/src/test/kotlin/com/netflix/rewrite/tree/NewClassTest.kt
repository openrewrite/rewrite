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
package com.netflix.rewrite.tree

import com.netflix.rewrite.asClass
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class NewClassTest(p: Parser): Parser by p {
    val a = """
        package a;
        public class A {
           public static class B { }
        }
    """
    
    @Test
    fun anonymousInnerClass() {
        val c = """
            import a.*;
            public class C {
                A.B anonB = new A.B() {};
            }
        """
        
        val b = parse(c, a).classes[0].fields[0].vars[0]
        assertEquals("a.A.B", b.type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun concreteInnerClass() {
        val c = """
            import a.*;
            public class C {
                A.B anonB = new A.B();
            }
        """

        val cu = parse(c, a)
        val b = cu.classes[0].fields[0].vars[0]
        assertEquals("a.A.B", b.type.asClass()?.fullyQualifiedName)
        assertEquals("A.B", (b.initializer as Tr.NewClass).clazz.printTrimmed())
    }
    
    @Test
    fun concreteClassWithParams() {
        val a = parse("""
            import java.util.*;
            public class A {
                Object l = new ArrayList<String>(0);
            }
        """)

        val newClass = a.classes[0].fields[0].vars[0].initializer as Tr.NewClass
        assertEquals(1, newClass.args.args.size)
    }

    @Test
    fun format() {
        val a = parse("""
            import java.util.*;
            public class A {
                Object l = new ArrayList< String > ( 0 ) { };
            }
        """)

        val newClass = a.classes[0].fields[0].vars[0].initializer as Tr.NewClass
        assertEquals("new ArrayList< String > ( 0 ) { }", newClass.printTrimmed())
    }

    @Test
    fun formatRawType() {
        val a = parse("""
            import java.util.*;
            public class A {
                List<String> l = new ArrayList < > ();
            }
        """)

        val newClass = a.classes[0].fields[0].vars[0].initializer as Tr.NewClass
        assertEquals("new ArrayList < > ()", newClass.printTrimmed())
    }
}