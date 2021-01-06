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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J

interface JavaTemplateTest {

//    @Test
//    fun buildSnippetLocalVariableReference(jp: JavaParser) {
//        val a = jp.parse("""
//            import java.util.List;
//            public class A {
//                int n = 0;
//                void foo(String m, List<String> others) {
//                    n++;
//                }
//            }
//        """.trimIndent())[0]
//
//        val method = a.classes[0].methods[0]
//        val methodBodyCursor = CursorExtractor(method.body!!.statements[0].elem).extractFromTree(a)
//        val paramName = (method.params.elem[0].elem as J.VariableDecls).vars[0].elem.name
//        val template = JavaTemplate.builder(
//            """
//                    n++;
//                    others.add(#{});
//                  """).build()
//        val snippets = template.generate<J>(methodBodyCursor, paramName)
//        Assertions.assertTrue(snippets[0] is J.MethodInvocation)
//    }
//
//    @Test
//    fun buildSnippetLocalMethodReference(jp: JavaParser) {
//        val a = jp.parse("""
//            import java.util.List;
//            import static java.util.Collections.emptyList;
//
//            public class A {
//                int n = 0;
//                void foo(String m, List<String> others) {
//                    incrementCounterByListSize(others);
//                    others.add(m);
//                }
//                void incrementCounterByListSize(List<String> list) {
//                    n =+ list.size();
//                }
//            }
//        """.trimIndent())[0]
//
//        val method = a.classes[0].methods[0]
//        val methodBodyCursor = CursorExtractor(method.body!!.statements[0].elem).extractFromTree(a)
//        val param = (method.params.elem[0].elem as J.VariableDecls).vars[0].elem.name
//
//        val template = JavaTemplate.builder(
//            """
//                    others.add(#{});
//                    incrementCounterByListSize(others);
//                 """).build()
//        val snippets = template.generate<J>(methodBodyCursor, param)
//        val methodInv1 : Expression = snippets[0] as Expression
//        Assertions.assertNotNull(methodInv1.type, "The type information should be populated")
//        val methodInv2 : Expression = snippets[1] as Expression
//        Assertions.assertNotNull(methodInv2.type, "The type information should be populated")
//    }
//
    @Test
    fun buildSnippetMethodReferenceGenerics(jp: JavaParser) {
        val a = jp.parse("""
            import java.util.List;
            import java.util.ArrayList;
            import static java.util.Collections.emptyList;

            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    boolean flag = true;
                    if (flag) {
                        List<String> clone = B.cloneList(others);
                        clone.add(m);
                    }
                }

                public static class B {
                    public static List<String> cloneList(List<String> list) {
                        return new ArrayList<>(list);
                    }
                }

                public static class C {

                    private int hello = 0;
                    private String nope = "nothing here";
                }

            }
        """.trimIndent())[0]

        val method = a.classes[0].methods[0]
        val then = (method.body!!.statements[1].elem as J.If).thenPart.elem as J.Block
        val methodBodyCursor = CursorExtractor(then.statements[0].elem).visit(a)
        val param = (method.params.elem[0].elem as J.VariableDecls).vars[0].elem.name

        val template = JavaTemplate.builder(
            """others.add(#{});
                    #{};""").build()
        val snippets = template.generate<J>(methodBodyCursor, param, then.statements[0].elem)

        val methodInv1 : Expression = snippets[0] as Expression
        Assertions.assertThat(methodInv1.type).`as`("The type information should be populated").isNotNull()
        val variableDeclarations : J.VariableDecls = snippets[1] as J.VariableDecls
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        (Assertions.assertThat("List<String>").isEqualTo(variableDeclarations.typeExpr.printTrimmed()))
    }

    class CursorExtractor(val scope: J) : JavaIsoProcessor<CursorHolder>() {

        init {
            setCursoringOn()
        }

        fun visit(tree : J?) : Cursor? {
            val cursorHolder = CursorHolder()
            super.visit(tree, cursorHolder)
            return cursorHolder.c;
        }

        override fun visitEach(tree: J?, cursorHolder: CursorHolder) : J?  {
            if (scope.isScope(tree)) {
                cursorHolder.c = this.cursor
            }
            return tree
        }
    }
    class CursorHolder() {
        var c : Cursor? = null
    }
}
