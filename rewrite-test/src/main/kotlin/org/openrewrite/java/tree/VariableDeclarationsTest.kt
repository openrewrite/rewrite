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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTreeTest
import org.openrewrite.java.JavaTreeTest.NestingLevel.Block
import org.openrewrite.java.JavaTreeTest.NestingLevel.Class

interface VariableDeclarationsTest : JavaTreeTest {

    @Test
    fun fieldDefinition(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Class, """
            public String a = "";
        """
    )

    @Test
    fun implicitlyDeclaredLocalVariable(jp: JavaParser) {
        if (jp.majorJavaVersion < 10) {
            return
        }

        assertParsePrintAndProcess(
            jp, Block, """
                var a = "";
                var/* comment */b = "";
                /*comment*/var c = "";
                var     d = "";
                long /* yep */ i /* comments */, /*everywhere*/ j; 
            """
        )
    }

    @Test
    fun implicitlyDeclaredLocalAstValidation(jp: JavaParser) {
        if (jp.majorJavaVersion < 10) {
            return
        }
        val statements = (jp.parse("""
            import java.util.Date;
            public class Sample {
                static {
                    var a = "";
                    var /* comment */ b = 'a';
                    /*comment*/var c = new Date();
                    var     d = 1f;
                    long e, /* hello */   f = 1L;
                }
            }
        """.trimIndent())[0].classes[0].body.statements[0] as J.Block).statements
        assertThat(namedVariable(statements[0]).isImplicitlyTyped).isTrue()
        assertThat((namedVariable(statements[0]).type as JavaType.FullyQualified).fullyQualifiedName).isEqualTo("java.lang.String")
        assertThat(namedVariable(statements[1]).isImplicitlyTyped).isTrue()
        assertThat((namedVariable(statements[1]).type as JavaType.Primitive)).isEqualTo(JavaType.Primitive.Char)
        assertThat(namedVariable(statements[2]).isImplicitlyTyped).isTrue()
        assertThat((namedVariable(statements[2]).type as JavaType.FullyQualified).fullyQualifiedName).isEqualTo("java.util.Date")
        assertThat(namedVariable(statements[3]).isImplicitlyTyped).isTrue()
        assertThat((namedVariable(statements[3]).type as JavaType.Primitive)).isEqualTo(JavaType.Primitive.Float)
        assertThat(namedVariable(statements[4]).isImplicitlyTyped).isFalse()
        assertThat((namedVariable(statements[4]).type as JavaType.Primitive)).isEqualTo(JavaType.Primitive.Long)
        val secondVariable = (statements[4] as J.VariableDeclarations).variables[1]
        assertThat(secondVariable.isImplicitlyTyped).isFalse()
        assertThat(secondVariable.type as JavaType.Primitive).isEqualTo(JavaType.Primitive.Long)
        assertThat(secondVariable.prefix.comments[0].text).isEqualTo(" hello ")
        assertThat(secondVariable.prefix.comments[0].suffix).isEqualTo("   ")
    }

    fun namedVariable(statement : Statement) : J.VariableDeclarations.NamedVariable {
        return (statement as J.VariableDeclarations).variables[0]
    }
    @Test
    fun localVariableDefinition(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            String a = "";
        """
    )

    @Test
    fun fieldWithNoInitializer(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Class, """
            public String a;
        """
    )

    @Test
    fun arrayVariables(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
           int n [ ];
           String s [ ] [ ];
           int [ ] n2;
           String [ ] [ ] s2;
        """
    )

    @Test
    fun multipleDeclarationOneAssignment(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            int i , j = 0;
        """
    )

    @Test
    fun multipleDeclaration(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            Integer[] m = { 0 }, n[] = { { 0 } };
        """
    )

    /**
     * OpenJDK does NOT preserve the order of modifiers in its AST representation
     */
    @Test
    fun modifierOrdering(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Class, """
            public /* static */ final static Integer n = 0;
        """
    )

    @Test
    fun hasModifier(jp: JavaParser) {
        val a = jp.parse(
            """
            class A {
                protected static final Integer n = 0;
            }
        """
        )[0]

        val inv = a.classes[0].body.statements.filterIsInstance<J.VariableDeclarations>().first()
        assertThat(inv.modifiers).hasSize(3)
        assertTrue(inv.hasModifier(J.Modifier.Type.Protected))
        assertTrue(inv.hasModifier(J.Modifier.Type.Static))
        assertTrue(inv.hasModifier(J.Modifier.Type.Final))
    }
}
