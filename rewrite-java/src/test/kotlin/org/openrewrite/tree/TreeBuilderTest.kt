/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.tree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openrewrite.Parser
import org.openrewrite.asClass
import org.openrewrite.assertRefactored
import org.openrewrite.tree.Formatting.EMPTY
import org.openrewrite.visitor.RetrieveCursorVisitor
import org.openrewrite.visitor.refactor.AstTransform
import org.openrewrite.visitor.refactor.RefactorVisitor

class TreeBuilderTest {
    @Test
    fun buildSnippet() {
        val a = Parser().parse("""
            import java.util.List;
            public class A {
                int n = 0;
                
                void foo(String m, List<String> others) {
                }
            }
        """.trimIndent())

        val method = a.classes[0].methods[0]
        val methodBodyCursor = RetrieveCursorVisitor(method.body!!.id).visit(a)
        val paramName = (method.params.params[0] as J.VariableDecls).vars[0].name

        val snippets = TreeBuilder.buildSnippet<Statement>(a, methodBodyCursor, "others.add({});", paramName)
        assertTrue(snippets[0] is J.MethodInvocation)
    }

    @Test
    fun injectSnippetIntoMethod() {
        val a = Parser().parse("""
            import java.util.List;
            public class A {
                int n = 0;
                
                void foo(String m, List<String> others) {
                }
            }
        """.trimIndent())

        val method = a.classes[0].methods[0]
        val methodBodyCursor = RetrieveCursorVisitor(method.body!!.id).visit(a)
        val paramName = (method.params.params[0] as J.VariableDecls).vars[0].name

        val snippets = TreeBuilder.buildSnippet<Statement>(a, methodBodyCursor, """
            others.add({});
            if(others.contains({})) {
                others.remove({});
            }
        """.trimIndent(), paramName, paramName, paramName)

        val fixed = a.refactor().visit(object: RefactorVisitor() {
            override fun visitMethod(method: J.MethodDecl): MutableList<AstTransform> {
                return transform(method) { m ->
                    m.withBody(m.body!!.withStatements(snippets))
                }
            }
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
    fun buildFullyQualifiedClassName() {
        val name = TreeBuilder.buildName("java.util.List", EMPTY) as J.FieldAccess

        assertEquals("java.util.List", name.printTrimmed())
        assertEquals("List", name.simpleName)
    }

    @Test
    fun buildFullyQualifiedInnerClassName() {
        val name = TreeBuilder.buildName("a.Outer.Inner", EMPTY) as J.FieldAccess

        assertEquals("a.Outer.Inner", name.printTrimmed())
        assertEquals("Inner", name.simpleName)
        assertEquals("a.Outer.Inner", name.type.asClass()?.fullyQualifiedName)

        val outer = name.target as J.FieldAccess
        assertEquals("Outer", outer.simpleName)
        assertEquals("a.Outer", outer.type.asClass()?.fullyQualifiedName)
    }

    @Test
    fun buildStaticImport() {
        val name = TreeBuilder.buildName("a.A.*", EMPTY) as J.FieldAccess

        assertEquals("a.A.*", name.printTrimmed())
        assertEquals("*", name.simpleName)
    }
}
