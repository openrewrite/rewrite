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
import org.openrewrite.ExecutionContext
import org.openrewrite.RecipeTest
import org.openrewrite.java.format.MinimumViableSpacingVisitor
import org.openrewrite.java.tree.*

interface JavaTemplateTest : RecipeTest {

    @Test
    fun lamdaMethodParameterTest(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = JavaTemplate.builder("() -> \"test\"").templateEventHandler(TemplateLoggingEventHandler()).build()
            init {
                setCursoringOn()
            }

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
                val m = super.visitMethodInvocation(method, p)
                return generate(template,  m.args[0].coordinates().replaceThis())
            }
        }.toRecipe(),
        before = """
            import java.util.function.Supplier;
            class A {
                void test() {
                    printStuff("test");
                }
                void printStuff(String string) {}
                void printStuff(Supplier<String> stringSupplier) {}
            }
        """,
        after = """
            import java.util.function.Supplier;
            class A {
                void test() {
                    printStuff(() -> "test");
                }
                void printStuff(String string) {}
                void printStuff(Supplier<String> stringSupplier) {}
            }
        """,
        afterConditions = { cu -> cu.classes }
    )

    @Test
    fun beforeStatements(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            init {
                setCursoringOn()
            }
            val template = JavaTemplate.builder("others.add(#{});").templateEventHandler(TemplateLoggingEventHandler()).build()

            override fun visitBlock(block: J.Block, p: ExecutionContext): J {
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.MethodDecl) {

                    //Test when coordinate is before the first statement in the block
                    var b = generate<J.Block>(
                        template,
                        block.statements[0].coordinates().before(),
                        (parent.params[0] as J.VariableDecls).vars[0]
                    )
                    assertThat(b.statements).`as`("The list of statements should be 3.").hasSize(3)
                    assertThat((b.statements[0] as J.MethodInvocation).type).isNotNull

                    //Test when insertion scope is between two statements in a block
                    b = generate(
                            template,
                            block.statements[1].coordinates().before(),
                            (parent.params[0] as J.VariableDecls).vars[0]
                        )
                    assertThat(b.statements).`as`("The list of statements should be 3.").hasSize(3)
                    assertThat((b.statements[1] as J.MethodInvocation).type).isNotNull
                    return b
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
                    others.add(m);
                    n++;
                }
            }
        """,
        afterConditions = { cu -> cu.classes }
    )

    @Test
    fun lastInMethodBodyStatement(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val template = JavaTemplate.builder("others.add(#{});").templateEventHandler(TemplateLoggingEventHandler()).build()
            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J {
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.MethodDecl) {

                    //Test when insertion scope is after the last statements in a block
                    val b = generate<J.Block>(template,
                        block.coordinates().lastStatement(),
                        (parent.params[0] as J.VariableDecls).vars[0])

                    assertThat(b.statements).`as`("The list of generated statements should be 1.").hasSize(3)
                    assertThat((b.statements[2] as J.MethodInvocation).type).isNotNull
                    return b
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
    fun addToEmptyMethodBody(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val template = JavaTemplate.builder("others.add(#{});").templateEventHandler(TemplateLoggingEventHandler()).build()
            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J {
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.MethodDecl) {
                    //Test to make sure the template extraction is working correctly. Both of these calls should return
                    //a single template element and should have type attribution

                    //Test when insertion scope is between two statements in a block
                    val b = generate<J.Block>(template,
                        block.coordinates().lastStatement(),
                        (parent.params[0] as J.VariableDecls).vars[0])
                    assertThat(b.statements).`as`("The list of generated statements should be 1.").hasSize(1)
                    assertThat((b.statements[0] as J.MethodInvocation).type).isNotNull
                    return b
                }
                return super.visitBlock(block, p)
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                }
            }
        """,
        after = """
            import java.util.List;
            public class A {
                int n = 0;
                void foo(String m, List<String> others) {
                    others.add(m);
                }
            }
        """
    )

    @Test
    fun addMethodToClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {

            val template = JavaTemplate.builder(
                """
                            char incrementCounterByListSize(List<String> list) {
                                n += list.size();
                                return 'f';
                            }
                        """
            ).templateEventHandler(TemplateLoggingEventHandler()).build()

            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.ClassDecl) {

                    //Test generating the method as the last element in the class block
                    b = generate<J.Block>(template, block.coordinates().lastStatement())
                    assertThat(b.statements).`as`("The list of generated statements should be 3.").hasSize(3)
                    assertThat((b.statements[2] as J.MethodDecl).type).isNotNull
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
                    n += list.size();
                    return 'f';
                }
            }
        """
    )

    @Test
    fun addStaticMethodToClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {

            val template = JavaTemplate.builder(
                """
                            static char incrementCounterByListSize(List<String> list) {
                                n += list.size();
                                return 'f';
                            }
                        """
            ).templateEventHandler(TemplateLoggingEventHandler()).build()

            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.ClassDecl) {

                    b = generate<J.Block>(template, block.coordinates().lastStatement())
                    assertThat(b.statements).`as`("The list of generated statements should be 1.").hasSize(3)
                    assertThat((b.statements[2] as J.MethodDecl).type).isNotNull
                }
                return b
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            import static java.util.Collections.emptyList;

            public class A {
                static int n = 0;
                void foo(String m, List<String> others) {
                    others.add(m);
                }
            }
        """,
        after = """
            import java.util.List;
            import static java.util.Collections.emptyList;

            public class A {
                static int n = 0;
                void foo(String m, List<String> others) {
                    others.add(m);
                }
            
                static char incrementCounterByListSize(List<String> list) {
                    n += list.size();
                    return 'f';
                }
            }
        """
    )

    @Test
    fun changeMethodInvocations(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {

            val template: JavaTemplate = JavaTemplate.builder("withString(#{}).length()")
                .templateEventHandler(TemplateLoggingEventHandler()).build()

            init {
                setCursoringOn()
            }

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                val m = super.visitMethodInvocation(method, p) as J.MethodInvocation
                if (m.name.ident.simpleName != "countLetters") return m
                val argument = m.args[0]
                val results = generate<J.MethodInvocation>(template, m.coordinates().replaceThis(), argument)
                return results
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
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = JavaTemplate.builder("@Deprecated")
                .templateEventHandler(TemplateLoggingEventHandler()).build()
            init {
                setCursoringOn()
            }
            override fun visitMethod(method: J.MethodDecl, p: ExecutionContext): J.MethodDecl {
                var m = super.visitMethod(method, p)
                m = generate<J.MethodDecl>(template, m.coordinates().before())
                //TODO - I tried using AutoFormat here but it is not putting a space between the end of the
                //      annotation and the void on the method.
                return  MinimumViableSpacingVisitor<ExecutionContext>().visitMethod(m, p)
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
        recipe = object : JavaIsoVisitor<ExecutionContext>() {

            val template = JavaTemplate.builder("@Deprecated")
                .templateEventHandler(TemplateLoggingEventHandler()).build()

            init {
                setCursoringOn()
            }


            override fun visitClassDecl(clazz: J.ClassDecl, p: ExecutionContext): J.ClassDecl {
                var c = super.visitClassDecl(clazz, p)
                c = generate<J.ClassDecl>(template, c.coordinates().lastAnnotation())
                assertThat(c.annotations).`as`("The list of generated annotations should be 1.").hasSize(1)
                assertThat(c.annotations[0].type).isNotNull
                //TODO - I tried using AutoFormat here but it is not putting a space between the end of the
                //      annotation and the public on the class.
                return  MinimumViableSpacingVisitor<ExecutionContext>().visitClassDecl(c, p)
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
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = JavaTemplate.builder("@Deprecated")
                .templateEventHandler(TemplateLoggingEventHandler()).build()

            init {
                setCursoringOn()
            }

            override fun visitClassDecl(clazz: J.ClassDecl, p: ExecutionContext): J.ClassDecl {
                var c = super.visitClassDecl(clazz, p)

                c = generate<J.ClassDecl>(template, clazz.coordinates().lastAnnotation())

                assertThat(c.annotations).`as`("The list of generated annotations should be 1.").hasSize(1)
                assertThat(c.annotations[0].type).isNotNull

                return MinimumViableSpacingVisitor<ExecutionContext>().visitClassDecl(c,ExecutionContext.builder().build())
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
    fun templateWithLocalMethodReference(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {

            val template = JavaTemplate.builder("\n#{};\n#{};")
                .templateEventHandler(TemplateLoggingEventHandler()).build()

            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.MethodDecl && parent.name.ident.simpleName == "foo") {


                    //Test when statement is the insertion scope is before the first statement in the block
                    b = generate<J.Block>(template, b.statements[0].coordinates().before(),
                            b.statements[1] as J,
                            b.statements[0] as J
                        )
                    //Make sure type attribution is valid on the generated method invocations.
                    assertThat(b.statements).`as`("The list of generated statements should be 2.").hasSize(2)
                    assertThat((b.statements[0] as J.MethodInvocation).type).`as`("The type information should be populated").isNotNull
                    assertThat((b.statements[1] as J.MethodInvocation).type).`as`("The type information should be populated").isNotNull
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
                    n += list.size();
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
                    n += list.size();
                    return 'f';
                }
            }
        """
    )

    @Test
    fun templateWithSiblingClassMethodReference(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {

            val template = JavaTemplate.builder("#{};\n#{};")
                .templateEventHandler(TemplateLoggingEventHandler()).build()

            //This test ensures that the source generation is working when a parameter contains a method invocation
            //to a method that exists in a sibling class. It
            init {
                setCursoringOn()
            }

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.If) {

                    //Test when statement is the insertion scope is before the first statement in the block
                    b = generate<J.Block>(template,
                        b.coordinates().lastStatement(),
                        b.statements[1],
                        b.statements[0]
                    )

                    //Make sure type attribution is valid on the generated method invocations.
                    assertThat(b.statements).`as`("The list of generated statements should be 2.").hasSize(4)
                    assertThat((b.statements[2] as J.MethodInvocation).type).`as`("The type information should be populated").isNotNull
                    assertThat((b.statements[3] as J.MethodInvocation).type).`as`("The type information should be populated").isNotNull
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

    @Test
    fun replaceClassTypeParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = JavaTemplate.builder("T,P")
                .templateEventHandler(TemplateLoggingEventHandler()).build()
            init {
                setCursoringOn()
            }

            override fun visitClassDecl(classDecl: J.ClassDecl, p: ExecutionContext): J.ClassDecl {
                val c = super.visitClassDecl(classDecl, p)
                return generate<J.ClassDecl>(template, c.coordinates().replaceTypeParameters())
            }
        }.toRecipe(),
        before = """
            public class A <T> {
                void foo() {
                }
            }
        """,
        after = """
            public class A <T,P> {
                void foo() {
                }
            }
        """
    )

    @Test
    fun replaceClassExtends(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {

            val template = JavaTemplate.builder("ArrayList<String>").templateEventHandler(TemplateLoggingEventHandler()).build()

            init {
                setCursoringOn()
            }

            override fun visitClassDecl(classDecl: J.ClassDecl, p: ExecutionContext): J.ClassDecl {

                val c = super.visitClassDecl(classDecl, p)

                //Replace body.
                return generate<J.ClassDecl>(template, c.coordinates().replaceExtendsClause())
            }
        }.toRecipe(),
        before = """
            import java.util.ArrayList;
            public abstract class A {
                private String name = "Jill";
            }
        """,
        after = """
            import java.util.ArrayList;
            public abstract class A extends ArrayList<String> {
                private String name = "Jill";
            }
        """
    )

    @Test
    fun replaceClassImplements(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {

            val template = JavaTemplate.builder("List<String>").templateEventHandler(TemplateLoggingEventHandler()).build()

            init {
                setCursoringOn()
            }

            override fun visitClassDecl(classDecl: J.ClassDecl, p: ExecutionContext): J.ClassDecl {

                val c = super.visitClassDecl(classDecl, p)

                //Replace implements clause.
                return  generate<J.ClassDecl>(template, c.coordinates().replaceImplementsClause())
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            public abstract class A {
                private String name = "Jill";
            }
        """,
        after = """
            import java.util.List;
            public abstract class A  implements List<String> {
                private String name = "Jill";
            }
        """
    )

    @Test
    fun replaceClassBody(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {

            val template = JavaTemplate.builder("""
                {
                private String name = "Jill";
                private String name2 = "Fred";
                }
                """).templateEventHandler(TemplateLoggingEventHandler()).build()

            init {
                setCursoringOn()
            }

            override fun visitClassDecl(classDecl: J.ClassDecl, p: ExecutionContext): J.ClassDecl {

                var c = super.visitClassDecl(classDecl, p)

                //Replace body.
                 c = generate<J.ClassDecl>(template, c.coordinates().replaceBody())
                assertThat(c.body.statements).`as`("The list of generated statements should be 2.")
                    .hasSize(2)
                assertThat((c.body.statements[0] as J.VariableDecls).typeAsClass).isNotNull
                return c
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            public class A {
            }
        """,
        after = """
            import java.util.List;
            public class A {
                private String name = "Jill";
                private String name2 = "Fred";
            }
        """
    )

    @Test
    fun replaceMethodDeclarationTypeParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = JavaTemplate.builder("T,P")
                .templateEventHandler(TemplateLoggingEventHandler()).build()
            init {
                setCursoringOn()
            }
            override fun visitMethod(method: J.MethodDecl, p: ExecutionContext): J.MethodDecl {
                val m = super.visitMethod(method, p)
                return generate<J.MethodDecl>(template, m.coordinates().replaceTypeParameters())
            }
        }.toRecipe(),
        before = """
            public class A {
                <T> void foo() {
                }
            }
        """,
        after = """
            public class A {
                <T,P> void foo() {
                }
            }
        """
    )

    @Test
    fun replaceMethodDeclarationParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = JavaTemplate.builder("String foo, String bar")
                .templateEventHandler(TemplateLoggingEventHandler()).build()
            init {
                setCursoringOn()
            }
            override fun visitMethod(method: J.MethodDecl, p: ExecutionContext): J.MethodDecl {
                val m = super.visitMethod(method, p)
                return generate<J.MethodDecl>(template, m.coordinates().replaceParameters())
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
                void foo(String foo, String bar) {
                }
            }
        """
    )

    @Test
    fun replaceMethodDeclarationThrows(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = JavaTemplate.builder(" Exception, Throwable")
                .templateEventHandler(TemplateLoggingEventHandler()).build()
            init {
                setCursoringOn()
            }
            override fun visitMethod(method: J.MethodDecl, p: ExecutionContext): J.MethodDecl {
                val m = super.visitMethod(method, p)
                return generate<J.MethodDecl>(template, m.coordinates().replaceThrows())
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
                void foo() throws Exception, Throwable {
                }
            }
        """
    )

    @Test
    fun replaceMethodInvocationArguments(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = JavaTemplate.builder("\"fred\", \"sally\", \"dude\"")
                .templateEventHandler(TemplateLoggingEventHandler()).build()
            init {
                setCursoringOn()
            }

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
                val m = super.visitMethodInvocation(method, p)
                return generate<J.MethodInvocation>(template, m.coordinates().replaceArguments())
            }
        }.toRecipe(),
        before = """
            public class A {
                void foo() {
                    logNames("fred");
                }

                void logNames(String... names) {
                }
            }
        """,
        after = """
            public class A {
                void foo() {
                    logNames("fred", "sally", "dude");
                }

                void logNames(String... names) {
                }
            }
        """
    )

}
