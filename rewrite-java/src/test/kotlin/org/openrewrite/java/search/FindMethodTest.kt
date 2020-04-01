package org.openrewrite.java.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass

open class FindMethodTest : JavaParser() {

    @Test
    fun findStaticMethodCalls() {
        val a = parse("""
            import java.util.Collections;
            public class A {
               Object o = Collections.emptyList();
            }
        """)
        
        val m = a.classes[0].findMethodCalls("java.util.Collections emptyList()").first()
        
        assertEquals("emptyList", m.simpleName)
        assertEquals("Collections.emptyList()", m.printTrimmed())
    }

    @Test
    fun findStaticallyImportedMethodCalls() {
        val a = parse("""
            import static java.util.Collections.emptyList;
            public class A {
               Object o = emptyList();
            }
        """)

        val m = a.classes[0].findMethodCalls("java.util.Collections emptyList()").firstOrNull()
        assertEquals("java.util.Collections", m?.type?.declaringType.asClass()?.fullyQualifiedName)
    }

    @Test
    fun matchVarargs() {
        val a = """
            public class A {
                public void foo(String s, Object... o) {}
            }
        """

        val b = """
            public class B {
               public void test() {
                   new A().foo("s", "a", 1);
               }
            }
        """

        assertTrue(parse(b, a).classes[0].findMethodCalls("A foo(String, Object...)").isNotEmpty())
    }

    @Test
    fun matchOnInnerClass() {
        val b = """
            public class B {
               public static class C {
                   public void foo() {}
               }
            }
        """

        val a = """
            public class A {
               void test() {
                   new B.C().foo();
               }
            }
        """

        assertEquals(1, parse(a, b).classes[0].findMethodCalls("B.C foo()").size)
    }
}
