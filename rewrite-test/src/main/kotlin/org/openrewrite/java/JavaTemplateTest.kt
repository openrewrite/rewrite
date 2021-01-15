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
import org.openrewrite.java.tree.*
import kotlin.streams.toList

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
                        .generateBefore(
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
        afterConditions = {cu -> cu.classes }
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
                        .generateAfter(
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
    fun addMethodToClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoProcessor<ExecutionContext>() {
            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.parentOrThrow.getTree<J>()
                if (parent is J.ClassDecl) {
                    val template = JavaTemplate.builder("""
                            char incrementCounterByListSize(List<String> list) {
                                n =+ list.size();
                                return 'f';
                            }
                        """).build()

                    //Test generating the method using generateAfter and make sure the extraction is correct and has
                    //type attribution.
                    var generatedMethodDeclarations = template.generateAfter<J.MethodDecl>(
                        Cursor(cursor, block.statements[0].elem))
                    assertThat(generatedMethodDeclarations).`as`("The list of generated statements should be 1.")
                        .hasSize(1)
                    assertThat(generatedMethodDeclarations[0].type).isNotNull

                    //Test generating the method using generateBefore and make sure the extraction is correct and has
                    //type attribution.
                    generatedMethodDeclarations = template.generateBefore(
                        Cursor(cursor, block.statements[0].elem))
                    assertThat(generatedMethodDeclarations).`as`("The list of generated statements should be 1.")
                        .hasSize(1)
                    assertThat(generatedMethodDeclarations[0].type).isNotNull

                    b = b.withStatements(
                        ListUtils.concat(
                            block.statements,
                            JRightPadded(generatedMethodDeclarations[0],
                                Space.EMPTY
                            )
                        )
                    )
                }
                return b
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            import static java.util.Collections.emptyList;

            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    others.add(m);
                }
            }
        """,
        after = """
            import java.util.List;
            import static java.util.Collections.emptyList;

            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    others.add(m);
                }
                char incrementCounterByListSize(List<String> list) {
                    n =+ list.size();
                    return 'f';
                }
            }
        """
    )

    @Test
    fun addStaticMethodToClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoProcessor<ExecutionContext>() {
            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.parentOrThrow.getTree<J>()
                if (parent is J.ClassDecl) {
                    val template = JavaTemplate.builder("""
                            static char incrementCounterByListSize(List<String> list) {
                                n =+ list.size();
                                return 'f';
                            }
                        """).build()

                    //Test generating the method using generateAfter and make sure the extraction is correct and has
                    //type attribution.
                    var generatedMethodDeclarations = template.generateAfter<J.MethodDecl>(
                        Cursor(cursor, block.statements[0].elem))
                    assertThat(generatedMethodDeclarations).`as`("The list of generated statements should be 1.")
                        .hasSize(1)
                    assertThat(generatedMethodDeclarations[0].type).isNotNull

                    //Test generating the method using generateBefore and make sure the extraction is correct and has
                    //type attribution.
                    generatedMethodDeclarations = template.generateBefore(
                        Cursor(cursor, block.statements[0].elem))
                    assertThat(generatedMethodDeclarations).`as`("The list of generated statements should be 1.")
                        .hasSize(1)
                    assertThat(generatedMethodDeclarations[0].type).isNotNull

                    b = b.withStatements(
                        ListUtils.concat(
                            block.statements,
                            JRightPadded(generatedMethodDeclarations[0],
                                Space.EMPTY
                            )
                        )
                    )
                }
                return b
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            import static java.util.Collections.emptyList;

            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    others.add(m);
                }
            }
        """,
        after = """
            import java.util.List;
            import static java.util.Collections.emptyList;

            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    others.add(m);
                }
                static char incrementCounterByListSize(List<String> list) {
                    n =+ list.size();
                    return 'f';
                }
            }
        """
    )

    @Test
    fun changeMethodInvocations(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoProcessor<ExecutionContext>() {

            val template: JavaTemplate = JavaTemplate.builder("withString(#{}).length();")
                .build()
            init {
                setCursoringOn()
            }

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
                val m =  super.visitMethodInvocation(method, p)
                if (m.name.ident.simpleName != "countLetters") return m
                val argument = m.args.elem[0].elem
                val generatedMethodInvocations = template.generateBefore<J.MethodInvocation>(cursor, argument)
                assertThat(generatedMethodInvocations).`as`("The list of generated invocations should be 1.")
                    .hasSize(1)
                assertThat(generatedMethodInvocations[0].type).isNotNull
                return generatedMethodInvocations[0].withPrefix(m.prefix)
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            import java.util.stream.Collectors;
            import java.util.Arrays;

            public class A {
                String name = "Jill";
                {
                    countLetters("fred");
                }
                int n = countLetters(name);
                void foo() {
                    if (countLetters("fred") == 4) {
                        System.out.println("Letter Count :" + countLetters(name));
                    }
                    int letterCount = countLetters("fred") == 4 ? countLetters("fred") : 0;
                    letterCount = countLetters(name) != 3 ? 0 : countLetters(name);
                    String sub = "Fred".substring(0, countLetters("fred"));
                    Integer count = Integer.valueOf(countLetters("fred"));
                    for (int index = 0; index < countLetters(name); index++) {
                    }
                    int index = 0;
                    while (index < countLetters(name)) {
                        index++;
                    }
                    switch (countLetters(name)) {
                        case 4:
                        default:
                            break;
                    }
                    List<String> names = Arrays.asList("fred", "joe", "jill");
                    List<Integer> counts = names.stream().map(this::countLetters).collect(Collectors.toList());
                }
                public int countLetters(String sourceString) {
                    return sourceString.length();
                }
                public StringStub withString(String source) {
                    return new StringStub(source);
                }
                public class StringStub {
                    String source;
                    private StringStub(String source) {
                        this.source = source;
                    }
                    public int length() {
                        return source.length();
                    }
                }
           }
        """,
        after = """
           import java.util.List;
            import java.util.stream.Collectors;
            import java.util.Arrays;

            public class A {
                String name = "Jill";
                {
                    withString("fred").length();
                }
                int n = withString(name).length();
                void foo() {
                    if (withString("fred").length() == 4) {
                        System.out.println("Letter Count :" + withString(name).length());
                    }
                    int letterCount = withString("fred").length() == 4 ? withString("fred").length() : 0;
                    letterCount = withString(name).length() != 3 ? 0 : withString(name).length();
                    String sub = "Fred".substring(0, withString("fred").length());
                    Integer count = Integer.valueOf(withString("fred").length());
                    for (int index = 0; index < withString(name).length(); index++) {
                    }
                    int index = 0;
                    while (index < withString(name).length()) {
                        index++;
                    }
                    switch (withString(name).length()) {
                        case 4:
                        default:
                            break;
                    }
                    List<String> names = Arrays.asList("fred", "joe", "jill");
                    List<Integer> counts = names.stream().map(this::countLetters).collect(Collectors.toList());
                }
                public int countLetters(String sourceString) {
                    return sourceString.length();
                }
                public StringStub withString(String source) {
                    return new StringStub(source);
                }
                public class StringStub {
                    String source;
                    private StringStub(String source) {
                        this.source = source;
                    }
                    public int length() {
                        return source.length();
                    }
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

    @Test
    fun addAnnotationToClassWithImports(jp: JavaParser) = assertChanged(
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
            import java.util.List;
            
            public class A {
                void foo() {
                }
            }
        """,
        after = """
            import java.util.List;
            
            @Deprecated public class A {
                void foo() {
                }
            }
        """
    )

    @Test
    fun templateWithLocalMethodReference(jp: JavaParser) = assertChanged (
        jp,
        recipe = object : JavaIsoProcessor<ExecutionContext>() {
            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.parentOrThrow.getTree<J>()
                if (parent is J.MethodDecl && parent.name.ident.simpleName == "foo") {

                    val template = JavaTemplate.builder("\n#{};\n#{};").build()

                    //Test when statement is the insertion scope is before the first statement in the block
                    val generatedStatements = template
                        .generateBefore<J.MethodInvocation>(
                            Cursor(cursor, b.statements[0].elem),
                            b.statements[1].elem as J,
                            b.statements[0].elem as J
                        )
                    //Make sure type attribution is valid on the generated method invocations.
                    assertThat(generatedStatements).`as`("The list of generated statements should be 2.").hasSize(2)
                    assertThat(generatedStatements[0].type).`as`("The type information should be populated").isNotNull
                    assertThat(generatedStatements[1].type).`as`("The type information should be populated").isNotNull
                    b = b.withStatements(generatedStatements.stream().map { state ->
                        JRightPadded<Statement>(state,
                            Space.EMPTY)
                    }.toList())
                }
                return b
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            import static java.util.Collections.emptyList;

            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    incrementCounterByListSize(others);
                    others.add(m);
                }
                char incrementCounterByListSize(List<String> list) {
                    n =+ list.size();
                    return 'f';
                }
            }
        """,
        after = """
            import java.util.List;
            import static java.util.Collections.emptyList;

            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    others.add(m);
                    incrementCounterByListSize(others);
                }
                char incrementCounterByListSize(List<String> list) {
                    n =+ list.size();
                    return 'f';
                }
            }
        """
    )

    @Test
    fun templateWithSiblingClassMethodReference(jp: JavaParser) = assertChanged (
        jp,
        recipe = object : JavaIsoProcessor<ExecutionContext>() {

            //This test ensures that the source generation is working when a parameter contains a method invocation
            //to a method that exists in a sibling class. It
            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.parentOrThrow.getTree<J>()
                if (parent is J.If) {

                    val template = JavaTemplate.builder("#{};\n#{}").build()

                    //Test when statement is the insertion scope is before the first statement in the block
                    val generatedStatements = template
                        .generateAfter<J.MethodInvocation>(
                            Cursor(cursor, b.statements[0].elem),
                            b.statements[1].elem as J,
                            b.statements[0].elem as J
                        )
                    assertThat(generatedStatements).`as`("The list of generated statements should be 2.").hasSize(2)
                    //Make sure type attribution is valid on the generated method invocations.
                    assertThat(generatedStatements).`as`("The list of generated statements should be 2.").hasSize(2)
                    assertThat(generatedStatements[0].type).`as`("The type information should be populated").isNotNull
                    assertThat(generatedStatements[1].type).`as`("The type information should be populated").isNotNull
                    b = b.withStatements(generatedStatements.stream().map { state ->
                        JRightPadded<Statement>(state,
                            Space.EMPTY)
                    }.toList())
                }
                return b
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            import java.util.ArrayList;
            import static java.util.Collections.emptyList;

            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    boolean flag = true;
                    List<String> clone;
                    if (flag) {
                        clone.add(m);
                        B.cloneList(others);
                    }
                    int fred = 8;
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
        """,
        after = """
            import java.util.List;
            import java.util.ArrayList;
            import static java.util.Collections.emptyList;

            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    boolean flag = true;
                    List<String> clone;
                    if (flag) {
                        B.cloneList(others);
                        clone.add(m);
                    }
                    int fred = 8;
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
        """
    )
}
