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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.asClass
import org.openrewrite.java.fields
import org.openrewrite.java.firstMethodStatement

interface VariableDeclsTest {
    
    @Test
    fun fieldDefinition(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                public String a = "";
            }
        """)[0]
        
        val varDecl = a.classes[0].fields[0]
        assertTrue(varDecl.typeExpr is J.Ident)

        val singleVar = varDecl.vars[0]
        assertEquals("a", singleVar.simpleName)
        assertEquals("java.lang.String", singleVar.type.asClass()?.fullyQualifiedName)
        assertEquals((varDecl.typeExpr as J.Ident).type, singleVar.type)
        assertTrue(singleVar.initializer is J.Literal)

        assertTrue(varDecl.hasModifier("public"))
    }

    @Test
    fun localVariableDefinition(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                public void test() {
                    String a = "";
                }
            }
        """)[0]

        val varDecl = a.firstMethodStatement() as J.VariableDecls

        val singleVar = varDecl.vars[0]
        assertEquals("java.lang.String", singleVar.type.asClass()?.fullyQualifiedName)
        assertEquals("a", singleVar.simpleName)
    }

    @Test
    fun fieldWithNoInitializer(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                String a;
            }
        """)[0]

        val varDecl = a.classes[0].fields[0]
        assertNull(varDecl.vars[0].initializer)
    }

    @Test
    fun format(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                public static int n = 0;
            }
        """)[0]
        
        val varDecl = a.classes[0].fields[0]
        assertEquals("public static int n = 0", varDecl.printTrimmed())
    }

    @Test
    fun formatArrayVariables(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
               int n [ ];
               String s [ ] [ ];
               int [ ] n2;
               String [ ] [ ] s2;
            }
        """)[0]

        val (n, s, n2, s2) = a.fields(0..3)

        assertEquals("int n [ ]", n.printTrimmed())
        assertEquals("String s [ ] [ ]", s.printTrimmed())
        assertEquals("int [ ] n2", n2.printTrimmed())
        assertEquals("String [ ] [ ] s2", s2.printTrimmed())
    }

    @Test
    fun multipleDeclarationOneAssignment(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                int i , j = 0;
            }
        """)[0]

        assertEquals("int i , j = 0", a.classes[0].fields[0].printTrimmed())
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun multipleDeclaration(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                static {
                    Integer[] m = { 0 }, n[] = { { 0 } };
                    for(int i = 0, j = 0; i < 1; i++) { }
                }

                Integer m = 0, n = 0;
            }
        """)[0]

        assertEquals("Integer m = 0, n = 0", a.classes[0].fields[0].printTrimmed())

        val staticInit = a.classes[0].body.statements[0] as J.Block<Statement>

        val (localDecl, forLoop) = staticInit.statements.subList(0, 2)
        assertEquals("Integer[] m = { 0 }, n[] = { { 0 } }", localDecl.printTrimmed())
        assertEquals("for(int i = 0, j = 0; i < 1; i++) { }", forLoop.printTrimmed())
    }

    /**
     * OpenJDK does NOT preserve the order of modifiers in its AST representation
     */
    @Test
    fun modifierOrdering(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                public /* static */ final static Integer n = 0;
            }
        """)[0]

        assertEquals("public /* static */ final static Integer n = 0", a.classes[0].fields[0].printTrimmed())
    }
}
