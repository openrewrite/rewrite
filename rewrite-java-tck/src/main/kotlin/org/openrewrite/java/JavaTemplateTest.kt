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
@file:Suppress("DuplicatedCode", "ConstantConditions", "JUnitMalformedDeclaration", "UnusedAssignment",
    "InstantiationOfUtilityClass", "StatementWithEmptyBody", "StringOperationCanBeSimplified", "deprecation"
)

package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.tree.*
import org.openrewrite.test.RewriteTest
import java.util.*
import java.util.Comparator.comparing

@Suppress("Convert2MethodRef", "UnnecessaryBoxing")
interface JavaTemplateTest : RewriteTest {

    val replaceToStringWithLiteralRecipe: Recipe
        get() = RewriteTest.toRecipe{object : JavaVisitor<ExecutionContext>() {
            private var TO_STRING = MethodMatcher("java.lang.String toString()")
            private val t = JavaTemplate.builder({ cursor }, "#{any(java.lang.String)}").build()

            override fun visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): J {
                val mi = super.visitMethodInvocation(method, ctx) as J
                if (mi is J.MethodInvocation && TO_STRING.matches(mi)) {
                    return mi.withTemplate(t, mi.coordinates.replace(), mi.select)
                }
                return mi
            }
        }}

    @Test
    fun methodArgumentStopCommentsOnlyTerminateEnumInitializers() = rewriteRun(
        {spec -> spec.recipe(replaceToStringWithLiteralRecipe)},
        java("""
            import java.io.File;
            import java.io.IOException;
            import java.util.List;
            
            class Test {
                File getFile(File testDir, List<String> compileClassPath ) throws IOException {
                    assertEquals(new File(testDir, "ejbs/target/classes").getCanonicalFile(),
                        new File(compileClassPath.get(1).toString()).getCanonicalFile());
                }
                void assertEquals(File f1, File f2) {}
            }
        ""","""
            import java.io.File;
            import java.io.IOException;
            import java.util.List;
            
            class Test {
                File getFile(File testDir, List<String> compileClassPath ) throws IOException {
                    assertEquals(new File(testDir, "ejbs/target/classes").getCanonicalFile(),
                        new File(compileClassPath.get(1)).getCanonicalFile());
                }
                void assertEquals(File f1, File f2) {}
            }
        """)
    )
    @Issue("https://github.com/openrewrite/rewrite/issues/2475")
    @Test
    fun enumWithinEnum() = rewriteRun(
        {spec -> spec
            .recipe(replaceToStringWithLiteralRecipe)},
        java("""
            public enum Test {
                INSTANCE;
                public enum MatchMode { DEFAULT }
                public String doSomething() {
                    return "STARTING".toString();
                }
            }
        ""","""
            public enum Test {
                INSTANCE;
                public enum MatchMode { DEFAULT }
                public String doSomething() {
                    return "STARTING";
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1339")
    @Test
    fun templateStatementIsWithinTryWithResourcesBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                override fun visitNewClass(newClass: J.NewClass, p: ExecutionContext): J {
                    var nc = super.visitNewClass(newClass, p)
                    val md: J.MethodDeclaration? = cursor.firstEnclosing(J.MethodDeclaration::class.java)
                    if (md != null && md.simpleName.equals("createBis")) {
                        return nc
                    }
                    if (newClass.type != null && (newClass.type as JavaType.Class).fullyQualifiedName.equals("java.io.ByteArrayInputStream")
                        && newClass.arguments.isNotEmpty()
                    ) {
                        nc = nc.withTemplate(
                            JavaTemplate.builder({ this.cursor }, "createBis(#{anyArray()})").build(),
                            newClass.coordinates.replace(), newClass.arguments[0]
                        )
                    }
                    return nc
                }
            }
        })},
        java("""
            import java.io.*;
            import java.nio.charset.StandardCharsets;
            
            class Test {
                ByteArrayInputStream createBis(byte[] bytes) {
                    return new ByteArrayInputStream(bytes);
                }
                
                void doSomething() {
                    String sout = "";
                    try (BufferedReader br = new BufferedReader(new FileReader(null))) {
                        new ByteArrayInputStream("bytes".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        """,
        """
            import java.io.*;
            import java.nio.charset.StandardCharsets;
            
            class Test {
                ByteArrayInputStream createBis(byte[] bytes) {
                    return new ByteArrayInputStream(bytes);
                }
                
                void doSomething() {
                    String sout = "";
                    try (BufferedReader br = new BufferedReader(new FileReader(null))) {
                        createBis("bytes".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1796")
    @Test
    fun replaceIdentifierWithMethodInvocation() =  rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    return method.withBody(visit(method.body, p) as J.Block)
                }

                override fun visitIdentifier(identifier: J.Identifier, p: ExecutionContext): J {
                    return if (identifier.simpleName == "f") {
                        identifier.withTemplate(
                            JavaTemplate.builder({ this.cursor }, "#{any(java.io.File)}.getCanonicalFile().toPath()").build(),
                            identifier.coordinates.replace(),
                            identifier
                        )
                    } else {
                        identifier
                    }
                }
            }
        }).expectedCyclesThatMakeChanges(1).cycles(1)},
        java("""
            import java.io.File;
            class Test {
                void test(File f) {
                    System.out.println(f);
                }
            }
        """,
        """
            import java.io.File;
            class Test {
                void test(File f) {
                    System.out.println(f.getCanonicalFile().toPath());
                }
            }
        """)
    )

    @Suppress("UnaryPlus", "UnusedAssignment")
    @Test
    fun replaceExpressionWithAnotherExpression() =  rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                override fun visitUnary(unary: J.Unary, p: ExecutionContext): J {
                    return unary.withTemplate(
                        JavaTemplate.builder({ this.cursor }, "#{any()}++").build(),
                        unary.coordinates.replace(),
                        unary.expression
                    )
                }
            }
        }).expectedCyclesThatMakeChanges(1).cycles(1)},
        java("""
            class Test {
                void test(int i) {
                    int n = +i;
                }
            }
        """,
        """
            class Test {
                void test(int i) {
                    int n = i++;
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1796")
    @Test
    fun replaceFieldAccessWithMethodInvocation() =  rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    return method.withBody(visit(method.body, p) as J.Block)
                }

                override fun visitFieldAccess(fa: J.FieldAccess, p: ExecutionContext): J {
                    return if (fa.simpleName == "f") {
                        fa.withTemplate(
                            JavaTemplate.builder({ this.cursor }, "#{any(java.io.File)}.getCanonicalFile().toPath()").build(),
                            fa.coordinates.replace(),
                            fa
                        )
                    } else {
                        fa
                    }
                }
            }
        }).expectedCyclesThatMakeChanges(1).cycles(1)},
        java("""
            import java.io.File;
            class Test {
                File f;
                void test() {
                    System.out.println(this.f);
                }
            }
        """,
        """
            import java.io.File;
            class Test {
                File f;
                void test() {
                    System.out.println(this.f.getCanonicalFile().toPath());
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1092")
    @Test
    fun methodInvocationReplacementHasContextAboutLocalVariables() =  rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitMethodInvocation(
                    method: J.MethodInvocation,
                    ctx: ExecutionContext
                ): J.MethodInvocation {
                    return if (method.simpleName == "clear") {
                        method.withTemplate(
                            JavaTemplate.builder({ this.cursor }, """words.add("jon");""")
                                .build(),
                            method.coordinates.replace()
                        )
                    } else method
                }
            }
        })},
        java("""
            import java.util.List;
            class Test {
                List<String> words;
                void test() {
                    words.clear();
                }
            }
        """,
        """
            import java.util.List;
            class Test {
                List<String> words;
                void test() {
                    words.add("jon");
                }
            }
        """)
    )

    @Test
    fun innerEnumWithStaticMethod() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "new A()")
                    .build()

                override fun visitNewClass(newClass: J.NewClass, p: ExecutionContext): J =
                    when (newClass.arguments[0]) {
                        is J.Empty -> newClass
                        else -> newClass.withTemplate(t, newClass.coordinates.replace())
                    }
            }
        })},
        java("""
            class A {
                public enum Type {
                    One;
            
                    public Type(String t) {
                    }
            
                    String t;
            
                    public static Type fromType(String type) {
                        return null;
                    }
                }
            
                public A(Type type) {}
                public A() {}
            
                public void method(Type type) {
                    new A(type);
                }
            }
        """,
        """
            class A {
                public enum Type {
                    One;
            
                    public Type(String t) {
                    }
            
                    String t;
            
                    public static Type fromType(String type) {
                        return null;
                    }
                }
            
                public A(Type type) {}
                public A() {}
            
                public void method(Type type) {
                    new A();
                }
            }
        """)
    )

    @Test
    fun replacePackage(jp: JavaParser) =  rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "b").build()

                override fun visitPackage(pkg: J.Package, p: ExecutionContext): J.Package {
                    if (pkg.expression.printTrimmed(cursor) == "a") {
                        return pkg.withTemplate(t, pkg.coordinates.replace())
                    }
                    return super.visitPackage(pkg, p)
                }

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext,
                ): J.ClassDeclaration {
                    var cd = super.visitClassDeclaration(classDecl, p)
                    if (classDecl.type!!.packageName == "a") {
                        cd = cd.withType(cd.type!!.withFullyQualifiedName("b.${cd.simpleName}"))
                    }
                    return cd
                }
            }
        })},
        java("""
            package a;
            class Test {
            }
        """,
        """
            package b;
            class Test {
            }
        """)
    )

    @Test
    fun replaceMethod() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "int test2(int n) { return n; }").build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext,
                ): J.MethodDeclaration {
                    if (method.simpleName == "test") {
                        return method.withTemplate(t, method.coordinates.replace())
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        }).afterRecipe{
            val cu = it.results.get(0).after as J.CompilationUnit
            val methodType = (cu.classes.first().body.statements.first() as J.MethodDeclaration).methodType!!
            assertThat(methodType.returnType).isEqualTo(JavaType.Primitive.Int)
            assertThat(methodType.parameterTypes).containsExactly(JavaType.Primitive.Int)
        }},
        java("""
            class Test {
                void test() {
                }
            }
        """,
        """
            class Test {
            
                int test2(int n) {
                    return n;
                }
            }
        """)
    )

    @Test
    fun replaceLambdaWithMethodReference() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "Object::toString").build()

                override fun visitLambda(lambda: J.Lambda, p: ExecutionContext): J {
                    return lambda.withTemplate(t, lambda.coordinates.replace())
                }
            }
        })},
        java("""
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = it -> it.toString();
            }
        """,
        """
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = Object::toString;
            }
        """)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @Suppress("UnusedAssignment", "ResultOfMethodCallIgnored", "CodeBlock2Expr")
    fun replaceStatementInLambdaBodySingleStatementBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "return n == 1;").build()

                override fun visitReturn(retrn: J.Return, p: ExecutionContext): J {
                    if (retrn.expression is J.Binary) {
                        val binary = retrn.expression as J.Binary
                        if (binary.right is J.Literal && Integer.valueOf(0) == (binary.right as J.Literal).value) {
                            return retrn.withTemplate(t, retrn.coordinates.replace())
                        }
                    }
                    return retrn
                }
            }
        })},
        java("""
            import java.util.stream.Stream;

            class Test {
                int n;

                void method(Stream<Object> obj) {
                    obj.filter(o -> {
                        return n == 0;
                    });
                }
            }
        """,
        """
            import java.util.stream.Stream;

            class Test {
                int n;

                void method(Stream<Object> obj) {
                    obj.filter(o -> {
                        return n == 1;
                    });
                }
            }
        """)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @Suppress("UnusedAssignment", "ResultOfMethodCallIgnored", "ConstantConditions")
    fun replaceStatementInLambdaBodyWithVariableDeclaredInBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "return n == 1;").build()

                override fun visitReturn(retrn: J.Return, p: ExecutionContext): J {
                    if (retrn.expression is J.Binary) {
                        val binary = retrn.expression as J.Binary
                        if (binary.right is J.Literal && Integer.valueOf(0) == (binary.right as J.Literal).value) {
                            return retrn.withTemplate(t, retrn.coordinates.replace())
                        }
                    }
                    return retrn
                }
            }
        })},
        java("""
            import java.util.stream.Stream;

            class Test {
                static void method(Stream<Object> obj) {
                    obj.filter(o -> {
                        int n = 0;
                        return n == 0;
                    });
                }
            }
        """,
        """
            import java.util.stream.Stream;

            class Test {
                static void method(Stream<Object> obj) {
                    obj.filter(o -> {
                        int n = 0;
                        return n == 1;
                    });
                }
            }
        """)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @Suppress("ResultOfMethodCallIgnored", "UnusedAssignment")
    fun replaceStatementInLambdaBodyMultiStatementBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "#{any(java.lang.String)}.toUpperCase()").build()

                override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                    if (method.simpleName.equals("toLowerCase")) {
                        return method.withTemplate(t, method.coordinates.replace(), method.select)
                    }
                    return super.visitMethodInvocation(method, p)
                }
            }
        })},
        java("""
            import java.util.stream.Stream;

            class Test {
                static void method(Stream<String> obj) {
                    obj.map(o -> {
                        String str = o;
                        str = o.toLowerCase();
                        return str;
                    });
                }
            }
        """,
        """
            import java.util.stream.Stream;

            class Test {
                static void method(Stream<String> obj) {
                    obj.map(o -> {
                        String str = o;
                        str = o.toUpperCase();
                        return str;
                    });
                }
            }
        """)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @Suppress("ResultOfMethodCallIgnored", "SizeReplaceableByIsEmpty")
    fun replaceSingleExpressionInLambdaBody() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "#{any(java.lang.String)}.toUpperCase()").build()

                override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                    if (method.simpleName.equals("toLowerCase")) {
                        return method.withTemplate(t, method.coordinates.replace(), method.select)
                    }
                    return super.visitMethodInvocation(method, p)
                }
            }
        })},
        java("""
            import java.util.stream.Stream;

            class Test {
                static void method(Stream<String> obj) {
                    obj.filter(o -> o.toLowerCase().length() > 0);
                }
            }
        """,
        """
            import java.util.stream.Stream;

            class Test {
                static void method(Stream<String> obj) {
                    obj.filter(o -> o.toUpperCase().length() > 0);
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2176")
    @Test
    fun replaceSingleExpressionInLambdaBodyWithExpression() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val ENUM_EQUALS = MethodMatcher("java.lang.Enum equals(java.lang.Object)")
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "#{any()} == #{any()}").build()

                override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                    if (ENUM_EQUALS.matches(method)) {
                        return method.withTemplate(t, method.coordinates.replace(), method.select, method.arguments[0])
                    }
                    return super.visitMethodInvocation(method, p)
                }
            }
        })},
        java("""
            import java.util.stream.Stream;

            class Test {
                enum Abc {A,B,C}
                static void method(Stream<Abc> obj) {
                    Object a = obj.filter(o -> o.equals(Abc.A));
                }
            }
        """,
        """
            import java.util.stream.Stream;

            class Test {
                enum Abc {A,B,C}
                static void method(Stream<Abc> obj) {
                    Object a = obj.filter(o -> o == Abc.A);
                }
            }
        """)
    )

    @Suppress("ClassInitializerMayBeStatic")
    @Test
    fun replaceMethodNameAndArgumentsSimultaneously() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "acceptString(#{any()}.toString())")
                    .javaParser {
                        JavaParser.fromJavaVersion()
                            .dependsOn(
                                """
                            package org.openrewrite;
                            public class A {
                                public A acceptInteger(Integer i) { return this; }
                                public A acceptString(String s) { return this; }
                                public A someOtherMethod() { return this; }
                            }
                        """
                            )
                            .build()
                    }
                    .build()

                override fun visitMethodInvocation(
                    method: J.MethodInvocation,
                    p: ExecutionContext
                ): J.MethodInvocation {
                    var m: J.MethodInvocation = super.visitMethodInvocation(method, p)
                    if (m.simpleName.equals("acceptInteger")) {
                        m = m.withTemplate(t, m.coordinates.replaceMethod(), m.arguments[0])
                    }
                    return m
                }
            }
        })},
        java(
            """
                package org.openrewrite;
                public class A {
                    public A acceptInteger(Integer i) { return this; }
                    public A acceptString(String s) { return this; }
                    public A someOtherMethod() { return this; }
                }
            """
        ),
        java("""
            package org.openrewrite;
            
            public class Foo {
                {
                    Integer i = 1;
                    new A().someOtherMethod()
                            .acceptInteger(i)
                            .someOtherMethod();
                }
            }
        """,
        """
            package org.openrewrite;
            
            public class Foo {
                {
                    Integer i = 1;
                    new A().someOtherMethod()
                            .acceptString(i.toString())
                            .someOtherMethod();
                }
            }
        """)
    )

    @Test
    fun replaceMethodInvocationWithArray() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "#{anyArray(int)}").build()

                override fun visitMethodInvocation(
                    method: J.MethodInvocation,
                    p: ExecutionContext
                ): J.MethodInvocation {
                    var m: J.MethodInvocation = super.visitMethodInvocation(method, p)
                    if (m.simpleName.equals("method") && m.arguments.size == 2) {
                        m = m.withTemplate(t, m.coordinates.replaceArguments(), m.arguments[0])
                    }
                    return m
                }
            }
        })},
        java("""
            package org.openrewrite;
            public class Test {
                public void method(int[] val) {}
                public void method(int[] val1, String val2) {}
            }
        """),
        java("""
            import org.openrewrite.Test;
            class A {
                public void method() {
                    Test test = new Test();
                    int[] arr = new int[]{};
                    test.method(arr, null);
                }
            }
        """,
        """
            import org.openrewrite.Test;
            class A {
                public void method() {
                    Test test = new Test();
                    int[] arr = new int[]{};
                    test.method(arr);
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/602")
    @Test
    fun replaceMethodInvocationWithMethodReference() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "Object::toString").build()

                override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                    return method.withTemplate(t, method.coordinates.replace())
                }

            }
        })},
        java("""
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = getToString();
                
                static Function<Object, String> getToString() {
                    return Object::toString;
                } 
            }
        """,
        """
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = Object::toString;
                
                static Function<Object, String> getToString() {
                    return Object::toString;
                } 
            }
        """)
    )

    @Test
    fun replaceMethodParameters() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "int m, java.util.List<String> n")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
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
            }
        }).afterRecipe{
            val cu = it.results[0].after as J.CompilationUnit
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).methodType!!

            assertThat(type.parameterNames)
                .`as`("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("m", "n")
            assertThat(type.parameterTypes[0])
                .`as`("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                .isEqualTo(JavaType.Primitive.Int)
            assertThat(type.parameterTypes[1])
                .matches(
                    {
                        it is JavaType.Parameterized
                                && it.type.fullyQualifiedName == "java.util.List"
                                && it.typeParameters.size == 1
                                && it.typeParameters.first()
                            .asFullyQualified()!!.fullyQualifiedName == "java.lang.String"
                    },
                    "Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'"
                )
        }},
        java(
            """
            class Test {
                void test() {
                    new Runnable() {
                        void inner() {
                        }
                        @Override 
                        public void run() {}
                    };
                }
            }
        """,
        """
            class Test {
                void test(int m, java.util.List<String> n) {
                    new Runnable() {
                        void inner(int m, java.util.List<String> n) {
                        }
                        @Override 
                        public void run() {}
                    };
                }
            }
        """),
    )

    @Test
    fun replaceMethodParametersVariadicArray() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "Object[]... values")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
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
            }
        }).afterRecipe{
            val cu = it.results[0].after as J.CompilationUnit
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).methodType!!

            assertThat(type.parameterNames)
                .`as`("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("values")
            val param = type.parameterTypes[0]
            assertThat(param.asArray()!!.elemType)
                .`as`("Changing the method's parameters should have resulted in the first parameter's type being 'Object[]'")
                .matches { it.asArray()!!.elemType.asFullyQualified()?.fullyQualifiedName == "java.lang.Object" }
        }},
        java("""
            class Test {
                void test() {
                    new Runnable() {
                        void inner() {
                        }
                    };
                }
            }
        """,
        """
            class Test {
                void test(Object[]... values) {
                    new Runnable() {
                        void inner(Object[]... values) {
                        }
                    };
                }
            }
        """)
    )

    @Test
    fun replaceAndInterpolateMethodParameters() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "int n, #{}")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.simpleName == "test" && method.parameters.size == 1) {
                        return method.withTemplate(
                            t,
                            method.coordinates.replaceParameters(),
                            method.parameters[0]
                        )
                    }
                    return method
                }
            }
        }).afterRecipe{
            val cu = it.results[0].after as J.CompilationUnit
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).methodType!!

            assertThat(type.parameterNames)
                .`as`("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("n", "s")
            assertThat(type.parameterTypes[0])
                .`as`("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                .isEqualTo(JavaType.Primitive.Int)
            assertThat(type.parameterTypes[1])
                .`as`("Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'")
                .matches { it.asFullyQualified()!!.fullyQualifiedName == "java.lang.String" }
        }},
        java("""
            class Test {
                void test(String s) {
                }
            }
        """,
        """
            class Test {
                void test(int n, String s) {
                }
            }
        """)
    )

    @Test
    fun replaceLambdaParameters() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "int m, int n")
                    .build()

                override fun visitLambda(lambda: J.Lambda, p: ExecutionContext): J.Lambda =
                    if (lambda.parameters.parameters.size == 1) {
                        lambda.withTemplate(t, lambda.parameters.coordinates.replace())
                    } else {
                        super.visitLambda(lambda, p)
                    }
            }
        })},
        java("""
            class Test {
                void test() {
                    Object o = () -> 1;
                }
            }
        """,
        """
            class Test {
                void test() {
                    Object o = (int m, int n) -> 1;
                }
            }
        """)
    )

    @Test
    fun replaceSingleStatement() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder(
                    { cursor.parentOrThrow },
                    "if(n != 1) {\n" +
                            "  n++;\n" +
                            "}"
                )
                    .build()

                override fun visitAssert(_assert: J.Assert, p: ExecutionContext): J =
                    _assert.withTemplate(t, _assert.coordinates.replace())
            }
        })},
        java("""
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        """
            class Test {
                int n;
                void test() {
                    if (n != 1) {
                        n++;
                    }
                }
            }
        """)
    )

    @Suppress("UnusedAssignment")
    @Test
    fun replaceStatementInBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "n = 2;\nn = 3;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    val statement = method.body!!.statements[1]
                    if (statement is J.Unary) {
                        return method.withTemplate(t, statement.coordinates.replace())
                    }
                    return method
                }
            }
        })},
        java("""
            class Test {
                int n;
                void test() {
                    n = 1;
                    n++;
                }
            }
        """,
        """
            class Test {
                int n;
                void test() {
                    n = 1;
                    n = 2;
                    n = 3;
                }
            }
        """)
    )

    @Test
    fun beforeStatementInBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "assert n == 0;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    val statement = method.body!!.statements[0]
                    if (statement is J.Assignment) {
                        return method.withTemplate(t, statement.coordinates.before())
                    }
                    return method
                }
            }
        })},
        java("""
            class Test {
                int n;
                void test() {
                    n = 1;
                }
            }
        """,
        """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                    n = 1;
                }
            }
        """)
    )

    @Test
    fun afterStatementInBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "n = 1;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    if (method.body!!.statements.size == 1) {
                        return method.withTemplate(t, method.body!!.statements[0].coordinates.after())
                    }
                    return method
                }
            }
        })},
        java("""
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                    n = 1;
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1093")
    @Test
    fun firstStatementInClassBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "int m;")
                    .build()

                override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J {
                    if (classDecl.body.statements.size == 1) {
                        return classDecl.withTemplate(t, classDecl.body.coordinates.firstStatement())
                    }
                    return classDecl
                }
            }
        })},
        java("""
            class Test {
                // comment
                int n;
            }
        """,
        """
            class Test {
                int m;
                // comment
                int n;
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1093")
    @Test
    fun firstStatementInMethodBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "int m = 0;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    if (method.body!!.statements.size == 1) {
                        return method.withTemplate(t, method.body!!.coordinates.firstStatement())
                    }
                    return method
                }
            }
        })},
        java("""
            class Test {
                int n;
                void test() {
                    // comment
                    int n = 1;
                }
            }
        """,
        """
            class Test {
                int n;
                void test() {
                    int m = 0;
                    // comment
                    int n = 1;
                }
            }
        """)
    )

    @Test
    fun lastStatementInClassBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "int n;")
                    .build()

                override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J {
                    if (classDecl.body.statements.isEmpty()) {
                        return classDecl.withTemplate(t, classDecl.body.coordinates.lastStatement())
                    }
                    return classDecl
                }
            }
        })},
        java("""
            class Test {
            }
        """,
        """
            class Test {
                int n;
            }
        """)
    )

    @Test
    fun lastStatementInMethodBlock() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "n = 1;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    if (method.body!!.statements.size == 1) {
                        return method.withTemplate(t, method.body!!.coordinates.lastStatement())
                    }
                    return method
                }
            }
        })},
        java("""
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                    n = 1;
                }
            }
        """)
    )

    @Test
    fun replaceStatementRequiringNewImport() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "List<String> s = null;")
                    .imports("java.util.List")
                    .build()

                override fun visitAssert(_assert: J.Assert, p: ExecutionContext): J {
                    maybeAddImport("java.util.List")
                    return _assert.withTemplate(t, _assert.coordinates.replace())
                }
            }
        })},
        java("""
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        """
            import java.util.List;
            
            class Test {
                int n;
                void test() {
                    List<String> s = null;
                }
            }
        """)
    )

    @Suppress("UnnecessaryBoxing")
    @Test
    fun replaceArguments() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "m, Integer.valueOf(n), \"foo\"")
                    .build()

                override fun visitMethodInvocation(
                    method: J.MethodInvocation,
                    p: ExecutionContext
                ): J.MethodInvocation {
                    if (method.arguments.size == 1) {
                        return method.withTemplate(t, method.coordinates.replaceArguments())
                    }
                    return method
                }
            }
        }).afterRecipe{
            val cu = it.results[0].after as J.CompilationUnit
            val m = (cu.classes[0].body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.MethodInvocation
            val type = m.methodType!!
            assertThat(type.parameterTypes[0]).isEqualTo(JavaType.Primitive.Int)
            assertThat(type.parameterTypes[1]).isEqualTo(JavaType.Primitive.Int)
            assertThat(type.parameterTypes[2]).matches { it.asFullyQualified()!!.fullyQualifiedName.equals("java.lang.String") }
        }},
        java("""
            abstract class Test {
                abstract void test();
                abstract void test(int m, int n, String foo);
                void fred(int m, int n, String foo) {
                    test();
                }
            }
        """,
        """
            abstract class Test {
                abstract void test();
                abstract void test(int m, int n, String foo);
                void fred(int m, int n, String foo) {
                    test(m, Integer.valueOf(n), "foo");
                }
            }
        """)
    )

    val replaceAnnotationRecipe: Recipe
        get() = RewriteTest.toRecipe{object : JavaIsoVisitor<ExecutionContext>() {
            val t = JavaTemplate.builder({ cursor.parentOrThrow }, "@Deprecated")
                .build()

            override fun visitAnnotation(annotation: J.Annotation, p: ExecutionContext): J.Annotation {
                if (annotation.simpleName == "SuppressWarnings") {
                    return annotation.withTemplate(t, annotation.coordinates.replace())
                } else if (annotation.simpleName == "A1") {
                    return annotation.withTemplate(JavaTemplate.builder({ cursor.parentOrThrow }, "@A2")
                        .build(), annotation.coordinates.replace())
                }
                return super.visitAnnotation(annotation, p)
            }
        }}
    @Test
    fun replaceClassAnnotation() = rewriteRun(
        { spec -> spec.recipe(replaceAnnotationRecipe)},
        java("""@SuppressWarnings("ALL") class Test {}""",
        """@Deprecated class Test {}""")
    )

    @Test
    fun replaceMethodDeclarationAnnotation() = rewriteRun(
        { spec -> spec.recipe(replaceAnnotationRecipe)},
        java(
            """
                class A {
                    @SuppressWarnings("ALL")
                    void someTest() {}
                }
            """,
            """
                class A {
                    @Deprecated
                    void someTest() {}
                }
            """
        )
    )

    @Test
    fun replaceVariableDeclarationAnnotation() = rewriteRun(
        { spec -> spec.recipe(replaceAnnotationRecipe)},
        java(
            """
                class A {
                    @interface A1{}
                    @interface A2{}
                    
                    @A1
                    Object someObject;
                }
            """,
            """
                class A {
                    @interface A1{}
                    @interface A2{}
                    
                    @A2
                    Object someObject;
                }
            """
        )
    )

    @Test
    fun replaceMethodDeclarationVariableDeclarationAnnotation() = rewriteRun(
        { spec -> spec.recipe(replaceAnnotationRecipe)},
        java(
            """
                class A {
                    @interface A1{}
                    @interface A2{}
                    
                    void someMethod(@A1 String a){}
                }
            """,
            """
                class A {
                    @interface A1{}
                    @interface A2{}
                    
                    void someMethod(@A2 String a){}
                }
            """
        )
    )

    @Test
    fun replaceMethodAnnotations() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "@SuppressWarnings(\"other\")")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.leadingAnnotations.size == 0) {
                        return method.withTemplate(t, method.coordinates.replaceAnnotations())
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        })},
        java("""
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
        """
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
        """)
    )

    @Test
    fun replaceClassAnnotations() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "@SuppressWarnings(\"other\")")
                    .build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    if (classDecl.leadingAnnotations.size == 0 && classDecl.simpleName != "Test") {
                        return classDecl.withTemplate(t, classDecl.coordinates.replaceAnnotations())
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        })},
        java("""
            class Test {
                static final String WARNINGS = "ALL";
                
                class Inner1 {
                }
            }
        """,
        """
            class Test {
                static final String WARNINGS = "ALL";
            
                @SuppressWarnings("other")
                class Inner1 {
                }
            }
        """)
    )

    @Test
    fun replaceVariableAnnotations() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "@SuppressWarnings(\"other\")")
                    .build()

                override fun visitVariableDeclarations(
                    multiVariable: J.VariableDeclarations,
                    p: ExecutionContext,
                ): J.VariableDeclarations {
                    if (multiVariable.leadingAnnotations.size == 0) {
                        return multiVariable.withTemplate(t, multiVariable.coordinates.replaceAnnotations())
                    }
                    return super.visitVariableDeclarations(multiVariable, p)
                }
            }
        })},
        java("""
            class Test {
                void test() {
                    // the m
                    int m;
                    final @SuppressWarnings("ALL") int n;
                }
            }
        """,
        """
            class Test {
                void test() {
                    // the m
                    @SuppressWarnings("other")
                    int m;
                    @SuppressWarnings("other")
                    final int n;
                }
            }
        """)
    )

    @Test
    fun addVariableAnnotationsToVariableAlreadyAnnotated() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "@Deprecated")
                    .build()

                override fun visitVariableDeclarations(
                    multiVariable: J.VariableDeclarations,
                    p: ExecutionContext,
                ): J.VariableDeclarations {
                    if (multiVariable.leadingAnnotations.size == 1) {
                        return multiVariable.withTemplate(t, multiVariable.coordinates.addAnnotation(comparing { 0 }))
                    }
                    return super.visitVariableDeclarations(multiVariable, p)
                }
            }
        })},
        java("""
            class Test {
                @SuppressWarnings("ALL") private final int m, a;
                void test() {
                    @SuppressWarnings("ALL") /* hello */
                    Boolean z;
                    // comment n
                    @SuppressWarnings("ALL")
                    int n;
                    @SuppressWarnings("ALL") final Boolean b;
                    @SuppressWarnings("ALL")
                    // comment x, y
                    private Boolean x, y;
                }
            }
        """,
        """
            class Test {
                @SuppressWarnings("ALL")
                @Deprecated
                private final int m, a;
                void test() {
                    @SuppressWarnings("ALL")
                    @Deprecated /* hello */
                    Boolean z;
                    // comment n
                    @SuppressWarnings("ALL")
                    @Deprecated
                    int n;
                    @SuppressWarnings("ALL")
                    @Deprecated
                    final Boolean b;
                    @SuppressWarnings("ALL")
                    @Deprecated
                    // comment x, y
                    private Boolean x, y;
                }
            }
        """)
    )

    @Test
    fun addVariableAnnotationsToVariableNotAnnotated() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "@SuppressWarnings(\"ALL\")")
                    .build()

                override fun visitVariableDeclarations(
                    multiVariable: J.VariableDeclarations,
                    p: ExecutionContext,
                ): J.VariableDeclarations {
                    if (multiVariable.leadingAnnotations.size == 0) {
                        return multiVariable.withTemplate(
                            t,
                            multiVariable.coordinates.addAnnotation(comparing { it.simpleName })
                        )
                    }
                    return super.visitVariableDeclarations(multiVariable, p)
                }
            }
        })},
        java("""
            class Test {
                void test() {
                    final int m;
                    int n;
                }
            }
        """,
        """
            class Test {
                void test() {
                    @SuppressWarnings("ALL")
                    final int m;
                    @SuppressWarnings("ALL")
                    int n;
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1111")
    @Test
    fun addMethodAnnotations() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "@SuppressWarnings(\"other\")")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.leadingAnnotations.size == 0) {
                        return method.withTemplate(t, method.coordinates.addAnnotation(comparing { it.simpleName }))
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        })},
        java("""
            class Test {
                public void test0() {
                }

                static final String WARNINGS = "ALL";

                void test1() {
                }
            }
        """,
        """
            class Test {
                @SuppressWarnings("other")
                public void test0() {
                }

                static final String WARNINGS = "ALL";

                @SuppressWarnings("other")
                void test1() {
                }
            }
        """)
    )

    @Test
    fun addClassAnnotations() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "@SuppressWarnings(\"other\")")
                    .build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    if (classDecl.leadingAnnotations.size == 0 && classDecl.simpleName != "Test") {
                        return classDecl.withTemplate(
                            t,
                            classDecl.coordinates.addAnnotation(comparing { it.simpleName })
                        )
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        })},
        java("""
            class Test {
                class Inner1 {
                }
            }
        """,
        """
            class Test {
                @SuppressWarnings("other")
                class Inner1 {
                }
            }
        """)
    )

    @Test
    fun replaceAnnotation() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "@Deprecated").build()

                override fun visitAnnotation(annotation: J.Annotation, p: ExecutionContext): J.Annotation {
                    if(annotation.simpleName == "SuppressWarnings") {
                        return annotation.withTemplate(t, annotation.coordinates.replace())
                    }
                    return annotation
                }
            }
        })},
        java("""
            @SuppressWarnings("ALL")
            class Test {
            }
        """,
        """
            @Deprecated
            class Test {
            }
        """)
    )

    @Test
    fun replaceClassImplements() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "Serializable, Closeable")
                    .imports("java.io.*")
                    .build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    if (classDecl.implements == null) {
                        maybeAddImport("java.io.Closeable")
                        maybeAddImport("java.io.Serializable")
                        return classDecl.withTemplate(t, classDecl.coordinates.replaceImplementsClause())
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        })},
        java("""
            class Test {
            }
        """,
        """
            import java.io.Closeable;
            import java.io.Serializable;
            
            class Test implements Serializable, Closeable {
            }
        """)
    )

    @Test
    fun replaceClassExtends() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "List<String>")
                    .imports("java.util.*")
                    .build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    if (classDecl.extends == null) {
                        maybeAddImport("java.util.List")
                        return classDecl.withTemplate(t, classDecl.coordinates.replaceExtendsClause())
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        })},
        java("""
            class Test {
            }
        """,
        """
            import java.util.List;
            
            class Test extends List<String> {
            }
        """)
    )

    @Suppress("RedundantThrows")
    @Test
    fun replaceThrows() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "Exception")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.throws == null) {
                        return method.withTemplate(t, method.coordinates.replaceThrows())
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        }).afterRecipe{
            val cu = it.results[0].after as J.CompilationUnit
            val testMethodDecl = cu.classes.first().body.statements.first() as J.MethodDeclaration
            assertThat(testMethodDecl.methodType!!.thrownExceptions.map { it.fullyQualifiedName })
                .containsExactly("java.lang.Exception")
        }},
        java("""
            class Test {
                void test() {}
            }
        """,
        """
            class Test {
                void test() throws Exception {}
            }
        """)
    )

    @Disabled
    @Test
    fun replaceMethodTypeParameters() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val typeParamsTemplate = JavaTemplate.builder({ cursor.parentOrThrow }, "T, U")
                    .build()

                val methodArgsTemplate = JavaTemplate.builder({ cursor.parentOrThrow }, "List<T> t, U u")
                    .imports("java.util.List")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.typeParameters == null) {
                        return method.withTemplate<J.MethodDeclaration>(
                            typeParamsTemplate,
                            method.coordinates.replaceTypeParameters()
                        )
                            .withTemplate(methodArgsTemplate, method.coordinates.replaceParameters())
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        }).afterRecipe{
            val cu = it.results[0].after as J.CompilationUnit
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).methodType!!
            assertThat(type).isNotNull
            val paramTypes = type.parameterTypes

            assertThat(paramTypes[0])
                .`as`("The method declaration's type's genericSignature first argument should have have type 'java.util.List'")
                .matches { tType ->
                    tType is JavaType.FullyQualified && tType.fullyQualifiedName == "java.util.List"
                }

            assertThat(paramTypes[1])
                .`as`("The method declaration's type's genericSignature second argument should have type 'U' with bound 'java.lang.Object'")
                .matches { uType ->
                    uType is JavaType.GenericTypeVariable &&
                            uType.name == "U" &&
                            uType.bounds.isEmpty()
                }
        }},
        java("""
            import java.util.List;
            
            class Test {
            
                void test() {
                }
            }
        """,
        """
            import java.util.List;
            
            class Test {
            
                <T, U> void test(List<T> t, U u) {
                }
            }
        """)
    )

    @Test
    fun replaceClassTypeParameters() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "T, U")
                    .build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    if (classDecl.typeParameters == null) {
                        return classDecl.withTemplate(t, classDecl.coordinates.replaceTypeParameters())
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        })},
        java("""
            class Test {
            }
        """,
        """
            class Test<T, U> {
            }
        """)
    )

    @Test
    fun replaceBody() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "n = 1;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    val statement = method.body!!.statements[0]
                    if (statement is J.Unary) {
                        return method.withTemplate(t, method.coordinates.replaceBody())
                    }
                    return method
                }
            }
        })},
        java("""
            class Test {
                int n;
                void test() {
                    n++;
                }
            }
        """,
        """
            class Test {
                int n;
                void test() {
                    n = 1;
                }
            }
        """)
    )

    @Test
    fun replaceMissingBody() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    var m = method
                    if (!m.isAbstract) {
                        return m
                    }
                    m = m.withReturnTypeExpression(m.returnTypeExpression!!.withPrefix(Space.EMPTY))
                    m = m.withModifiers(emptyList())

                    m = m.withTemplate(t, m.coordinates.replaceBody())

                    return m
                }
            }
        })},
        java("""
            abstract class Test {
                abstract void test();
            }
        """,
        """
            abstract class Test {
                void test(){
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1198")
    @Test
    @Suppress(
        "UnnecessaryBoxing",
        "UnnecessaryLocalVariable",
        "CachedNumberConstructorCall",
        "UnnecessaryTemporaryOnConversionToString",
        "ResultOfMethodCallIgnored"
    )
    fun replaceNamedVariableInitializerMethodInvocation() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val matcher = MethodMatcher("Integer valueOf(..)")
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "new Integer(#{any()})").build()
                override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                    if (matcher.matches(method)) {
                        return method.withTemplate(t, method.coordinates.replace(), method.arguments[0])
                    }
                    return super.visitMethodInvocation(method, p)
                }
            }
        })},
        java("""
            import java.util.Arrays;
            import java.util.List;
            import java.util.function.Function;
            class Test {
                void t() {
                    List<String> nums = Arrays.asList("1", "2", "3");
                    nums.forEach(s -> Integer.valueOf(s));
                }
                void inLambda(int i) {
                    Function<String, Integer> toString = it -> {
                        try {
                            return Integer.valueOf(it);
                        }catch (NumberFormatException ex) {
                            ex.printStackTrace();
                        }
                        return 0;
                    };
                }
                String inClassDeclaration(int i) {
                    return new Object() {
                        void foo() {
                            Integer.valueOf(i);
                        }
                    }.toString();
                }
            }
        """,
        """
            import java.util.Arrays;
            import java.util.List;
            import java.util.function.Function;
            class Test {
                void t() {
                    List<String> nums = Arrays.asList("1", "2", "3");
                    nums.forEach(s -> new Integer(s));
                }
                void inLambda(int i) {
                    Function<String, Integer> toString = it -> {
                        try {
                            return new Integer(it);
                        }catch (NumberFormatException ex) {
                            ex.printStackTrace();
                        }
                        return 0;
                    };
                }
                String inClassDeclaration(int i) {
                    return new Object() {
                        void foo() {
                            new Integer(i);
                        }
                    }.toString();
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1198")
    @Test
    @Suppress(
        "UnnecessaryBoxing",
        "UnnecessaryLocalVariable",
        "CachedNumberConstructorCall",
        "UnnecessaryTemporaryOnConversionToString"
    )
    fun lambdaIsVariableInitializer() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val matcher = MethodMatcher("Integer valueOf(..)")
                val t = JavaTemplate.builder({ cursor.parentOrThrow }, "new Integer(#{any()})").build()
                override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                    if (matcher.matches(method)) {
                        return method.withTemplate(t, method.coordinates.replace(), method.arguments[0])
                    }
                    return super.visitMethodInvocation(method, p)
                }
            }
        })},
        java("""
            import java.util.function.Function;
            class Test {
                Function<String, Integer> asInteger = it -> Integer.valueOf(it);
            }
        """,
        """
            import java.util.function.Function;
            class Test {
                Function<String, Integer> asInteger = it -> new Integer(it);
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1505")
    @Test
    fun methodDeclarationWithComment() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                override fun visitClassDeclaration(classDeclaration: J.ClassDeclaration, p: ExecutionContext): J {
                    var cd = classDeclaration
                    if (cd.body.statements.isEmpty()) {
                        cd = cd.withBody(
                            cd.body.withTemplate(
                                JavaTemplate.builder(
                                    { cursor.parentOrThrow }, """
                                /**
                                 * comment
                                 */
                                void foo() {
                                }
                            """
                                )
                                    .build(),
                                cd.body.coordinates.firstStatement()
                            )
                        )
                    }
                    return cd
                }
            }
        })},
        java("""
            class A {
            
            }
        """,
        """
            class A {
                /**
                 * comment
                 */
                void foo() {
                }

            }
        """)
    )

    @Suppress("UnusedAssignment")
    @Issue("https://github.com/openrewrite/rewrite/issues/1821")
    @Test
    fun assignmentNotPartOfVariableDeclaration() = rewriteRun(
        {spec-> spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitAssignment(assignment: J.Assignment, p: ExecutionContext): J.Assignment {
                    var a = assignment

                    if(a.assignment is J.MethodInvocation) {
                        val mi = a.assignment as J.MethodInvocation
                        a = a.withAssignment(mi.withTemplate(
                            JavaTemplate.builder(this::getCursor, "1")
                                .build(),
                            mi.coordinates.replace()
                        ))
                    }
                    return a
                }
            }
        })},
        java("""
            class A {
                void foo() {
                    int i;
                    i = Integer.valueOf(1);
                }
            }
        """,
        """
            class A {
                void foo() {
                    int i;
                    i = 1;
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2090")
    @Test
    fun assignmentWithinIfPredicate() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe {
                object : JavaIsoVisitor<ExecutionContext>() {
                    override fun visitAssignment(assignment: J.Assignment, p: ExecutionContext): J.Assignment {
                        if((assignment.assignment is J.Literal) && "1" == (assignment.assignment as J.Literal).valueSource) {
                            return assignment.withTemplate(
                                JavaTemplate.builder(this::getCursor, "value = 0").build(),
                                assignment.coordinates.replace()
                            )
                        }
                        return assignment
                    }
                }
            })
        },
        java("""
            class A {
                void foo() {
                    double value = 0;
                    if ((value = 1) == 0) {}
                }
            }
        """,
        """
            class A {
                void foo() {
                    double value = 0;
                    if ((value = 0) == 0) {}
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/66")
    @Test
    fun lambdaIsNewClass() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe {
                object : JavaIsoVisitor<ExecutionContext>() {
                    override fun visitAssignment(assignment: J.Assignment, p: ExecutionContext): J.Assignment {
                        var a = assignment
                        if(a.assignment is J.MethodInvocation) {
                            val mi = a.assignment as J.MethodInvocation
                            a = a.withAssignment(mi.withTemplate(
                                JavaTemplate.builder(this::getCursor, "1").build(), mi.coordinates.replace()
                            ))
                        }
                        return a
                    }
                }
            })
        },
        java(
            """
            class T {
                public T (int a, Runnable r, String s) { }
                static void method() {
                    new T(1, () -> {
                        int i;
                        i = Integer.valueOf(1);
                    }, "hello" );
                }
            }
            """,
            """
            class T {
                public T (int a, Runnable r, String s) { }
                static void method() {
                    new T(1, () -> {
                        int i;
                        i = 1;
                    }, "hello" );
                }
            }
            """
        )
    )

    @Suppress("RedundantOperationOnEmptyContainer", "RedundantOperationOnEmptyContainer")
    @Test
    fun replaceForEachControlVariable() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe{
                object : JavaIsoVisitor<ExecutionContext>() {
                    override fun visitVariableDeclarations(
                        multiVariable: J.VariableDeclarations,
                        p: ExecutionContext
                    ): J.VariableDeclarations {
                        var mv = super.visitVariableDeclarations(multiVariable, p)
                        if (mv.variables[0].initializer == null && TypeUtils.isOfType(mv.typeExpression!!.type, JavaType.Primitive.String)) {
                            mv = multiVariable.withTemplate(
                                JavaTemplate.builder(this::getCursor, "Object #{}").build(),
                                multiVariable.coordinates.replace(),
                                multiVariable.variables[0].simpleName
                            )
                        }
                        return mv
                    }
                }
            })
        },
        java(
            """
            import java.util.ArrayList;
            class T {
                void m() {
                    for (String s : new ArrayList<String>()) {}
                }
            }
            """,
            """
            import java.util.ArrayList;
            class T {
                void m() {
                    for (Object s : new ArrayList<String>()) {}
                }
            }
            """
        )
    )

    @Suppress("StatementWithEmptyBody", "RedundantOperationOnEmptyContainer")
    @Test
    fun replaceForEachControlIterator() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe{
                object : JavaVisitor<ExecutionContext>() {
                    override fun visitNewClass(newClass: J.NewClass, p: ExecutionContext): J {
                        var nc = super.visitNewClass(newClass, p)
                        if (TypeUtils.isOfClassType(newClass.type, "java.util.ArrayList")) {
                            nc = nc.withTemplate(JavaTemplate.builder(this::getCursor,"Collections.emptyList()")
                                .imports("java.util.Collections").build(),
                                newClass.coordinates.replace())
                        }
                        return nc
                    }
                }
            })
        },
        java(
            """
            import java.util.ArrayList;
            import java.util.Collections;
            class T {
                void m() {
                    for (String s : new ArrayList<String>()) {}
                }
            }
            """,
            """
            import java.util.ArrayList;
            import java.util.Collections;
            class T {
                void m() {
                    for (String s : Collections.emptyList()) {}
                }
            }
            """
        )
    )

    @Suppress("StringOperationCanBeSimplified")
    @Issue("https://github.com/openrewrite/rewrite/issues/2185")
    @Test
    fun chainedMethodInvocationsAsNewClassArgument() = rewriteRun(
        { spec -> spec.recipe(replaceToStringWithLiteralRecipe)
        },
        java(
            """
            import java.util.ArrayList;
            import java.util.Collections;
            public class T {
                void m(String arg) {
                    U u = new U(arg.toString().toCharArray());
                }
                class U {
                    U(char[] chars){}
                }
            }
            """,
            """
            import java.util.ArrayList;
            import java.util.Collections;
            public class T {
                void m(String arg) {
                    U u = new U(arg.toCharArray());
                }
                class U {
                    U(char[] chars){}
                }
            }
            """
        )
    )

    @Test
    fun chainedMethodInvocationsAsNewClassArgument2() = rewriteRun(
        { spec -> spec.recipe(replaceToStringWithLiteralRecipe)},
        java(
            """
            class T {
                void m(String jsonPayload) {
                    HttpEntity entity = new HttpEntity(jsonPayload.toString(), 0);
                }
                class HttpEntity {
                    HttpEntity(String s, int i){}
                }
            }
            """,
            """
            class T {
                void m(String jsonPayload) {
                    HttpEntity entity = new HttpEntity(jsonPayload, 0);
                }
                class HttpEntity {
                    HttpEntity(String s, int i){}
                }
            }
            """
        )
    )

    @Suppress("LoopConditionNotUpdatedInsideLoop")
    @Test
    fun templatingWhileLoopCondition() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe {
                object : JavaVisitor<ExecutionContext>() {
                    override fun visitBinary(binary: J.Binary, p: ExecutionContext): J {
                        if (binary.left is J.MethodInvocation) {
                            val mi = binary.left as J.MethodInvocation
                            return binary.withTemplate(
                                JavaTemplate.builder(this::getCursor, "!#{any(java.util.List)}.isEmpty()")
                                    .build(), mi.coordinates.replace(), mi.select
                            )
                        } else if (binary.left is J.Unary) {
                            return binary.left
                        }
                        return binary
                    }
                }
            })
            spec.expectedCyclesThatMakeChanges(2)
        },
        java("""
            import java.util.List;
            class T {
                void m(List<?> l) {
                    while (l.size() != 0) {}
                }
            }
        """,
        """
            import java.util.List;
            class T {
                void m(List<?> l) {
                    while (!l.isEmpty()) {}
                }
            }
        """)
    )

    @Suppress("BigDecimalLegacyMethod", "deprecation")
    @Test
    fun javaTemplateControlsSemiColons() = rewriteRun(
        { spec ->
            spec.recipe(RewriteTest.toRecipe{
                object : JavaVisitor<ExecutionContext>() {
                    var BIG_DECIMAL_SET_SCALE = MethodMatcher("java.math.BigDecimal setScale(int, int)")
                    var twoArgScale = JavaTemplate.builder({ cursor.parentOrThrow }, "#{any(int)}, #{}")
                        .imports("java.math.RoundingMode").build()

                    override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                        var mi = super.visitMethodInvocation(method, p) as J.MethodInvocation
                        if (BIG_DECIMAL_SET_SCALE.matches(mi)) {
                            mi = mi.withTemplate(
                                twoArgScale, mi.getCoordinates().replaceArguments(),
                                mi.getArguments().get(0), "RoundingMode.HALF_UP"
                            )
                        }
                        return mi
                    }
                }
            })
        },
        java("""
            import java.math.BigDecimal;
            import java.math.RoundingMode;
            
            class A {
                void m() {
                    StringBuilder sb = new StringBuilder();
                    sb.append((new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue())).append("|");
                }
            }
        """,
            """
            import java.math.BigDecimal;
            import java.math.RoundingMode;
            
            class A {
                void m() {
                    StringBuilder sb = new StringBuilder();
                    sb.append((new BigDecimal(0).setScale(1, RoundingMode.HALF_UP).doubleValue())).append("|");
                }
            }
        """)
    )

    @Test
    fun enumClassWithAnonymousInnerClassConstructor() = rewriteRun(
        { spec -> spec.recipe(replaceToStringWithLiteralRecipe)},
        java(
            """
            enum MyEnum {
                THING_ONE(new MyEnumThing() {
                    @Override 
                    public String getName() {
                        return "Thing One".toString();
                    }
                });
                private final MyEnumThing enumThing;
                MyEnum(MyEnumThing myEnumThing) {
                    this.enumThing = myEnumThing;
                }
                interface MyEnumThing {
                    String getName();
                }
            }
            """,
            """
            enum MyEnum {
                THING_ONE(new MyEnumThing() {
                    @Override 
                    public String getName() {
                        return "Thing One";
                    }
                });
                private final MyEnumThing enumThing;
                MyEnum(MyEnumThing myEnumThing) {
                    this.enumThing = myEnumThing;
                }
                interface MyEnumThing {
                    String getName();
                }
            }
            """
        )
    )

    @Test
    fun replacingMethodInvocationWithinEnum() = rewriteRun(
        { spec -> spec.recipe(replaceToStringWithLiteralRecipe)},
        java("""
            public enum Options {
            
                JAR("instance.jar.file"),
                JVM_ARGUMENTS("instance.vm.args");
            
                private String name;
            
                Options(String name) {
                    this.name = name;
                }
            
                public String asString() {
                    return System.getProperty(name);
                }
                
                public Integer asInteger(int defaultValue) {
                    String string  = asString();
                    return new Integer(string.toString());
                }
            
            }
        """,
            """
            public enum Options {
            
                JAR("instance.jar.file"),
                JVM_ARGUMENTS("instance.vm.args");
            
                private String name;
            
                Options(String name) {
                    this.name = name;
                }
            
                public String asString() {
                    return System.getProperty(name);
                }
                
                public Integer asInteger(int defaultValue) {
                    String string  = asString();
                    return new Integer(string);
                }
            
            }
        """)
    )

    @Test
    fun replacingMethodInvocationWithinInnerEnum() = rewriteRun(
        { spec -> spec.recipe(replaceToStringWithLiteralRecipe)},
        java(
            """
            public class Test {
                void doSomething(Options options) {
                    switch (options) {
                        case JAR:
                        case JVM_ARGUMENTS:
                            System.out.println("");
                    }
                }
                enum Options {
                    JAR(0, "instance.jar.file".toString()),
                    JVM_ARGUMENTS(1, "instance.vm.args");
                
                    private final String name;
                    private final int id;
                
                    Options(int id,String name) {
                        this.id = id;
                        this.name = name;
                    }
                
                    public String asString() {
                        return System.getProperty(name);
                    }
                    
                    public Integer asInteger(int defaultValue) {
                        String string  = asString();
                        return new Integer(string);
                    }
                }
            }
        """,
            """
            public class Test {
                void doSomething(Options options) {
                    switch (options) {
                        case JAR:
                        case JVM_ARGUMENTS:
                            System.out.println("");
                    }
                }
                enum Options {
                    JAR(0, "instance.jar.file"),
                    JVM_ARGUMENTS(1, "instance.vm.args");
                
                    private final String name;
                    private final int id;
                
                    Options(int id,String name) {
                        this.id = id;
                        this.name = name;
                    }
                
                    public String asString() {
                        return System.getProperty(name);
                    }
                    
                    public Integer asInteger(int defaultValue) {
                        String string  = asString();
                        return new Integer(string);
                    }
                }
            }
        """)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2375")
    fun arrayInitializer() = rewriteRun(
        {spec -> spec.recipe(RewriteTest.toRecipe{
            object : JavaIsoVisitor<ExecutionContext>() {
            val mm = MethodMatcher("abc.ArrayHelper of(..)")
            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
                var mi = super.visitMethodInvocation(method, p)
                if (mm.matches(mi)) {
                    mi = mi.withTemplate(
                        JavaTemplate.builder(this::getCursor, "Arrays.asList(#{any(java.lang.Integer)}, #{any(java.lang.Integer)}, #{any(java.lang.Integer)})")
                            .imports("java.util.Arrays").build(),
                        mi.getCoordinates().replace(), mi.arguments[0], mi.arguments[1], mi.arguments[2]);
                }
                return mi
            }
        }})},
        java("""
            package abc;
            
            public class ArrayHelper {
                public static Object[] of(Object... objects){ return null;}
            }
        """),
        java("""
            import abc.ArrayHelper;
            import java.util.Arrays;
            
            class A {
                Object[] o = new Object[] {
                    ArrayHelper.of(1, 2, 3)
                };
            }
        """,
            """
            import abc.ArrayHelper;
            import java.util.Arrays;
            
            class A {
                Object[] o = new Object[] {
                        Arrays.asList(1, 2, 3)
                };
            }
        """
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2375")
    fun multiDimentionalArrayInitializer() = rewriteRun(
        {spec -> spec.recipe(RewriteTest.toRecipe {object : JavaVisitor<ExecutionContext>() {
            val mm = MethodMatcher("java.util.stream.IntStream sum()")
            override fun visitNewClass(newClass: J.NewClass, p: ExecutionContext): J {
                val nc = super.visitNewClass(newClass, p) as J.NewClass
                return nc.withTemplate(
                    JavaTemplate.builder(this::getCursor, "Integer.valueOf(#{any(java.lang.Integer)})")
                        .build(), nc.coordinates.replace(), nc.arguments[0]
                ) as J
            }
        }})},
        java(
            """
            class A {
                Integer[][] num2 = new Integer[][]{ {new Integer(1), new Integer(2)}, {new Integer(1), new Integer(2)} };
            }
        """,
            """
            class A {
                Integer[][] num2 = new Integer[][]{ {Integer.valueOf(1), Integer.valueOf(2)}, {Integer.valueOf(1), Integer.valueOf(2)} };
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2486")
    @Test
    fun dontDropTheAssert() = rewriteRun(
        {spec -> spec.recipe(RewriteTest.toRecipe{
            object : JavaVisitor<ExecutionContext>() {
                override fun visitBinary(binary: J.Binary, p: ExecutionContext): J {
                    val sizeCall = binary.left as J.MethodInvocation
                    return sizeCall.withTemplate<J?>(JavaTemplate.builder({ cursor }, "!#{any(java.util.Collection)}.isEmpty()").build(),
                        sizeCall.coordinates.replace(), sizeCall.select).withPrefix(binary.prefix)
                }
            }
        })},
        java("""
            import java.util.Collection;
            
            class Test {
                void doSomething(Collection<Object> c) {
                    assert c.size() > 0;
                }
            }
        ""","""
            import java.util.Collection;
            
            class Test {
                void doSomething(Collection<Object> c) {
                    assert !c.isEmpty();
                }
            }
        """)
    )
}
