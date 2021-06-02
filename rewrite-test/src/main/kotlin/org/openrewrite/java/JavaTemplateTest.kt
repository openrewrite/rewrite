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

import com.google.common.io.CharSink
import com.google.common.io.CharSource
import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.JavaFormatterOptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.tree.J
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.util.Comparator.comparing
import java.util.function.Consumer

interface JavaTemplateTest : JavaRecipeTest {

    @Test
    fun replacePackage(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("b").build()

            override fun visitPackage(pkg: J.Package, p: ExecutionContext): J.Package {
                if(pkg.expression.printTrimmed() == "a") {
                    return pkg.withTemplate(t, pkg.coordinates.replace())
                }
                return super.visitPackage(pkg, p)
            }
        }.toRecipe(),
        before = """
            package a;
            class Test {
            }
        """,
        after = """
            package b;
            class Test {
            }
        """
    )

    @Test
    fun replaceMethod(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("void test2() {}").build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if (method.simpleName == "test") {
                    return method.withTemplate(t, method.coordinates.replace())
                }
                return super.visitMethodDeclaration(method, p)
            }
        }.toRecipe(),
        before = """
            class Test {
                void test() {
                }
            }
        """,
        after = """
            class Test {
            
                void test2() {
                }
            }
        """
    )

    @Test
    fun replaceMethodParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("int m, int n")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if (method.simpleName == "test" && method.parameters.size == 1) {
                    // insert in outer method
                    val m: J.MethodDeclaration = method.withTemplate(t, method.coordinates.replaceParameters())
                    val newRunnable = (method.body!!.statements[0] as J.NewClass)

                    // insert in inner method
                    val innerMethod = (newRunnable.body!!.statements[0] as J.MethodDeclaration)
                    return m.withTemplate(t, innerMethod.coordinates.replaceParameters())
                }
                return super.visitMethodDeclaration(method, p)
            }
        }.toRecipe(),
        before = """
            class Test {
                void test() {
                    new Runnable() {
                        void inner() {
                        }
                    };
                }
            }
        """,
        after = """
            class Test {
                void test(int m, int n) {
                    new Runnable() {
                        void inner(int m, int n) {
                        }
                    };
                }
            }
        """
    )

    @Test
    fun replaceLambdaParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("int m, int n")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitLambda(lambda: J.Lambda, p: ExecutionContext): J.Lambda =
                if (lambda.parameters.parameters.size == 1) {
                    lambda.withTemplate(t, lambda.parameters.coordinates.replace())
                } else {
                    super.visitLambda(lambda, p)
                }
        }.toRecipe(),
        before = """
            class Test {
                void test() {
                    Object o = () -> 1;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    Object o = (int m, int n) -> 1;
                }
            }
        """
    )

    @Test
    fun replaceSingleStatement(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val t = template(
                "if(n != 1) {\n" +
                        "  n++;\n" +
                        "}"
            )
                .doBeforeParseTemplate(print)
                .build()

            override fun visitAssert(_assert: J.Assert, p: ExecutionContext): J =
                _assert.withTemplate(t, _assert.coordinates.replace())
        }.toRecipe(),
        before = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    if (n != 1) {
                        n++;
                    }
                }
            }
        """
    )

    @Test
    fun replaceStatementInBlock(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = object : JavaVisitor<ExecutionContext>() {
            val t = template("n = 2;\nn = 3;")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                val statement = method.body!!.statements[1]
                if (statement is J.Unary) {
                    return method.withTemplate(t, statement.coordinates.replace())
                }
                return method
            }
        }.toRecipe(),
        before = """
            class Test {
                int n;
                void test() {
                    n = 1;
                    n++;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    n = 1;
                    n = 2;
                    n = 3;
                }
            }
        """
    )

    @Test
    fun beforeStatementInBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val t = template("assert n == 0;")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                val statement = method.body!!.statements[0]
                if (statement is J.Assignment) {
                    return method.withTemplate(t, statement.coordinates.before())
                }
                return method
            }
        }.toRecipe(),
        before = """
            class Test {
                int n;
                void test() {
                    n = 1;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                    n = 1;
                }
            }
        """
    )

    @Test
    fun afterStatementInBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val t = template("n = 1;")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                if (method.body!!.statements.size == 1) {
                    return method.withTemplate(t, method.body!!.statements[0].coordinates.after())
                }
                return method
            }
        }.toRecipe(),
        before = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                    n = 1;
                }
            }
        """
    )

    @Test
    fun lastStatementInClassBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val t = template("int n;")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J {
                if (classDecl.body.statements.isEmpty()) {
                    return classDecl.withTemplate(t, classDecl.body.coordinates.lastStatement())
                }
                return classDecl
            }
        }.toRecipe(),
        before = """
            class Test {
            }
        """,
        after = """
            class Test {
                int n;
            }
        """
    )

    @Test
    fun lastStatementInMethodBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val t = template("n = 1;")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                if (method.body!!.statements.size == 1) {
                    return method.withTemplate(t, method.body!!.coordinates.lastStatement())
                }
                return method
            }
        }.toRecipe(),
        before = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                    n = 1;
                }
            }
        """
    )

    @Test
    fun replaceStatementRequiringNewImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val t = template("List<String> s = null;")
                .imports("java.util.List")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitAssert(_assert: J.Assert, p: ExecutionContext): J {
                maybeAddImport("java.util.List")
                return _assert.withTemplate(t, _assert.coordinates.replace())
            }
        }.toRecipe(),
        before = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        after = """
            import java.util.List;
            
            class Test {
                int n;
                void test() {
                    List<String> s = null;
                }
            }
        """
    )

    @Test
    fun replaceArguments(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("m, n")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
                if (method.arguments.size == 1) {
                    return method.withTemplate(t, method.coordinates.replaceArguments())
                }
                return super.visitMethodInvocation(method, p)
            }
        }.toRecipe(),
        before = """
            abstract class Test {
                abstract void test();
            
                void test(int m, int n) {
                    test();
                }
            }
        """,
        after = """
            abstract class Test {
                abstract void test();
            
                void test(int m, int n) {
                    test(m, n);
                }
            }
        """
    )

    @Test
    fun replaceClassAnnotation(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("@Deprecated")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitAnnotation(annotation: J.Annotation, p: ExecutionContext): J.Annotation {
                if(annotation.simpleName == "SuppressWarnings") {
                    return annotation.withTemplate(t, annotation.coordinates.replace())
                }
                return super.visitAnnotation(annotation, p)
            }
        }.toRecipe(),
        before = "@SuppressWarnings(\"ALL\") class Test {}",
        after = "@Deprecated class Test {}"
    )

    @Test
    fun replaceMethodAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("@SuppressWarnings(\"other\")")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if (method.leadingAnnotations.size == 0) {
                    return method.withTemplate(t, method.coordinates.replaceAnnotations())
                }
                return super.visitMethodDeclaration(method, p)
            }
        }.toRecipe(),
        before = """
            class Test {
                static final String WARNINGS = "ALL";
            
                public @SuppressWarnings(WARNINGS) Test() {
                }
            
                public void test1() {
                }
            
                public @SuppressWarnings(WARNINGS) void test2() {
                }
            }
        """,
        after = """
            class Test {
                static final String WARNINGS = "ALL";
            
                @SuppressWarnings("other")
                public Test() {
                }
            
                @SuppressWarnings("other")
                public void test1() {
                }
            
                @SuppressWarnings("other")
                public void test2() {
                }
            }
        """
    )

    @Test
    fun replaceClassAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("@SuppressWarnings(\"other\")")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                if(classDecl.leadingAnnotations.size == 0 && classDecl.simpleName != "Test") {
                    return classDecl.withTemplate(t, classDecl.coordinates.replaceAnnotations())
                }
                return super.visitClassDeclaration(classDecl, p)
            }
        }.toRecipe(),
        before = """
            class Test {
                static final String WARNINGS = "ALL";
                
                class Inner1 {
                }
            }
        """,
        after = """
            class Test {
                static final String WARNINGS = "ALL";
            
                @SuppressWarnings("other")
                class Inner1 {
                }
            }
        """
    )

    @Test
    fun replaceVariableAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("@SuppressWarnings(\"other\")")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitVariableDeclarations(multiVariable: J.VariableDeclarations, p: ExecutionContext): J.VariableDeclarations {
                if(multiVariable.leadingAnnotations.size == 0) {
                    return multiVariable.withTemplate(t, multiVariable.coordinates.replaceAnnotations())
                }
                return super.visitVariableDeclarations(multiVariable, p)
            }
        }.toRecipe(),
        before = """
            class Test {
                void test() {
                    int m;
                    final @SuppressWarnings("ALL") int n;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    @SuppressWarnings("other") int m;
                    @SuppressWarnings("other") final int n;
                }
            }
        """
    )

    @Test
    fun addMethodAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("@SuppressWarnings(\"other\")")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if (method.leadingAnnotations.size == 0) {
                    return method.withTemplate(t, method.coordinates.addAnnotation(comparing { it.simpleName }))
                }
                return super.visitMethodDeclaration(method, p)
            }
        }.toRecipe(),
        before = """
            class Test {
                static final String WARNINGS = "ALL";
            
                public void test() {
                }
            }
        """,
        after = """
            class Test {
                static final String WARNINGS = "ALL";
            
                @SuppressWarnings("other")
                public void test() {
                }
            }
        """
    )

    @Test
    fun addClassAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("@SuppressWarnings(\"other\")")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                if(classDecl.leadingAnnotations.size == 0 && classDecl.simpleName != "Test") {
                    return classDecl.withTemplate(t, classDecl.coordinates.addAnnotation(comparing { it.simpleName }))
                }
                return super.visitClassDeclaration(classDecl, p)
            }
        }.toRecipe(),
        before = """
            class Test {
                class Inner1 {
                }
            }
        """,
        after = """
            class Test {
            
                @SuppressWarnings("other")
                class Inner1 {
                }
            }
        """
    )

    @Test
    fun replaceClassImplements(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("Serializable, Closeable")
                .imports("java.io.*")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                if(classDecl.implements == null) {
                    maybeAddImport("java.io.Closeable");
                    maybeAddImport("java.io.Serializable");
                    return classDecl.withTemplate(t, classDecl.coordinates.replaceImplementsClause())
                }
                return super.visitClassDeclaration(classDecl, p)
            }
        }.toRecipe(),
        before = """
            class Test {
            }
        """,
        after = """
            import java.io.Closeable;
            import java.io.Serializable;
            
            class Test implements Serializable, Closeable {
            }
        """
    )

    @Test
    fun replaceClassExtends(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("List<String>")
                .imports("java.util.*")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                if(classDecl.extends == null) {
                    maybeAddImport("java.util.List");
                    return classDecl.withTemplate(t, classDecl.coordinates.replaceExtendsClause())
                }
                return super.visitClassDeclaration(classDecl, p)
            }
        }.toRecipe(),
        before = """
            class Test {
            }
        """,
        after = """
            import java.util.List;
            
            class Test extends List<String> {
            }
        """
    )

    @Test
    fun replaceThrows(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("Exception")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if(method.throws == null) {
                    return method.withTemplate(t, method.coordinates.replaceThrows())
                }
                return super.visitMethodDeclaration(method, p)
            }
        }.toRecipe(),
        before = """
            class Test {
                void test() {}
            }
        """,
        after = """
            class Test {
                void test() throws Exception {}
            }
        """
    )

    @Test
    fun replaceMethodTypeParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("T, U")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if(method.typeParameters == null) {
                    return method.withTemplate(t, method.coordinates.replaceTypeParameters())
                }
                return super.visitMethodDeclaration(method, p)
            }
        }.toRecipe(),
        before = """
            class Test {
            
                void test() {
                }
            }
        """,
        after = """
            class Test {
            
                <T, U> void test() {
                }
            }
        """
    )

    @Test
    fun replaceClassTypeParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("T, U")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                if(classDecl.typeParameters == null) {
                    return classDecl.withTemplate(t, classDecl.coordinates.replaceTypeParameters())
                }
                return super.visitClassDeclaration(classDecl, p)
            }
        }.toRecipe(),
        before = """
            class Test {
            }
        """,
        after = """
            class Test<T, U> {
            }
        """
    )

    @Test
    fun replaceBody(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = object : JavaVisitor<ExecutionContext>() {
            val t = template("n = 1;")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                val statement = method.body!!.statements[0]
                if (statement is J.Unary) {
                    return method.withTemplate(t, method.coordinates.replaceBody())
                }
                return method
            }
        }.toRecipe(),
        before = """
            class Test {
                int n;
                void test() {
                    n++;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    n = 1;
                }
            }
        """
    )

    companion object {
        val print = Consumer<String> { s: String ->
            try {
                val bos = ByteArrayOutputStream()
                Formatter(JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.AOSP).build())
                    .formatSource(CharSource.wrap(s), object : CharSink() {
                        override fun openStream() = OutputStreamWriter(bos)
                    })

//                println(bos.toString(Charsets.UTF_8).trim())
            } catch(_: Throwable) {
//                println("Unable to format:")
//                println(s)
            }
        }
    }
}
