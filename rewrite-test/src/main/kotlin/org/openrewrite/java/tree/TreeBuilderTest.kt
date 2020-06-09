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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Formatting.EMPTY
import org.openrewrite.java.*

interface TreeBuilderTest {
    @Test
    fun buildSnippet(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            public class A {
                int n = 0;
                
                void foo(String m, List<String> others) {
                }
            }
        """.trimIndent())

        val method = a.classes[0].methods[0]
        val methodBodyCursor = RetrieveCursor(method.body).visit(a)
        val paramName = (method.params.params[0] as J.VariableDecls).vars[0].name

        val snippets = TreeBuilder.buildSnippet<Statement>(
                jp, a, methodBodyCursor,
                "others.add(${paramName.printTrimmed()});")

        assertTrue(snippets[0] is J.MethodInvocation)
    }

    @Test
    fun injectSnippetIntoMethod(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            public class A {
                int n = 0;
                
                void foo(String m, List<String> others) {
                }
            }
        """.trimIndent())

        val method = a.classes[0].methods[0]
        val methodBodyCursor = RetrieveCursor(method.body).visit(a)
        val paramName = (method.params.params[0] as J.VariableDecls).vars[0].name.printTrimmed()

        val snippets = TreeBuilder.buildSnippet<Statement>(
                jp, a, methodBodyCursor, """
                    others.add(${paramName});
                    if(others.contains(${paramName})) {
                        others.remove(${paramName});
                    }
                """.trimIndent())

        val fixed = a.refactor().visit(object : JavaRefactorVisitor() {
            override fun visitMethod(method: J.MethodDecl): J = method.withBody(method.body!!.withStatements(snippets))
        }).fix().fixed

        assertRefactored(fixed, """
            import java.util.List;
            public class A {
                int n = 0;
                
                void foo(String m, List<String> others) {
                    others.add(m);
                    if(others.contains(m)) {
                        others.remove(m);
                    }
                }
            }
        """)
    }

    @Test
    fun buildFullyQualifiedClassName(jp: JavaParser) {
        val name = TreeBuilder.buildName("java.util.List", EMPTY) as J.FieldAccess

        assertEquals("java.util.List", name.printTrimmed())
        assertEquals("List", name.simpleName)
    }

    @Test
    fun buildFullyQualifiedInnerClassName(jp: JavaParser) {
        val name = TreeBuilder.buildName("a.Outer.Inner", EMPTY) as J.FieldAccess

        assertEquals("a.Outer.Inner", name.printTrimmed())
        assertEquals("Inner", name.simpleName)
        assertEquals("a.Outer.Inner", name.type.asClass()?.fullyQualifiedName)

        val outer = name.target as J.FieldAccess
        assertEquals("Outer", outer.simpleName)
        assertEquals("a.Outer", outer.type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun buildStaticImport(jp: JavaParser) {
        val name = TreeBuilder.buildName("a.A.*", EMPTY) as J.FieldAccess

        assertEquals("a.A.*", name.printTrimmed())
        assertEquals("*", name.simpleName)
    }
}
