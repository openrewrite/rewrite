package org.openrewrite.java.search

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
