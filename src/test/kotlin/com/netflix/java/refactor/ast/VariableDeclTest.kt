package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.*
import org.junit.Test

abstract class VariableDeclTest(p: Parser): Parser by p {
    
    @Test
    fun fieldDefinition() {
        val a = parse("""
            public class A {
                String a = "";
            }
        """)
        
        val varDecl = a.fields()[0]
        assertTrue(varDecl.typeExpr is Tr.Ident)

        val singleVar = varDecl.vars[0]
        assertEquals("a", singleVar.name.name)
        assertEquals("java.lang.String", singleVar.type.asClass()?.fullyQualifiedName)
        assertEquals((varDecl.typeExpr as Tr.Ident).type, singleVar.type)
        assertTrue(singleVar.initializer is Tr.Literal)
    }

    @Test
    fun localVariableDefinition() {
        val a = parse("""
            public class A {
                public void test() {
                    String a = "";
                }
            }
        """)

        val varDecl = a.firstMethodStatement() as Tr.VariableDecls

        val singleVar = varDecl.vars[0]
        assertEquals("java.lang.String", singleVar.type.asClass()?.fullyQualifiedName)
        assertEquals("a", singleVar.name.name)
    }

    @Test
    fun fieldWithNoInitializer() {
        val a = parse("""
            public class A {
                String a;
            }
        """)

        val varDecl = a.fields()[0]
        assertNull(varDecl.vars[0].initializer)
    }

    @Test
    fun format() {
        val a = parse("""
            public class A {
                public static int n = 0;
            }
        """)
        
        val varDecl = a.fields()[0]
        assertEquals("public static int n = 0", varDecl.printTrimmed())
    }

    @Test
    fun formatArrayVariables() {
        val a = parse("""
            |public class A {
            |   int n [ ];
            |   String s [ ] [ ];
            |   int [ ] n2;
            |   String [ ] [ ] s2;
            |}
        """)

        val (n, s, n2, s2) = a.fields(0..3)

        assertEquals("int n [ ]", n.printTrimmed())
        assertEquals("String s [ ] [ ]", s.printTrimmed())
        assertEquals("int [ ] n2", n2.printTrimmed())
        assertEquals("String [ ] [ ] s2", s2.printTrimmed())
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun multipleDeclaration() {
        val a = parse("""
            public class A {
                static {
                    Integer[] m = { 0 }, n[] = { { 0 } };
                    for(int i = 0, j = 0; i < 1; i++) { }
                }

                Integer m = 0, n = 0;
            }
        """)

        assertEquals("Integer m = 0, n = 0", a.fields()[0].printTrimmed())

        val staticInit = a.classes[0].body.statements[0] as Tr.Block<Statement>

        val (localDecl, forLoop) = staticInit.statements.subList(0, 2)
        assertEquals("Integer[] m = { 0 }, n[] = { { 0 } }", localDecl.printTrimmed())
        assertEquals("for(int i = 0, j = 0; i < 1; i++) { }", forLoop.printTrimmed())
    }

    /**
     * Oracle JDK does NOT preserve the order of modifiers in its AST representation
     */
    @Test
    fun modifierOrdering() {
        val a = parse("""
            public class A {
                public /* static */ final static Integer n = 0;
            }
        """)

        assertEquals("public /* static */ final static Integer n = 0", a.classes[0].fields()[0].printTrimmed())
    }
}