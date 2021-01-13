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
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.RecipeTest
import org.openrewrite.internal.ListUtils
import org.openrewrite.java.format.MinimumViableSpacingProcessor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JRightPadded
import org.openrewrite.java.tree.Space
import org.openrewrite.java.tree.Space.format
import org.openrewrite.java.tree.Statement

interface JavaTemplateTest : RecipeTest {
    @Test
    fun beforeMethodBodyStatement(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaProcessor<ExecutionContext>() {
            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J {
                val parent = cursor.parentOrThrow.getTree<J>()
                if(parent is J.MethodDecl) {
                    val template = JavaTemplate.builder("others.add(#{});").build()

                    //Test to make sure the template extraction is working correctly. Both of these calls should return
                    //a single template element and should have type attribution

                    //Test when statement is the insertion scope is before the first statement in the block
                    var generatedMethodInvocations = template
                        .generateBefore<J.MethodInvocation>(
                            Cursor(cursor, block.statements[0].elem),
                            (parent.params.elem[0].elem as J.VariableDecls).vars[0]
                        )
                    assertThat(generatedMethodInvocations).`as`("The list of generated statements should be 1.").hasSize(1)
                    assertThat(generatedMethodInvocations[0].type).isNotNull

                    //Test when insertion scope is between two statements in a block
                    generatedMethodInvocations = template
                        .generateBefore<J.MethodInvocation>(
                            Cursor(cursor, block.statements[1].elem),
                            (parent.params.elem[0].elem as J.VariableDecls).vars[0]
                        )
                    assertThat(generatedMethodInvocations).`as`("The list of generated statements should be 1.").hasSize(1)
                    assertThat(generatedMethodInvocations[0].type).isNotNull

                    return block.withStatements(
                        ListUtils.concat(
                            JRightPadded(
                                generatedMethodInvocations[0],
                                Space.EMPTY
                            ),
                            block.statements
                        )
                    )
                }
                return super.visitBlock(block, p)
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    n++;
                    n++;
                }
            }
        """,
        after = """
            import java.util.List;
            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    others.add(m);
                    n++;
                    n++;
                }
            }
        """,
        afterConditions = {cu -> cu.getClasses() }
    )

    @Test
    fun afterMethodBodyStatement(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaProcessor<ExecutionContext>() {
            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J {
                val parent = cursor.parentOrThrow.getTree<J>()
                if(parent is J.MethodDecl) {
                    val template = JavaTemplate.builder("others.add(#{});").build()

                    //Test to make sure the template extraction is working correctly. Both of these calls should return
                    //a single template element and should have type attribution

                    //Test when insertion scope is between two statements in a block
                    var generatedMethodInvocations = template
                        .generateAfter<J.MethodInvocation>(
                            Cursor(cursor, block.statements[0].elem),
                            (parent.params.elem[0].elem as J.VariableDecls).vars[0]
                        )
                    assertThat(generatedMethodInvocations).`as`("The list of generated statements should be 1.").hasSize(1)
                    assertThat(generatedMethodInvocations[0].type).isNotNull

                    //Test when insertion scope is after the last statements in a block
                    generatedMethodInvocations = template
                        .generateAfter<J.MethodInvocation>(
                            Cursor(cursor, block.statements[1].elem),
                            (parent.params.elem[0].elem as J.VariableDecls).vars[0]
                        )
                    assertThat(generatedMethodInvocations).`as`("The list of generated statements should be 1.").hasSize(1)
                    assertThat(generatedMethodInvocations[0].type).isNotNull

                    return block.withStatements(
                        ListUtils.concat(
                            block.statements,
                            JRightPadded(generatedMethodInvocations[0],
                                Space.EMPTY
                            )
                        )
                    )
                }
                return super.visitBlock(block, p)
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    n++;
                    n++;
                }
            }
        """,
        after = """
            import java.util.List;
            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    n++;
                    n++;
                    others.add(m);
                }
            }
        """
    )

    @Test
    fun addAnnotationToMethod(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoProcessor<ExecutionContext>() {
            init {
                setCursoringOn()
            }

            override fun visitMethod(method: J.MethodDecl, p: ExecutionContext): J.MethodDecl {
                var m = super.visitMethod(method, p)
                m = m.withAnnotations(ListUtils.concat(
                    m.annotations,
                    JavaTemplate.builder("@Deprecated").build()
                        .generateBefore<J.Annotation>(Cursor(cursor, method))[0]
                ))
                m = MinimumViableSpacingProcessor<ExecutionContext>().visitMethod(m, ExecutionContext.builder().build())
                return m
            }
        }.toRecipe(),
        before = """
            public class A {
                void foo() {
                }
            }
        """,
        after = """
            public class A {
                @Deprecated void foo() {
                }
            }
        """
    )

    @Test
    fun addAnnotationToClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoProcessor<ExecutionContext>() {
            init {
                setCursoringOn()
            }

            override fun visitClassDecl(clazz: J.ClassDecl, p: ExecutionContext): J.ClassDecl {
                var c = super.visitClassDecl(clazz, p)

                val generatedAnnotations = JavaTemplate.builder("@Deprecated").build()
                    .generateBefore<J.Annotation>(Cursor(cursor, clazz))

                assertThat(generatedAnnotations).`as`("The list of generated annotations should be 1.").hasSize(1)
                assertThat(generatedAnnotations[0].type).isNotNull

                c = c.withAnnotations(ListUtils.concat(c.annotations, generatedAnnotations[0]))
                c = MinimumViableSpacingProcessor<ExecutionContext>().visitClassDecl(c, ExecutionContext.builder().build())
                return c
            }
        }.toRecipe(),
        before = """
            public class A {
                void foo() {
                }
            }
        """,
        after = """
            @Deprecated public class A {
                void foo() {
                }
            }
        """
    )

//    @Test
//    fun buildSnippetLocalMethodReference(jp: JavaParser) {
//        val a = jp.parse(
//            """
//            import java.util.List;
//            import static java.util.Collections.emptyList;
//
//            public class A {
//                int n = 0;
//                void foo(String m, List<String> others) {
//                    incrementCounterByListSize(others);
//                    others.add(m);
//                }
//                char incrementCounterByListSize(List<String> list) {
//                    n =+ list.size();
//                    return 'f';
//                }
//            }
//        """.trimIndent()
//        )[0]
//
//        val method = a.classes[0].methods[0]
//        val methodBodyCursor = CursorExtractor(method.body!!).visit(a)
//        val param1 = (method.params.elem[0].elem as J.VariableDecls).vars[0].elem.name
//        val param2 = method.body!!.statements[0].elem
//
//        val template = JavaTemplate.builder(
//            """{
//                        others.add(#{});
//                        #{};
//                    }
//                 """
//        ).build()
//        val snippets = template.generate<J>(methodBodyCursor, param1, param2)
//        //Snippet should be the method block with the two statements in it.
//        assertThat(snippets).hasSize(1);
//        val block: J.Block = snippets[0] as J.Block
//
//        val methodInvocation1 = block.statements[0].elem as J.MethodInvocation // others.add(m)
//        val methodInvocation2 = block.statements[0].elem as J.MethodInvocation // incrementCounterByListSize(others);
//
//        assertThat(methodInvocation1.type).`as`("The type information should be populated").isNotNull
//        assertThat(methodInvocation2.type).`as`("The type information should be populated").isNotNull
//    }
//
//    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
//    @Test
//    fun buildSnippetMethodReferenceSiblingClass(jp: JavaParser) {
//        val a = jp.parse(
//            """
//            import java.util.List;
//            import java.util.ArrayList;
//            import static java.util.Collections.emptyList;
//
//            public class A {
//                int n = 0;
//                void foo(String m, List<String> others) {
//                    boolean flag = true;
//                    if (flag) {
//                        List<String> clone = B.cloneList(others);
//                        clone.add(m);
//                    }
//                    int fred = 8;
//                }
//
//                public static class B {
//                    public static List<String> cloneList(List<String> list) {
//                        return new ArrayList<>(list);
//                    }
//                }
//
//                public static class C {
//
//                    private int hello = 0;
//                    private String nope = "nothing here";
//                }
//
//            }
//        """.trimIndent()
//        )[0]
//
//        val method = a.classes[0].methods[0]
//        val then = (method.body!!.statements[1].elem as J.If).thenPart.elem as J.Block
//        val methodBodyCursor = CursorExtractor(then.statements[0].elem).visit(a)
//        val param = (method.params.elem[0].elem as J.VariableDecls).vars[0].elem.name
//
//        val template = JavaTemplate.builder(
//            """others.add(#{});
//                    #{};"""
//        ).build()
//        val snippets = template.generate<J>(methodBodyCursor, param, then.statements[0].elem)
//        assertThat(snippets).hasSize(2)
//
//        val methodInv1: Expression = snippets[0] as Expression
//        assertThat(methodInv1.type).`as`("The type information should be populated").isNotNull
//        val variableDeclarations = snippets[1] as J.VariableDecls
//        val methodInvocation = variableDeclarations.vars[0].elem.initializer.elem as J.MethodInvocation
//        assertThat("Class{A.B}").isEqualTo((methodInvocation.select.elem as J.Ident).ident.type.toString())
//        assertThat("List<String>").isEqualTo(variableDeclarations.typeExpr.printTrimmed())
//    }
//
//    class CursorExtractor(private val scope: J) : JavaIsoProcessor<CursorHolder>() {
//
//        init {
//            setCursoringOn()
//        }
//
//        fun visit(tree: J?): Cursor {
//            val cursorHolder = CursorHolder()
//            super.visit(tree, cursorHolder)
//            return cursorHolder.c!!
//        }
//
//        override fun visitEach(tree: J?, cursorHolder: CursorHolder): J? {
//            if (scope.isScope(tree)) {
//                cursorHolder.c = this.cursor
//            }
//            return tree
//        }
//    }
//
//    class CursorHolder {
//        var c: Cursor? = null
//    }
}
