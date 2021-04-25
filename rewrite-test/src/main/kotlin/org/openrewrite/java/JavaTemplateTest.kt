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

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Parser
import org.openrewrite.java.tree.J
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

interface JavaTemplateTest : JavaRecipeTest {
    companion object {
        private val logger = LoggerFactory.getLogger(JavaTemplateTest::class.java)
//        private val logEvent = Consumer<String> { s -> logger.info(s) }
        private val logEvent = Consumer<String> { _ -> /* do nothing */ }
    }

    @Test
    fun addMethodAnnotationTest(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.classpath("junit-jupiter-api").build(),
        recipe = object : JavaIsoVisitor<ExecutionContext>() {

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                val tagComp = Comparator<J.Annotation> { a1, a2 -> a1.simpleName.compareTo(a2.simpleName) }
                    .reversed()

                val m = super.visitMethodDeclaration(method, p)
                return m
                    .withTemplate<J.MethodDeclaration>(
                        template("@Tag(\"tag1\")")
                            .doBeforeParseTemplate(logEvent)
                            .doAfterVariableSubstitution(logEvent)
                            .build(),
                        m.coordinates.addAnnotation(tagComp)
                    )
                    .withTemplate(
                        template("@Tag(\"tag2\")")
                            .doBeforeParseTemplate(logEvent)
                            .doAfterVariableSubstitution(logEvent)
                            .build(),
                        m.coordinates.addAnnotation(tagComp)
                    )
            }
        }.toRecipe(),
        before = """
            import org.junit.jupiter.api.*;
            class A {
                @Test
                void method() {
                }
            }
        """,
        after = """
            import org.junit.jupiter.api.*;
            class A {

                @Test
                @Tag("tag1")
                @Tag("tag2")
                void method() {
                }
            }
        """,
        cycles = 1
    )

    @Test
    fun replaceMethodArgumentsTest(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("""(() -> "test")""")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
                val m = super.visitMethodInvocation(method, p)
                return m.withTemplate(template, m.coordinates.replaceArguments())
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
        """
    )

    @Test
    @Issue("#327")
    fun addStatementBeforeAnotherStatement(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            override fun visitClassDeclaration(
                classDecl: J.ClassDeclaration,
                p: ExecutionContext
            ): J {
                val c = super.visitClassDeclaration(classDecl, p) as J.ClassDeclaration
                return c.withTemplate(
                    template("public String bar = \"hey!\";").build(),
                    classDecl.body.statements[1].coordinates.before()
                )
            }
        }.toRecipe(),
        before = """
            class A {
                public String foo;
                public String fuz;
            }
        """,
        after = """
            class A {
                public String foo;
                public String bar = "hey!";
                public String fuz;
            }
        """,
        cycles = 1
    )

    @Test
    fun beforeStatements(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val template = template("others.add(#{});")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitBlock(block: J.Block, p: ExecutionContext): J {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.MethodDeclaration) {
                    b = b.withTemplate(
                        template,
                        block.statements[1].coordinates.before(),
                        (parent.parameters[0] as J.VariableDeclarations).variables[0]
                    )
                }
                return b
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
        cycles = 1
    )

    /**
     * Test to make sure when the current cursor is a parent but the element having the template applied is a child. This
     * tests that the logic to find the insertion point starts from the cursor rather than the "Changing element"
     */
    @Test
    fun templateOnNestedChild(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("others.add(#{});")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                val m = super.visitMethodDeclaration(method, p)
                val statements = m.body!!.statements
                statements[1] = statements[1].withTemplate(
                    template,
                    statements[1].coordinates.replace(),
                    (m.parameters[0] as J.VariableDeclarations).variables[0]
                )
                return m.withBody(m.body!!.withStatements(statements))
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
                }
            }
        """
    )

    @Test
    fun lastInMethodBodyStatement(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val template = template("others.add(#{});").doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitBlock(block: J.Block, p: ExecutionContext): J {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.MethodDeclaration) {
                    b = b.withTemplate(
                        template,
                        block.coordinates.lastStatement(),
                        (parent.parameters[0] as J.VariableDeclarations).variables[0]
                    )
                }
                return b
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
        """,
        cycles = 1
    )

    @Test
    fun addToEmptyMethodBody(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val template = template("others.add(#{});").doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitBlock(block: J.Block, p: ExecutionContext): J {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.MethodDeclaration) {
                    b = b.withTemplate(
                        template,
                        block.coordinates.lastStatement(),
                        (parent.parameters[0] as J.VariableDeclarations).variables[0]
                    )
                }
                return b
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
        """,
        cycles = 1
    )

    @Test
    fun addMethodToClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template(
                """
                        char incrementCounterByListSize(List<String> list) {
                            n += list.size();
                            return 'f';
                        }
                    """
            ).doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.ClassDeclaration) {
                    b = b.withTemplate(template, block.coordinates.lastStatement())
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
        """,
        cycles = 1
    )

    @Test
    fun addImportToTemplate(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("extends List<String>")
                .imports("java.util.List")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()
            override fun visitClassDeclaration(clazz: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                val c = super.visitClassDeclaration(clazz, p)
                return c.withTemplate(template, c.coordinates.replaceExtendsClause())
            }
        }.toRecipe(),
        before = """
            public class A {
            }
        """,
        after = """
            public class A extends List<String> {
            }
        """
    )

    @Test
    fun addStaticImportToTemplate(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("extends List<String>")
                .staticImports("java.util.Collections.emptyList")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()
            override fun visitClassDeclaration(clazz: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                val c = super.visitClassDeclaration(clazz, p)
                return c.withTemplate(template, c.coordinates.replaceExtendsClause())
            }
        }.toRecipe(),
        before = """
            package org.example;
            import java.util.List;
            public class A {
            }
        """,
        after = """
            package org.example;
            import java.util.List;
            public class A extends List<String> {
            }
        """
    )

    @Test
    fun addStaticMethodToClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template(
                """
                        static char incrementCounterByListSize(List<String> list) {
                            n += list.size();
                            return 'f';
                        }
                    """
            ).doAfterVariableSubstitution(logEvent).doBeforeParseTemplate(logEvent).build()

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.ClassDeclaration) {
                    b = b.withTemplate(template, block.coordinates.lastStatement())
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
        """,
        cycles = 1
    )

    @Test
    fun changeMethodInvocations(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val template: JavaTemplate = template("withString(#{}).length()")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                val m = super.visitMethodInvocation(method, p) as J.MethodInvocation
                if (m.name.simpleName != "countLetters") {
                    return m
                }
                return m.withTemplate(template, m.coordinates.replace(), m.arguments[0])
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
                    int letterCount = countLetters(name) == 4 ? countLetters("notfred") : countLetters(name);
                    int letterCount2 = countLetters(name) != 3 ? 0 : countLetters(name);
                    System.out.println("Letter Count :" + letterCount);
                    System.out.println("Letter Count :" + letterCount2);
                    String sub = "Fred".substring(0, countLetters("fred"));
                    for (int index = 0; index < countLetters(name); index++) {
                      letterCount++;
                    }
                    int index = 0;
                    while (index < countLetters(name)) {
                        index++;
                    }
                    switch (countLetters(name)) {
                        case 4:
                        case 1:
                        case 2:
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
                    int letterCount = withString(name).length() == 4 ? withString("notfred").length() : withString(name).length();
                    int letterCount2 = withString(name).length() != 3 ? 0 : withString(name).length();
                    System.out.println("Letter Count :" + letterCount);
                    System.out.println("Letter Count :" + letterCount2);
                    String sub = "Fred".substring(0, withString("fred").length());
                    for (int index = 0; index < withString(name).length(); index++) {
                      letterCount++;
                    }
                    int index = 0;
                    while (index < withString(name).length()) {
                        index++;
                    }
                    switch (withString(name).length()) {
                        case 4:
                        case 1:
                        case 2:
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
            val template = template("@Deprecated")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                val m = super.visitMethodDeclaration(method, p)
                return m.withTemplate(template, m.coordinates.replaceAnnotations())
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

                @Deprecated
                void foo() {
                }
            }
        """
    )

    @Issue("#239")
    @Test
    fun replaceAnnotation(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("@Issue")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitAnnotation(annotation: J.Annotation, p: ExecutionContext): J.Annotation {
                val a = super.visitAnnotation(annotation, p)
                return a.withTemplate(template, a.coordinates.replace())
            }
        }.toRecipe(),
        before = """
                public class A {
                    @Deprecated
                    void foo() {
                    }
                }
            """,
        after = """
                public class A {
                    @Issue
                    void foo() {
                    }
                }
        """
    )

    @Test
    fun replaceOneOfThreeMethodAnnotation() = assertChanged(
        JavaParser.fromJavaVersion().dependsOn(
            Collections.singletonList(
                Parser.Input.fromString(
            """
                public @interface Anno1{}
                public @interface Anno2{}
                public @interface Anno3{}
            """))).build(),
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("@Anno3")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitAnnotation(annotation: J.Annotation, p: ExecutionContext): J.Annotation {
                val a = super.visitAnnotation(annotation, p)
                if (a.simpleName.equals("Anno1")) {
                    return a.withTemplate(template, a.coordinates.replace())
                }
                return a
            }
        }.toRecipe(),
        before = """
            public class Cd {
                @Anno1
                @Anno2
                void md() {}
            }
        """,
        after = """
            public class Cd {
                @Anno3
                @Anno2
                void md() {}
            }
        """
    )

    @Issue("#331")
    @Test
    fun replaceAnnotationOnClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("@Issue")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitAnnotation(annotation: J.Annotation, p: ExecutionContext): J.Annotation {
                val a = super.visitAnnotation(annotation, p)
                return a.withTemplate(template, a.coordinates.replace())
            }
        }.toRecipe(),
        before = """
                @Deprecated
                public class A {}
            """,
        after = """
                @Issue
                public class A {}
        """
    )

    @Issue("#333")
    @Test
    fun replaceAnnotationOnClassWithCommentsAndAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("@Issue")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitAnnotation(annotation: J.Annotation, p: ExecutionContext): J.Annotation {
                val a = super.visitAnnotation(annotation, p)
                if (a.simpleName.equals("Deprecated")) {
                    return a.withTemplate(template, a.coordinates.replace())
                }
                return a
            }
        }.toRecipe(),
        before = """
                @Deprecated
                @SuppressWarnings("yo")
                public class A {}
            """,
        after = """
                @Issue
                @SuppressWarnings("yo")
                public class A {}
        """
    )

    @Test
    fun addAnnotationToClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("@Deprecated")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitClassDeclaration(clazz: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                val c = super.visitClassDeclaration(clazz, p)
                return c.withTemplate(template, c.coordinates.replaceAnnotations())
            }
        }.toRecipe(),
        before = """
            public class A {
                void foo() {
                }
            }
        """,
        after = """
            @Deprecated
            public class A {
                void foo() {
                }
            }
        """
    )

    @Test
    fun addAnnotationToClassWithImports(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("@Deprecated")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitClassDeclaration(clazz: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                val c = super.visitClassDeclaration(clazz, p)
                return c.withTemplate(template, c.coordinates.replaceAnnotations())
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
            
            @Deprecated
            public class A {
                void foo() {
                }
            }
        """
    )

    @Test
    fun templateWithLocalMethodReference(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("\n#{};\n#{};")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.MethodDeclaration && parent.name.simpleName == "foo") {
                    b = b.withTemplate(
                        template, b.statements[0].coordinates.before(),
                        b.statements[1] as J,
                        b.statements[0] as J
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
                    incrementCounterByListSize(others);
                    others.add(m);
                }
                char incrementCounterByListSize(List<String> list) {
                    n += list.size();
                    return 'f';
                }
            }
        """,
        cycles = 1
    )

    @Test
    fun templateWithSiblingClassMethodReference(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("#{};\n#{};")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                var b = super.visitBlock(block, p)
                val parent = cursor.dropParentUntil { it is J }.getValue<J>()
                if (parent is J.If) {
                    b = b.withTemplate(
                        template,
                        b.coordinates.lastStatement(),
                        b.statements[1],
                        b.statements[0]
                    )
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
                    boolean flag = others.contains(m);
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
                    boolean flag = others.contains(m);
                    List<String> clone;
                    if (flag) {
                        clone.add(m);
                        B.cloneList(others);
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
        """,
        cycles = 1
    )
    @Test
    fun templateMethodIntoClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("""public String hello() { return "Hello!"; }""")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                var cd = super.visitClassDeclaration(classDecl, p)
                val helloMethodExists = cd.body.statements.asSequence()
                    .filterIsInstance(J.MethodDeclaration::class.java)
                    .find { it.name.simpleName == "hello" } != null

                if (helloMethodExists) {
                    return cd
                }

                cd = cd.withBody(
                    cd.body.withTemplate(
                        template,
                        cd.body.coordinates.lastStatement())
                )

                return cd
            }
        }.toRecipe(),
        before = """
            package com.yourorg;

            class A {
            void foo() {}
            }
        """,
        after = """
            package com.yourorg;

            class A {
            void foo() {}
            
                public String hello() {
                    return "Hello!";
                }
            }
        """
    )

    @Test
    fun replaceClassTypeParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("<T,P>")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                val c = super.visitClassDeclaration(classDecl, p)
                return c.withTemplate(template, c.coordinates.replaceTypeParameters())
            }
        }.toRecipe(),
        before = """
            public class A<T> {
                void foo() {
                }
            }
        """,
        after = """
            public class A<T, P> {
                void foo() {
                }
            }
        """
    )

    @Test
    fun replaceClassExtends(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("extends ArrayList<String>").doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                val c = super.visitClassDeclaration(classDecl, p)
                return c.withTemplate(template, c.coordinates.replaceExtendsClause())
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
            val template = template("implements List<String>").doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                val c = super.visitClassDeclaration(classDecl, p)
                return c.withTemplate(template, c.coordinates.replaceImplementsClause())
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
            public abstract class A implements List<String> {
                private String name = "Jill";
            }
        """
    )

    @Test
    fun replaceClassBody(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template(
                """
                        {
                        private String name = "Jill";
                        private String name2 = "Fred";
                        }
                    """
            ).doAfterVariableSubstitution(logEvent).doBeforeParseTemplate(logEvent).build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                val c = super.visitClassDeclaration(classDecl, p)
                return c.withTemplate(template, c.coordinates.replaceBody())
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
            val template = template("<T,P>")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                val m = super.visitMethodDeclaration(method, p)
                return m.withTemplate(template, m.coordinates.replaceTypeParameters())
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
                <T, P> void foo() {
                }
            }
        """
    )

    @Test
    fun replaceMethodDeclarationParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("(String foo, String bar)")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                val m = super.visitMethodDeclaration(method, p)
                return m.withTemplate(template, m.coordinates.replaceParameters())
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
    fun addMethodDeclarationParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("Date dateOfBirth,String firstName,")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .imports("java.util.Date")
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                var m = super.visitMethodDeclaration(method, p)
                if (m.simpleName == "setCustomerInfo") {
                    m = m.withTemplate(template, m.parameters[0].coordinates.before())
                    maybeAddImport("java.util.Date")
                }

                return m
            }
        }.toRecipe(),
        before = """
            public abstract class Customer {
                public abstract void setCustomerInfo(String lastName);
            }
        """,
        after = """
            import java.util.Date;

            public abstract class Customer {
                public abstract void setCustomerInfo(Date dateOfBirth, String firstName, String lastName);
            }
        """,
        cycles = 1
    )

    @Test
    fun replaceMethodDeclarationThrows(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("throws RuntimeException")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                val m = super.visitMethodDeclaration(method, p)
                return m.withTemplate(template, m.coordinates.replaceThrows())
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
                void foo() throws RuntimeException {
                }
            }
        """
    )

    @Test
    fun replaceMethodInvocationArguments(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val template = template("(\"fred\", \"sally\", \"dude\")")
                .doAfterVariableSubstitution(logEvent)
                .doBeforeParseTemplate(logEvent)
                .build()

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
                val m = super.visitMethodInvocation(method, p)
                return m.withTemplate(template, m.coordinates.replaceArguments())
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
