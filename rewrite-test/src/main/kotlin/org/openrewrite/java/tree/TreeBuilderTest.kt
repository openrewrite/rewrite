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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Formatting.EMPTY
import org.openrewrite.java.*
import org.openrewrite.whenParsedBy

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
        """.trimIndent())[0]

        val method = a.classes[0].methods[0]
        val methodBodyCursor = RetrieveCursor(method.body).visit(a)
        val paramName = (method.params.params[0] as J.VariableDecls).vars[0].name

        val snippets = TreeBuilder(jp).buildSnippet<Statement>(
                methodBodyCursor,
                "others.add(${paramName.printTrimmed()});")

        assertTrue(snippets[0] is J.MethodInvocation)
    }

    @Test
    fun buildSnippetLocalVariableReference(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            public class A {
                int n = 0;
                
                void foo(String m, List<String> others) {
                    n++;
                }
            }
        """.trimIndent())[0]

        val method = a.classes[0].methods[0]
        val methodBodyCursor = RetrieveCursor(method.body).visit(a)
        val paramName = (method.params.params[0] as J.VariableDecls).vars[0].name

        val snippets = TreeBuilder(a.buildParser()).buildSnippet<Statement>(
                methodBodyCursor,
                """
                    n++;
                    others.add(${paramName.printTrimmed()});
                 """)
        val unary : Expression = snippets[0] as Expression
        assertEquals(unary.type, JavaType.Primitive.Int,"The unary should have a type of int.")

        //Second Statement should be "others.add(m)" and this should have type information.
        val methodInv : Expression = snippets[1] as Expression
        assertNotNull(methodInv.type, "The type information should be populated")
    }

    @Test
    fun buildSnippetLocalMethodReference(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            import static java.util.Collections.emptyList;

            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    incrementCounterByListSize(others);
                    others.add(m);
                }
                void incrementCounterByListSize(List<String> list) {
                    n =+ list.size();
                }
            }
        """.trimIndent())[0]

        val method = a.classes[0].methods[0]
        val methodBodyCursor = RetrieveCursor(method.body!!.statements[0]).visit(a)
        val paramName = (method.params.params[0] as J.VariableDecls).vars[0].name

        val snippets = TreeBuilder(a.buildParser()).buildSnippet<Statement>(
                methodBodyCursor,
                """
                    others.add(${paramName.printTrimmed()});
                    incrementCounterByListSize(others);
                 """)
        val methodInv1 : Expression = snippets[0] as Expression
        assertNotNull(methodInv1.type, "The type information should be populated")
        val methodInv2 : Expression = snippets[1] as Expression
        assertNotNull(methodInv2.type, "The type information should be populated")
    }

    @Test
    fun buildSnippetStaticMethodReference(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            
            import static java.util.Arrays.asList;

            public class A {
                int n = 0;
                List<String> bucket;
                
                void foo(String... m) {
                    bucket.addAll(asList(m));
                    incrementCounterByListSize(bucket);
                }
                void incrementCounterByListSize(List<String> list) {
                    n =+ list.size();
                }
            }
        """.trimIndent())[0]

        val method = a.classes[0].methods[0]
        val methodBodyCursor = RetrieveCursor(method.body!!.statements[0]).visit(a)
        val paramName = (method.params.params[0] as J.VariableDecls).vars[0].name

        val snippets = TreeBuilder(a.buildParser()).buildSnippet<Statement>(
                methodBodyCursor,
                """
                    if(bucket == null) {
                        bucket = asList(${paramName.printTrimmed()});
                    } else {
                        bucket.addAll(asList(${paramName.printTrimmed()}));
                    }
                    incrementCounterByListSize(bucket);
                 """)

        val ifStatement : J.If = snippets[0] as J.If
        val thenPart : J.Block<*> = ifStatement.thenPart as J.Block<*>
        val assign : J.Assign = thenPart.statements[0] as J.Assign
        val methodInv1 : Expression = assign.assignment as Expression
        assertNotNull(methodInv1.type, "The type information for the static method asList() should be populated")

        val methodInv2 : Expression = snippets[1] as Expression
        assertNotNull(methodInv2.type, "The type information should be populated")
    }

    @Test
    fun injectSnippetIntoMethod(jp: JavaParser) {
        """
            import java.util.List;
            public class A {
                int n = 0;
                
                void foo(String m, List<String> others) {
                }
            }
        """
                .whenParsedBy(jp)
                .whenVisitedByMapped { a: J.CompilationUnit ->
                    val method = a.classes[0].methods[0]
                    val methodBodyCursor = RetrieveCursor(method.body).visit(a)
                    val paramName = (method.params.params[0] as J.VariableDecls).vars[0].name.printTrimmed()

                    val snippets = TreeBuilder(jp).buildSnippet<Statement>(methodBodyCursor,
                            """
                                others.add(${paramName});
                                if(others.contains(${paramName})) {
                                    others.remove(${paramName});
                                }
                            """.trimIndent()
                    )

                    object : JavaRefactorVisitor() {
                        override fun visitMethod(method: J.MethodDecl): J = method.withBody(method.body!!.withStatements(snippets))
                    }
                }
                .isRefactoredTo("""
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
    @Disabled("https://github.com/openrewrite/rewrite/issues/38")
    fun buildStaticInit(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.ArrayList;
            import java.util.Collection;
            
            public class A {
                static Collection<String> list;
            }
        """.trimIndent())[0]

        @Suppress("UNCHECKED_CAST") val init = TreeBuilder(jp)
                .buildDeclaration(a.classes[0],
                        """
                            static {
                                list = new ArrayList<>();
                            }
                        """.trimIndent(), JavaType.Class.build("java.util.ArrayList")) as J.Block<J>

        assertThat((init.statements[0] as J.Assign).type)
                .isEqualTo(JavaType.Class.build("java.util.Collection"))
    }

    @Test
    fun buildMethodDeclaration(jp: JavaParser) {
        val b = """
            package b;

            public class B {}
        """.trimIndent()

        val a = jp.parse("""
            import java.util.ArrayList;
            import java.util.Collection;
            
            public class A {
                Collection<String> list = new ArrayList<>();
            }
        """.trimIndent())[0]

        val methodDecl = TreeBuilder(jp).buildMethodDeclaration(a.classes[0],
                """
                    B build() {
                        return new B();
                    }
                """.trimIndent(), JavaType.Class.build("b.B"))

        assertThat(methodDecl.printTrimmed()).isEqualTo("""
            B build() {
                return new B();
            }
        """.trimIndent())

        assertThat(methodDecl.returnTypeExpr?.type).isEqualTo(JavaType.Class.build("b.B"))
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

    @Test
    fun buildInnerClass(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.Arrays;
            import java.util.List;
            
            class A {
                List<String> foo = Arrays.asList("Hello", "World");
            }
        """).first()

        val innerClassSnippet = """
            class B {
                String hello = foo.get(0);
            }
        """.trimIndent()

        val result = TreeBuilder(jp).buildDeclaration(a.classes.first(), innerClassSnippet,
                JavaType.Class.build("java.util.List"))
        assertThat(result).isExactlyInstanceOf(J.ClassDecl::class.java)
        assertThat(result.printTrimmed()).isEqualTo(innerClassSnippet)
    }
}
