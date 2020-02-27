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
package org.openrewrite.java.visitor.search

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

open class HasTypeTest : JavaParser() {
    
    @Test
    fun hasType() {
        val a = parse("""
            import java.util.List;
            class A {
               List list;
            }
        """)

        assertTrue(a.classes[0].hasType("java.util.List"))
    }

    @Test
    fun hasTypeBasedOnStaticImport() {
        val a = parse("""
            import static java.util.Collections.emptyList;
            class A {
               Object o = emptyList();
            }
        """)

        assertTrue(a.classes[0].hasType("java.util.Collections"))
    }
    
    @Test
    fun hasTypeBasedOnStaticChainedCalls() {
        val a = """
            package a;
            public class A { 
                public static A none() { return null; }
            }
        """
        
        val b = """
            import static a.A.none;
            class B {
               Object o = none().none().none();
            }
        """

        assertTrue(parse(b, a).classes[0].hasType("a.A"))
    }

    @Test
    fun hasTypeInLocalVariable() {
        val a = parse("""
            import java.util.List;
            class A {
               public void test() {
                   List list;
               }
            }
        """)

        assertTrue(a.classes[0].hasType("java.util.List"))
    }

    @Test
    fun unresolvableMethodSymbol() {
        val a = parse("""
            public class B {
                public static void baz() {
                    // the parse tree inside this anonymous class will be un-attributed because
                    // A is not a resolvable symbol
                    A a = new A() {
                        @Override public void foo() {
                            bar();
                        }
                    };
                }
                public static void bar() {}
            }
        """)

        a.classes[0].hasType("DoesNotMatter") // doesn't throw an exception
    }
}
