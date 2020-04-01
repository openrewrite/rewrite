package org.openrewrite.java.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass

open class NewClassTest : JavaParser() {
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
        assertEquals("A.B", (b.initializer as J.NewClass).clazz.printTrimmed())
    }
    
    @Test
    fun concreteClassWithParams() {
        val a = parse("""
            import java.util.*;
            public class A {
                Object l = new ArrayList<String>(0);
            }
        """)

        val newClass = a.classes[0].fields[0].vars[0].initializer as J.NewClass
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

        val newClass = a.classes[0].fields[0].vars[0].initializer as J.NewClass
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

        val newClass = a.classes[0].fields[0].vars[0].initializer as J.NewClass
        assertEquals("new ArrayList < > ()", newClass.printTrimmed())
    }
}