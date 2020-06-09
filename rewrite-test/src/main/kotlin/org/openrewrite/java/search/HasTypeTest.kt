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
package org.openrewrite.java.search

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

interface HasTypeTest {
    
    @Test
    fun hasType(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            class A {
               List list;
            }
        """)

        assertTrue(a.classes[0].hasType("java.util.List"))
    }

    @Test
    fun hasTypeBasedOnStaticImport(jp: JavaParser) {
        val a = jp.parse("""
            import static java.util.Collections.emptyList;
            class A {
               Object o = emptyList();
            }
        """)

        assertTrue(a.classes[0].hasType("java.util.Collections"))
    }
    
    @Test
    fun hasTypeBasedOnStaticChainedCalls(jp: JavaParser) {
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

        assertTrue(jp.parse(b, a).classes[0].hasType("a.A"))
    }

    @Test
    fun hasTypeInLocalVariable(jp: JavaParser) {
        val a = jp.parse("""
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
    fun unresolvableMethodSymbol(jp: JavaParser) {
        val a = jp.parse("""
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
