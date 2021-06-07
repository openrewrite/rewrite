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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
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
            val t = template("int test2(int n) { return n; }").build()

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
            
                int test2(int n) {
                    return n;
                }
            }
        """,
        afterConditions = { cu ->
            val methodType = (cu.classes.first().body.statements.first() as J.MethodDeclaration).type!!
            assertThat(methodType.resolvedSignature.returnType).isEqualTo(JavaType.Primitive.Int)
            assertThat(methodType.resolvedSignature.paramTypes).containsExactly(JavaType.Primitive.Int)
            assertThat(methodType.genericSignature.returnType).isEqualTo(JavaType.Primitive.Int)
            assertThat(methodType.genericSignature.paramTypes).containsExactly(JavaType.Primitive.Int)
        }
    )

    @Test
    fun replaceLambdaWithMethodReference(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
            val t = template("Object::toString").build()

            override fun visitLambda(lambda: J.Lambda, p: ExecutionContext): J {
                return lambda.withTemplate(t, lambda.coordinates.replace())
            }
        }.toRecipe(),
        before = """
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = it -> it.toString();
            }
        """,
        after = """
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = Object::toString;
            }
        """
    )

    @Disabled
    @Test
    fun replaceMethodInvocationWithArray(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("""
            package org.openrewrite;
            public class Test {
                public void method(int[] val) {}
                public void method(int[] val1, String val2) {}
            }
        """.trimIndent()),
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("#{any(int[])}").build()

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
                var m: J.MethodInvocation = super.visitMethodInvocation(method, p)
                if (m.simpleName.equals("method") && m.arguments.size == 2) {
                    m = m.withTemplate(t, m.coordinates.replace(), m.arguments[0])
                }
                return m
            }
        }.toRecipe(),
        before = """
            import org.openrewrite.Test;
            class A {
                public void method() {
                    Test test = new Test();
                    int[] arr = new int[]{};
                    test.method(arr, null);
                }
            }
        """,
        after = """
            import org.openrewrite.Test;
            class A {
                public void method() {
                    Test test = new Test();
                    int[] arr = new int[]{};
                    test.method(arr);
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/602")
    @Test
    fun replaceMethodInvocationWithMethodReference(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaVisitor<ExecutionContext>() {
                val t = template("Object::toString").build()

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                return method.withTemplate(t, method.coordinates.replace());
            }

            }.toRecipe(),
        before = """
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = getToString();
                
                static Function<Object, String> getToString() {
                    return Object::toString;
                } 
            }
        """,
        after = """
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = Object::toString;
                
                static Function<Object, String> getToString() {
                    return Object::toString;
                } 
            }
        """
    )

    @Test
    fun replaceMethodParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("int m, java.util.List<String> n")
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
                void test(int m, java.util.List<String> n) {
                    new Runnable() {
                        void inner(int m, java.util.List<String> n) {
                        }
                    };
                }
            }
        """,
        afterConditions = { cu ->
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).type!!

            assertThat(type.paramNames)
                    .`as`("Changing the method's parameters should have also updated its type's parameter names")
                    .containsExactly("m", "n")
            assertThat(type.resolvedSignature.paramTypes[0])
                    .`as`("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                    .isEqualTo(JavaType.Primitive.Int)
            assertThat(type.resolvedSignature.paramTypes[1])
                    .`as`("Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'")
                    .matches { it is JavaType.Parameterized
                            && it.type.fullyQualifiedName == "java.util.List"
                            && it.typeParameters.size == 1
                            && it.typeParameters.first().asFullyQualified()!!.fullyQualifiedName == "java.lang.String"
                    }
        }
    )

    @Test
    fun replaceMethodParametersVariadicArray(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("Object[]... values")
                    .doBeforeParseTemplate(print)
                    .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if (method.simpleName == "test" && method.parameters.firstOrNull() is J.Empty) {
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
                void test(Object[]... values) {
                    new Runnable() {
                        void inner(Object[]... values) {
                        }
                    };
                }
            }
        """,
        afterConditions = { cu ->
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).type!!

            assertThat(type.paramNames)
                    .`as`("Changing the method's parameters should have also updated its type's parameter names")
                    .containsExactly("values")
            assertThat(type.resolvedSignature.paramTypes[0])
                    .`as`("Changing the method's parameters should have resulted in the first parameter's type being 'Object[]'")
                    .matches {
                        it is JavaType.Array && it.elemType.hasElementType("java.lang.Object")
                    }
        }
    )

    @Test
    fun replaceAndInterpolateMethodParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
                val t = template("int n, #{}")
                        .doBeforeParseTemplate(print)
                        .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                    if (method.simpleName == "test" && method.parameters.size == 1) {
                        return method.withTemplate(t,
                                method.coordinates.replaceParameters(),
                                method.parameters[0])
                    }
                    return method;
                }
            }.toRecipe(),
        before = """
            class Test {
                void test(String s) {
                }
            }
        """,
        after = """
            class Test {
                void test(int n, String s) {
                }
            }
        """,
        afterConditions = { cu ->
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).type!!

            assertThat(type.paramNames)
                    .`as`("Changing the method's parameters should have also updated its type's parameter names")
                    .containsExactly("n", "s")
            assertThat(type.resolvedSignature.paramTypes[0])
                    .`as`("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                    .isEqualTo(JavaType.Primitive.Int)
            assertThat(type.resolvedSignature.paramTypes[1])
                    .`as`("Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'")
                    .matches { it is JavaType.FullyQualified && it.fullyQualifiedName == "java.lang.String" }
        }
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
            val t = template("m, Integer.valueOf(n)")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
                if (method.arguments.size == 1) {
                    return method.withTemplate(t, method.coordinates.replaceArguments())
                }
                return method
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
                    test(m, Integer.valueOf(n));
                }
            }
        """,
        afterConditions = { cu ->
            val m = (cu.classes[0].body.statements[1] as J.MethodDeclaration).body!!.statements[0] as J.MethodInvocation
            val type = m.type!!
            assertThat(type.genericSignature.paramTypes[0])
                    .isEqualTo(JavaType.Primitive.Int)
            assertThat(type.genericSignature.paramTypes[1])
                    .matches { (it as JavaType.FullyQualified).fullyQualifiedName.equals("java.lang.Integer") }
        }
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
        """,
        afterConditions = { cu ->
            val testMethodDecl = cu.classes.first().body.statements.first() as J.MethodDeclaration
            assertThat(testMethodDecl.type!!.thrownExceptions.map { it.fullyQualifiedName })
                    .containsExactly("java.lang.Exception")
        }
    )

    @Test
    fun replaceMethodTypeParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val typeParamsTemplate = template("T, U")
                .doBeforeParseTemplate(print)
                .build()

            val methodArgsTemplate = template("List<T> t, U u")
                    .imports("java.util.List")
                    .doBeforeParseTemplate(print)
                    .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if(method.typeParameters == null) {
                    return method.withTemplate<J.MethodDeclaration>(typeParamsTemplate, method.coordinates.replaceTypeParameters())
                            .withTemplate(methodArgsTemplate, method.coordinates.replaceParameters())
                }
                return super.visitMethodDeclaration(method, p)
            }
        }.toRecipe(),
        before = """
            import java.util.List;
            
            class Test {
            
                void test() {
                }
            }
        """,
        after = """
            import java.util.List;
            
            class Test {
            
                <T, U> void test(List<T> t, U u) {
                }
            }
        """,
        afterConditions = { cu ->
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).type!!
            assertThat(type).isNotNull
            val paramTypes = type.genericSignature.paramTypes

            assertThat(paramTypes[0])
                    .`as`("The method declaration's type's genericSignature first argument should have have type 'java.util.List'")
                    .matches { tType ->
                        tType is JavaType.FullyQualified && tType.fullyQualifiedName == "java.util.List"
                    }

            assertThat(paramTypes[1])
                    .`as`("The method declaration's type's genericSignature second argument should have type 'U' with bound 'java.lang.Object'")
                    .matches { uType ->
                        uType is JavaType.GenericTypeVariable &&
                                uType.fullyQualifiedName == "U" &&
                                uType.bound!!.fullyQualifiedName == "java.lang.Object"
                    }
        }
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

    @Test
    fun replaceMissingBody(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = object : JavaVisitor<ExecutionContext>() {
                val t = template("")
                        .doBeforeParseTemplate(print)
                        .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    var m = method;
                    if(!m.isAbstract) {
                        return m;
                    }
                    m = m.withReturnTypeExpression(m.returnTypeExpression!!.withPrefix(Space.EMPTY))
                    m = m.withModifiers(emptyList())

                    m = m.withTemplate(t, m.coordinates.replaceBody())

                    return m;
                }
            }.toRecipe(),
        before = """
            abstract class Test {
                abstract void test();
            }
        """,
        after = """
            abstract class Test {
                void test(){
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
