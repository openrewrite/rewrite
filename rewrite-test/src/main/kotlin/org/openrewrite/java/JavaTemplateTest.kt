package org.openrewrite.java

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.Tree
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Expression

interface JavaTemplateTest {

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
        val methodBodyCursor = CursorExtractor(method.body!!.statements[0].elem).extractFromTree(a)
        val paramName = (method.params.elem[0].elem as J.VariableDecls).vars[0].elem.name
        val template = JavaTemplate.builder(
            """
                    n++;
                    others.add(#{});
                  """).build()
        val snippets = template.generate<J>(methodBodyCursor, paramName)
        Assertions.assertTrue(snippets[0] is J.MethodInvocation)
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
        val methodBodyCursor = CursorExtractor(method.body!!.statements[0].elem).extractFromTree(a)
        val param = (method.params.elem[0].elem as J.VariableDecls).vars[0].elem.name

        val template = JavaTemplate.builder(
            """
                    others.add(#{});
                    incrementCounterByListSize(others);
                 """).build()
        val snippets = template.generate<J>(methodBodyCursor, param)
        val methodInv1 : Expression = snippets[0] as Expression
        Assertions.assertNotNull(methodInv1.type, "The type information should be populated")
        val methodInv2 : Expression = snippets[1] as Expression
        Assertions.assertNotNull(methodInv2.type, "The type information should be populated")
    }

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
        val methodBodyCursor = CursorExtractor(then.statements[0].elem).extractFromTree(a)
        val param = (method.params.elem[0].elem as J.VariableDecls).vars[0].elem.name

        val template = JavaTemplate.builder(
            """
                    others.add(#{});
                    #{};
                 """).build()
        val snippets = template.generate<J>(methodBodyCursor, param, then.statements[0].elem)

        val methodInv1 : Expression = snippets[0] as Expression
        Assertions.assertNotNull(methodInv1.type, "The type information should be populated")
        val variableDeclarations : J.VariableDecls = snippets[1] as J.VariableDecls
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        (Assertions.assertEquals("List<String>",
            variableDeclarations.typeExpr.printTrimmed()))
    }

    class CursorExtractor(val scope: Tree) : JavaTreeExtractor<Cursor>() {

        init {
            setCursoringOn()
        }

        override fun visitEach(tree: J?, context: ValueContext?) : J?  {
            if (scope.isScope(tree)) {
                context?.value = cursor
            }
            return tree
        }
    }
}