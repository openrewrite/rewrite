package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
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
        
        val b = parse(c, whichDependsOn = a).fields()[0].vars[0]
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

        val cu = parse(c, whichDependsOn = a)
        val b = cu.fields()[0].vars[0]
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

        val newClass = a.fields()[0].vars[0].initializer as Tr.NewClass
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

        val newClass = a.fields()[0].vars[0].initializer as Tr.NewClass
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

        val newClass = a.fields()[0].vars[0].initializer as Tr.NewClass
        assertEquals("new ArrayList < > ()", newClass.printTrimmed())
    }
}