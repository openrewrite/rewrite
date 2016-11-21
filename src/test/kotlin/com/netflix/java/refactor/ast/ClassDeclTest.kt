package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

abstract class ClassDeclTest(p: Parser): Parser by p {
    
    @Test
    fun multipleClassDeclarationsInOneCompilationUnit() {
        val a = parse("""
            public class A {}
            class B {}
        """)

        assertEquals(listOf("A", "B"), a.classes.map { it.name.name }.sorted())
    }
    
    @Test
    fun fields() {
        val a = parse("""
            import java.util.*;
            public class A {
                List l;
            }
        """)

        assertEquals(1, a.classes[0].fields().size)
    }

    @Test
    fun methods() {
        val a = parse("""
            public class A {
                public void fun() {}
            }
        """)

        assertEquals(1, a.classes[0].methods().size)
    }
    
    @Test
    fun implements() {
        val b = "public interface B {}"
        val a = "public class A implements B {}"
        
        assertEquals(1, parse(a, whichDependsOn = b).classes[0].implements.size)
    }

    @Test
    fun extends() {
        val b = "public class B {}"
        val a = "public class A extends B {}"

        val aClass = parse(a, whichDependsOn = b).classes[0]
        assertNotNull(aClass.extends)
    }

    @Test
    fun format() {
        val b = "public class B<T> {}"
        val a = "@Deprecated public class A < T > extends B < T > {}"
        assertEquals(a, parse(a, whichDependsOn = b).classes[0].printTrimmed())
    }

    @Test
    fun formatInterface() {
        val b = "public interface B {}"
        val a = "public interface A extends B {}"
        assertEquals(a, parse(a, whichDependsOn = b).classes[0].printTrimmed())
    }

    @Test
    fun formatAnnotation() {
        val a = "public @interface Produces { }"
        assertEquals(a, parse(a).classes[0].printTrimmed())
    }

    @Test
    fun enumWithParameters() {
        val aSrc = """
            |public enum A {
            |    ONE(1),
            |    TWO(2);
            |
            |    A(int n) {}
            |}
        """.trimMargin()

        val a = parse(aSrc)

        Assert.assertTrue(a.classes[0].kind is Tr.ClassDecl.Kind.Enum)
        assertEquals("ONE(1),\nTWO(2);", a.classes[0].enumValues()?.printTrimmed())
        assertEquals(aSrc, a.printTrimmed())
    }

    @Test
    fun enumWithoutParameters() {
        val a = parse("public enum A { ONE, TWO }")
        assertEquals("public enum A { ONE, TWO }", a.classes[0].printTrimmed())
        assertEquals("ONE, TWO", a.classes[0].enumValues()?.printTrimmed())
    }

    @Test
    fun enumUnnecessarilyTerminatedWithSemicolon() {
        val a = parse("public enum A { ONE ; }")
        assertEquals("{ ONE ; }", a.classes[0].body.printTrimmed())
    }

    @Test
    fun enumWithEmptyParameters() {
        val a = parse("public enum A { ONE ( ), TWO ( ) }")
        assertEquals("public enum A { ONE ( ), TWO ( ) }", a.classes[0].printTrimmed())
        assertEquals("ONE ( ), TWO ( )", a.classes[0].enumValues()?.printTrimmed())
    }

    /**
     * Oracle JDK does NOT preserve the order of modifiers in its AST representation
     */
    @Test
    fun modifierOrdering() {
        val a = parse("public /* abstract */ final abstract class A {}")
        assertEquals("public /* abstract */ final abstract class A {}", a.printTrimmed())
    }

    @Test
    fun innerClass() {
        val aSrc = """
            |public class A {
            |    public enum B {
            |        ONE,
            |        TWO
            |    }
            |
            |    private B b;
            |}
        """.trimMargin()

        assertEquals(aSrc, parse(aSrc).printTrimmed())
    }
}