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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.config.CompositeRecipe
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.tree.TypeUtils
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import java.nio.file.Path

interface ChangeTypeTest : JavaRecipeTest, RewriteTest {
    override val recipe: ChangeType
        get() = ChangeType("a.A1","a.A2", true)

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(recipe)
    }

    //language=java
    companion object {

        private const val a1 = """
            package a;
            public class A1 extends Exception {
                public static void stat() {}
                public void foo() {}
            }
        """

        private const val a2 = """
            package a;
            public class A2 extends Exception {
                public static void stat() {}
                public void foo() {}
            }
        """
    }

    @Test
    fun doNotAddJavaLangWrapperImports() = rewriteRun(
        { spec -> spec.recipe(ChangeType("java.lang.Integer","java.lang.Long", true)) },
        java("public class ThinkPositive { private Integer fred = 1;}",
            "public class ThinkPositive { private Long fred = 1;}")
    )

    @Suppress("deprecation", "KotlinRedundantDiagnosticSuppress")
    @Test
    fun allowJavaLangSubpackages() = rewriteRun(
        { spec -> spec.recipe(ChangeType("java.util.logging.LoggingMXBean","java.lang.management.PlatformLoggingMXBean", true)) },
        java("""
            import java.util.logging.LoggingMXBean;

            class Test {
                static void method() {
                    LoggingMXBean loggingBean = null;
                }
            }
        """,
        """
            import java.lang.management.PlatformLoggingMXBean;

            class Test {
                static void method() {
                    PlatformLoggingMXBean loggingBean = null;
                }
            }
        """)
    )

    @Suppress("InstantiationOfUtilityClass")
    @Issue("https://github.com/openrewrite/rewrite/issues/788")
    @Test
    fun unnecessaryImport() = rewriteRun(
        { spec -> spec.recipe(ChangeType("test.Outer.Inner", "java.util.ArrayList", true)) },
        java("""
            import test.Outer;
            
            class Test {
                private Outer p = Outer.of();
                private Outer p2 = test.Outer.of();
            }
        """),
        java("""
            package test;
            
            public class Outer {
                public static Outer of() {
                    return new Outer();
                }
            
                public static class Inner {
                }
            }
        """)
    )

    @Suppress("rawtypes")
    @Issue("https://github.com/openrewrite/rewrite/issues/868")
    @Test
    fun changeInnerClassToOuterClass() = rewriteRun(
        { spec -> spec.recipe(ChangeType("java.util.Map${'$'}Entry", "java.util.List", true)) },
        java("""
            import java.util.Map;
            import java.util.Map.Entry;
            
            class Test {
                Entry p;
                Map.Entry p2;
            }
        """,
        """
            import java.util.List;
            
            class Test {
                List p;
                List p2;
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/768")
    @Test
    fun changeStaticFieldAccess() = rewriteRun(
        { spec -> spec.recipe(ChangeType("java.io.File", "my.pkg.List", true)) },
        java("""
            import java.io.File;
            
            class Test {
                String p = File.separator;
            }
        """,
        """
            import my.pkg.List;
            
            class Test {
                String p = List.separator;
            }
        """)
    )

    @Test
    fun dontAddImportWhenNoChangesWereMade() = rewriteRun(
        java("public class B {}")
    )

    @Suppress("rawtypes")
    @Issue("https://github.com/openrewrite/rewrite/issues/774")
    @Test
    fun replaceWithNestedType() = rewriteRun(
        { spec -> spec.recipe(ChangeType("java.io.File", "java.util.Map${'$'}Entry", true)) },
        java("""
            import java.io.File;
            
            class Test {
                File p;
            }
        """,
        """
            import java.util.Map;
            
            class Test {
                Map.Entry p;
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2521")
    @Test
    fun replacePrivateNestedType() = rewriteRun(
        { spec -> spec.recipe(ChangeType("a.A.B1", "a.A.B2", false)) },
        java("""
            package a;
            
            class A {
                private static class B1 {}
            }
        """,
        """
            package a;
            
            class A {
                private static class B2 {}
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2521")
    @Test
    fun deeplyNestedInnerClass() = rewriteRun(
        { spec -> spec.recipe(ChangeType("a.A.B.C", "a.A.B.C2", false)) },
        java("""
            package a;
            
            class A {
                public static class B {
                    public static class C {
                    }
                }
            }
        """,
            """
            package a;
            
            class A {
                public static class B {
                    public static class C2 {
                    }
                }
            }
        """)
    )

    @Test
    fun simpleName() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class B extends A1 {}
        """,
        """
            import a.A2;
            
            public class B extends A2 {}
        """)
    )

    @Test
    fun fullyQualifiedName() = rewriteRun(
        java(a1),
        java(a2),
        java("public class B extends a.A1 {}",
        "public class B extends a.A2 {}")
    )

    @Test
    fun annotation() = rewriteRun(
        { spec -> spec.recipe(ChangeType("a.b.c.A1","a.b.d.A2", true)) },
        java("package a.b.c;\npublic @interface A1 {}"),
        java("package a.b.d;\npublic @interface A2 {}"),
        java("@a.b.c.A1 public class B {}",
        "@a.b.d.A2 public class B {}")
    )

    @Test
    fun array2() = rewriteRun(
        { spec -> spec.recipe(ChangeType("com.acme.product.Pojo","com.acme.product.v2.Pojo", true)) },
        java("""
            package com.acme.product;
            
            public class Pojo {
            }
        """),
        java("""
            package com.acme.project.impl;
            
            import com.acme.product.Pojo;
            
            public class UsePojo2 {
                Pojo[] p;
            
                void run() {
                    p[0] = null;
                }
            }
        """,
        """
            package com.acme.project.impl;
            
            import com.acme.product.v2.Pojo;
            
            public class UsePojo2 {
                Pojo[] p;
            
                void run() {
                    p[0] = null;
                }
            }
        """)
    )

    // array types and new arrays
    @Test
    fun array() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class B {
               A1[] a = new A1[0];
            }
        """,
        """
            import a.A2;
            
            public class B {
               A2[] a = new A2[0];
            }
        """)
    )

    @Test
    fun multiDimensionalArray() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class A {
                A1[][] multiDimensionalArray;
            }
        """,
        """
            import a.A2;
            
            public class A {
                A2[][] multiDimensionalArray;
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("a.A1")).isEmpty()
            assertThat(cu.findType("a.A2")).isNotEmpty()
        }}
    )

    @Test
    fun classDecl() = rewriteRun(
        { spec -> spec.recipe(CompositeRecipe()
                .doNext(recipe)
                .doNext(ChangeType("I1", "I2", true))) },
        java(a1),
        java(a2),
        java("public interface I1 {}"),
        java("public interface I2 {}"),
        java("""
            import a.A1;
            
            public class B extends A1 implements I1 {}
        """,
        """
            import a.A2;
            
            public class B extends A2 implements I2 {}
        """)
    )

    @Suppress("RedundantThrows")
    @Test
    fun method() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class B {
               public A1 foo() throws A1 { return null; }
            }
        """,
        """
            import a.A2;
            
            public class B {
               public A2 foo() throws A2 { return null; }
            }
        """)
    )

    @Test
    fun methodInvocationTypeParametersAndWildcard() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class B {
               public <T extends A1> T generic(T n, List<? super A1> in) {
               
               }
               public void test() {
                   A1.stat();
                   this.<A1>generic(null, true);
               }
            }
        """,
        """
            import a.A2;
            
            public class B {
               public <T extends A2> T generic(T n, List<? super A2> in) {
               
               }
               public void test() {
                   A2.stat();
                   this.<A2>generic(null, true);
               }
            }
        """)
    )

    @Suppress("EmptyTryBlock", "CatchMayIgnoreException")
    @Test
    fun multiCatch() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class B {
               public void test() {
                   try {}
                   catch(A1 | RuntimeException e) {}
               }
            }
        """,
        """
            import a.A2;
            
            public class B {
               public void test() {
                   try {}
                   catch(A2 | RuntimeException e) {}
               }
            }
        """)
    )

    @Test
    fun multiVariable() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class B {
               A1 f1, f2;
            }
        """,
        """
            import a.A2;
            
            public class B {
               A2 f1, f2;
            }
        """)
    )

    @Test
    fun newClass() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class B {
               A1 a = new A1();
            }
        """,
        """
            import a.A2;
            
            public class B {
               A2 a = new A2();
            }
        """)
    )

    @Suppress("UnnecessaryLocalVariable")
    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/704")
    fun updateAssignments() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;

            class B {
                void method(A1 param) {
                    A1 a = param;
                }
            }
        """,
        """
            import a.A2;

            class B {
                void method(A2 param) {
                    A2 a = param;
                }
            }
        """)
    )

    @Test
    fun parameterizedType() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class B {
               Map<A1, A1> m;
            }
        """,
        """
            import a.A2;
            
            public class B {
               Map<A2, A2> m;
            }
        """)
    )

    @Suppress("RedundantCast")
    @Test
    fun typeCast() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class B {
               A1 a = (A1) null;
            }
        """,
        """
            import a.A2;
            
            public class B {
               A2 a = (A2) null;
            }
        """)
    )

    @Test
    fun classReference() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class A {
                Class<?> clazz = A1.class;
            }
        """,
        """
            import a.A2;
            
            public class A {
                Class<?> clazz = A2.class;
            }
        """)
    )

    @Test
    fun methodSelect() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class B {
               A1 a = null;
               public void test() { a.foo(); }
            }
        """,
        """
            import a.A2;
            
            public class B {
               A2 a = null;
               public void test() { a.foo(); }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2302")
    @Test
    fun staticImport() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import static a.A1.stat;
            
            public class B {
                public void test() {
                    stat();
                }
            }
        """,
        """
            import static a.A2.stat;
            
            public class B {
                public void test() {
                    stat();
                }
            }
        """)
    )

    @Test
    fun staticImports2() = rewriteRun(
        { spec -> spec.recipe(ChangeType("com.acme.product.RunnableFactory","com.acme.product.v2.RunnableFactory", true)) },
        java("""
            package com.acme.product;
            
            public class RunnableFactory {
                public static String getString() {
                    return "hello";
                }
            }
        """),
        java("""
            package com.acme.project.impl;
            
            import static com.acme.product.RunnableFactory.getString;
            
            public class StaticImportWorker {
                public void work() {
                    getString().toLowerCase();
                }
            }
        """,
        """
            package com.acme.project.impl;
            
            import static com.acme.product.v2.RunnableFactory.getString;
            
            public class StaticImportWorker {
                public void work() {
                    getString().toLowerCase();
                }
            }
        """)
    )

    @Test
    fun staticConstant() = rewriteRun(
        { spec -> spec.recipe(ChangeType("com.acme.product.RunnableFactory","com.acme.product.v2.RunnableFactory", true)) },
        java("""
            package com.acme.product;
            
            public class RunnableFactory {
                public static final String CONSTANT = "hello";
            }
        """),
        java("""
            package com.acme.project.impl;
            
            import static com.acme.product.RunnableFactory.CONSTANT;
            
            public class StaticImportWorker {
                public void work() {
                    System.out.println(CONSTANT + " fred.");
                }
            }
        """,
        """
            package com.acme.project.impl;
            
            import static com.acme.product.v2.RunnableFactory.CONSTANT;
            
            public class StaticImportWorker {
                public void work() {
                    System.out.println(CONSTANT + " fred.");
                }
            }
        """)
    )

    @Disabled("https://github.com/openrewrite/rewrite/issues/62")
    @Test
    fun primitiveToClass() = rewriteRun(
        { spec -> spec.recipe(ChangeType("int", "java.lang.Integer", true)) },
        java("""
            class A {
                int foo = 5;
                int getFoo() {
                    return foo;
                }
            }
        """,
        """
            class A {
                Integer foo = 5;
                Integer getFoo() {
                    return foo;
                }
            }
        """)
    )

    @Test
    fun classToPrimitive() = rewriteRun(
        { spec -> spec.recipe(ChangeType("java.lang.Integer","int", true)) },
        java("""
            class A {
                Integer foo = 5;
                Integer getFoo() {
                    return foo;
                }
            }
        """,
        """
            class A {
                int foo = 5;
                int getFoo() {
                    return foo;
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/698")
    @Test
    fun importOrdering() = rewriteRun(
        { spec -> spec.recipe(ChangeType("com.yourorg.a.A", "com.myorg.b.B", true)) },
        java("""
            package com.yourorg.a;
            public class A {}
        """),
        java("""
            package com.myorg.b;
            public class B {}
        """),
        java("""
            package com.myorg;

            import java.util.ArrayList;
            import com.yourorg.a.A;
            import java.util.List;
            
            public class Foo {
                List<A> a = new ArrayList<>();
            }
        """,
        """
            package com.myorg;

            import com.myorg.b.B;

            import java.util.ArrayList;
            import java.util.List;
            
            public class Foo {
                List<B> a = new ArrayList<>();
            }
        """)
    )

    @Test
    fun changeTypeWithInnerClass() = rewriteRun(
        { spec -> spec.recipe(ChangeType("com.acme.product.OuterClass", "com.acme.product.v2.OuterClass", true)) },
        java("""
                package com.acme.product;
                
                public class OuterClass {
                    public static class InnerClass {
                
                    }
                }
        """),
        java("""
            package de;
            
            import com.acme.product.OuterClass.InnerClass;
            import com.acme.product.OuterClass;
            
            public class UseInnerClass {
                public String work() {
                    return new InnerClass().toString();
                }
            
                public String work2() {
                    return new OuterClass().toString();
                }
            }
        """,
        """
            package de;

            import com.acme.product.v2.OuterClass.InnerClass;
            import com.acme.product.v2.OuterClass;
            
            public class UseInnerClass {
                public String work() {
                    return new InnerClass().toString();
                }
            
                public String work2() {
                    return new OuterClass().toString();
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/925")
    @Test
    fun uppercaseInPackage() = rewriteRun(
        { spec -> spec.recipe(ChangeType("com.acme.product.util.accessDecision.AccessVote", "com.acme.product.v2.util.accessDecision.AccessVote", true)) },
        java("""
                package com.acme.product.util.accessDecision;
                
                public enum AccessVote {
                    ABSTAIN
                }
        """),
        java("""
            package de;
            
            import com.acme.product.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote() {
                    return AccessVote.ABSTAIN;
                }
            }
        """,
        """
            package de;

            import com.acme.product.v2.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote() {
                    return AccessVote.ABSTAIN;
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/934")
    @Test
    fun lambda() = rewriteRun(
        { spec -> spec.recipe(ChangeType("com.acme.product.Procedure", "com.acme.product.Procedure2", true)) },
        java("""
            package com.acme.product;
            public interface Procedure {
                void execute();
            }
        """),
        java("""
            import com.acme.product.Procedure;
            
            public abstract class Worker {
                void callWorker() {
                    worker(() -> {
                    });
                }
                abstract void worker(Procedure callback);
            }
        """,
        """
            import com.acme.product.Procedure2;
            
            public abstract class Worker {
                void callWorker() {
                    worker(() -> {
                    });
                }
                abstract void worker(Procedure2 callback);
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/932")
    @Test
    fun assignment() = rewriteRun(
        { spec -> spec.recipe(ChangeType("com.acme.product.util.accessDecision.AccessVote", "com.acme.product.v2.util.accessDecision.AccessVote", true)) },
        java("""
            package com.acme.product.util.accessDecision;
            
            public enum AccessVote {
                ABSTAIN,
                GRANT
            }
        """),
        java("""
            package de;
            
            import com.acme.product.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote(Object input) {
                    AccessVote fred;
                    fred = (AccessVote) input;
                    return fred;
                }
            }
        """,
        """
            package de;

            import com.acme.product.v2.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote(Object input) {
                    AccessVote fred;
                    fred = (AccessVote) input;
                    return fred;
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/932")
    @Test
    fun ternary() = rewriteRun(
        { spec -> spec.recipe(ChangeType("com.acme.product.util.accessDecision.AccessVote", "com.acme.product.v2.util.accessDecision.AccessVote", true)) },
        java("""
            package com.acme.product.util.accessDecision;
            
            public enum AccessVote {
                ABSTAIN,
                GRANT
            }
        """),
        java("""
            package de;
            
            import com.acme.product.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote(Object input) {
                    return input == null ? AccessVote.GRANT : AccessVote.ABSTAIN;
                }
            }
        """,
        """
            package de;

            import com.acme.product.v2.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote(Object input) {
                    return input == null ? AccessVote.GRANT : AccessVote.ABSTAIN;
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/775")
    @Test
    fun changeTypeInTypeDeclaration() = rewriteRun(
        { spec -> spec.recipe(ChangeType("de.Class2", "de.Class1", false)) },
        java("""
            package de;
            public class Class2 {}
        """,
            """
            package de;
            public class Class1 {}
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1291")
    @Test
    fun doNotChangeTypeInTypeDeclaration() = rewriteRun(
        { spec -> spec.recipe(ChangeType("de.Class2", "de.Class1", true)) },
        java("""
            package de;
            public class Class2 {}
        """)
    )

    @Test
    fun javadocs() = rewriteRun(
        { spec -> spec.recipe(ChangeType("java.util.List", "java.util.Collection", true)) },
        java("""
            import java.util.List;
            
            /**
             * {@link List} here
             */
            class Test {
                int n;
            }
        """,
        """
            import java.util.Collection;
            
            /**
             * {@link Collection} here
             */
            class Test {
                int n;
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/978")
    @Test
    fun onlyUpdateApplicableImport() = rewriteRun(
        { spec -> spec.recipe(ChangeType("com.acme.product.factory.V1Factory","com.acme.product.factory.V1FactoryA", true)) },
        java("""
            package com.acme.product.factory;

            public class V1Factory {
                public static String getItem() {
                    return "V1Factory";
                }
            }
        """),
        java("""
            package com.acme.product.factory;

            public class V2Factory {
                public static String getItem() {
                    return "V2Factory";
                }
            }
        """),
        java("""
            import com.acme.product.factory.V1Factory;

            import static com.acme.product.factory.V2Factory.getItem;

            public class UseFactories {
                static class MyV1Factory extends V1Factory {
                    static String getMyItemInherited() {
                        return getItem();
                    }
                }

                static String getMyItemStaticImport() {
                    return getItem();
                }
            }
        """,
        """
            import com.acme.product.factory.V1FactoryA;

            import static com.acme.product.factory.V2Factory.getItem;

            public class UseFactories {
                static class MyV1Factory extends V1FactoryA {
                    static String getMyItemInherited() {
                        return getItem();
                    }
                }

                static String getMyItemStaticImport() {
                    return getItem();
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun filePathMatchWithNoMatchedClassFqn(@TempDir tempDir: Path) = assertUnchangedBase(
        recipe = ChangeType("a.b.Original", "x.y.Target", false),
        before = tempDir.resolve("a/b/Original.java").apply {
            toFile().parentFile.mkdirs()
            // language=java
            toFile().writeText("""
                package a;
                public class NoMatch {
                }
            """.trimIndent())
        }.toFile()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun onlyChangeTypeMissingPublicModifier(@TempDir tempDir: Path) = assertChanged(
        recipe = ChangeType("a.b.Original", "x.y.Target", false),
        before = tempDir.resolve("a/b/Original.java").apply {
            toFile().parentFile.mkdirs()
            // language=java
            toFile().writeText("""
                package a.b;
                class Original {
                }
            """.trimIndent())
        }.toFile(),
        after = """
                package x.y;
                class Target {
                }
        """.trimIndent(),
        afterConditions = {
                cu -> assertThat(TypeUtils.isOfClassType(cu.classes[0].type, "x.y.Target")).isTrue
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun onlyChangeTypeWithoutMatchedFilePath(@TempDir tempDir: Path) = assertChanged(
        recipe = ChangeType("a.b.Original", "x.y.Target", false),
        before = tempDir.resolve("a/b/NoMatch.java").apply {
            toFile().parentFile.mkdirs()
            // language=java
            toFile().writeText("""
                package a.b;
                public class Original {
                }
            """.trimIndent())
        }.toFile(),
        after = """
                package x.y;
                public class Target {
                }
        """.trimIndent(),
        afterConditions = {
                cu -> assertThat(TypeUtils.isOfClassType(cu.classes[0].type, "x.y.Target")).isTrue
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun renameClassAndFilePath(@TempDir tempDir: Path) {
        val sources = JavaParser.fromJavaVersion().build().parse(
            listOf(tempDir.resolve("a/b/Original.java").apply {
                toFile().parentFile.mkdirs()
                // language=java
                toFile().writeText("""
                    package a.b;
                    public class Original {
                    }
                """.trimIndent())
            }),
            tempDir,
            InMemoryExecutionContext()
        )

        val results = ChangeType("a.b.Original", "x.y.Target", false).run(sources).results

        // similarity index doesn't matter
        // language=diff
        assertThat(results.joinToString("") { it.diff() }).isEqualTo("""
            diff --git a/a/b/Original.java b/x/y/Target.java
            similarity index 0%
            rename from a/b/Original.java
            rename to x/y/Target.java
            index 49dd697..65e689d 100644
            --- a/a/b/Original.java
            +++ b/x/y/Target.java
            @@ -1,3 +1,3 @@ org.openrewrite.java.ChangeType
            -package a.b;
            -public class Original {
            +package x.y;
            +public class Target {
             }
            \ No newline at end of file
        """.trimIndent() + "\n")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun renamePackageAndFilePath(@TempDir tempDir: Path) {
        val sources = JavaParser.fromJavaVersion().build().parse(
            listOf(tempDir.resolve("a/b/Original.java").apply {
                toFile().parentFile.mkdirs()
                // language=java
                toFile().writeText("""
                    package a.b;
                    public class Original {
                    }
                """.trimIndent())
            }),
            tempDir,
            InMemoryExecutionContext()
        )

        val results = ChangeType("a.b.Original", "x.y.Original", false).run(sources).results

        // similarity index doesn't matter
        // language=diff
        assertThat(results.joinToString("") { it.diff() }).isEqualTo("""
            diff --git a/a/b/Original.java b/x/y/Original.java
            similarity index 0%
            rename from a/b/Original.java
            rename to x/y/Original.java
            index 49dd697..02e6a06 100644
            --- a/a/b/Original.java
            +++ b/x/y/Original.java
            @@ -1,3 +1,3 @@ org.openrewrite.java.ChangeType
            -package a.b;
            +package x.y;
             public class Original {
             }
            \ No newline at end of file
        """.trimIndent() + "\n")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun renamePackageAndInnerClassName(@TempDir tempDir: Path) {
        val sources = JavaParser.fromJavaVersion().build().parse(
            listOf(tempDir.resolve("a/b/C.java").apply {
                toFile().parentFile.mkdirs()
                // language=java
                toFile().writeText("""
                    package a.b;
                    public class C {
                        public static class Original {
                        }
                    }
                """.trimIndent())
            }),
            tempDir,
            InMemoryExecutionContext()
        )

        val results = ChangeType("a.b.C${'$'}Original", "x.y.C${'$'}Target", false).run(sources).results

        // similarity index doesn't matter
        // language=diff
        assertThat(results.joinToString("") { it.diff() }).isEqualTo("""
            diff --git a/a/b/C.java b/x/y/C.java
            similarity index 0%
            rename from a/b/C.java
            rename to x/y/C.java
            index 7b428dc..56a5c53 100644
            --- a/a/b/C.java
            +++ b/x/y/C.java
            @@ -1,5 +1,5 @@ org.openrewrite.java.ChangeType
            -package a.b;
            +package x.y;
             public class C {
            -    public static class Original {
            +    public static class Target {
                 }
             }
            \ No newline at end of file
        """.trimIndent() + "\n")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun updateImportPrefixWithEmptyPackage() = rewriteRun(
        { spec -> spec.recipe(ChangeType("a.b.Original", "Target", false)) },
        java("""
            package a.b;
            
            import java.util.List;
            
            class Original {
            }
        """,
        """
            import java.util.List;
            
            class Target {
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun updateClassPrefixWithEmptyPackage() = rewriteRun(
        { spec -> spec.recipe(ChangeType("a.b.Original", "Target", false)) },
        java("""
            package a.b;
            
            class Original {
            }
        """,
        """
            class Target {
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun renameInnerClass() = rewriteRun(
        { spec -> spec.recipe(ChangeType("a.b.C${'$'}Original", "a.b.C${'$'}Target", false)) },
        java("""
            package a.b;
            public class C {
                public static class Original {
                }
            }
        """,
        """
            package a.b;
            public class C {
                public static class Target {
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun multipleLevelsOfInnerClasses(@TempDir tempDir: Path) {
        val sources = JavaParser.fromJavaVersion().build().parse(
            listOf(tempDir.resolve("a/b/C.java").apply {
                toFile().parentFile.mkdirs()
                // language=java
                toFile().writeText("""
                    package a.b;
                    public class C {
                        public static class D {
                            public static class Original {
                            }
                        }
                    }
                """.trimIndent())
            }),
            tempDir,
            InMemoryExecutionContext()
        )

        val results = ChangeType("a.b.C${'$'}D${'$'}Original", "x.y.C${'$'}D${'$'}Target", false).run(sources).results

        // similarity index doesn't matter
        // language=diff
        assertThat(results.joinToString("") { it.diff() }).isEqualTo("""
            diff --git a/a/b/C.java b/x/y/C.java
            similarity index 0%
            rename from a/b/C.java
            rename to x/y/C.java
            index 22bd92f..816d6b1 100644
            --- a/a/b/C.java
            +++ b/x/y/C.java
            @@ -1,7 +1,7 @@ org.openrewrite.java.ChangeType
            -package a.b;
            +package x.y;
             public class C {
                 public static class D {
            -        public static class Original {
            +        public static class Target {
                     }
                 }
             }
            \ No newline at end of file
        """.trimIndent() + "\n")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2528")
    @Test
    fun changePathOfNonPublicClass(@TempDir tempDir: Path) {
        val sources = JavaParser.fromJavaVersion().build().parse(
            listOf(tempDir.resolve("a/b/C.java").apply {
                toFile().parentFile.mkdirs()
                // language=java
                toFile().writeText("""
                    package a.b;
                    class C {
                    }
                    class D {
                    }
                """.trimIndent())
            }),
            tempDir,
            InMemoryExecutionContext()
        )

        val results = ChangeType("a.b.C", "x.y.Z", false).run(sources).results

        // similarity index doesn't matter
        // language=diff
        assertThat(results.joinToString("") { it.diff() }).isEqualTo("""
            diff --git a/a/b/C.java b/x/y/Z.java
            similarity index 0%
            rename from a/b/C.java
            rename to x/y/Z.java
            index 1ef60ec..3b77cb3 100644
            --- a/a/b/C.java
            +++ b/x/y/Z.java
            @@ -1,5 +1,5 @@ org.openrewrite.java.ChangeType
            -package a.b;
            -class C {
            +package x.y;
            +class Z {
             }
             class D {
             }
            \ No newline at end of file
        """.trimIndent() + "\n")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun renamePackage() = rewriteRun(
        { spec -> spec.recipe(ChangeType("a.b.Original", "x.y.Original", false)) },
        java("""
            package a.b;
            class Original {
            }
        """,
        """
            package x.y;
            class Original {
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1904")
    @Test
    fun renameClass() = rewriteRun(
        { spec -> spec.recipe(ChangeType("a.b.Original", "a.b.Target", false)) },
        java("""
            package a.b;
            class Original {
            }
        """,
        """
            package a.b;
            class Target {
            }
        """)
    )

    @Test
    fun updateMethodType() = rewriteRun(
        { spec -> spec.recipe(ChangeType("a.A1", "a.A2", false)) },
        java("""
            package a;
            public class A1 {
            }
        """,
        """
            package a;
            public class A2 {
            }
        """),
        java("""
            package org.foo;
            
            import a.A1;
            
            public class Example {
                public A1 method(A1 a1) {
                    return a1;
                }
            }
        """,
        """
            package org.foo;
            
            import a.A2;
            
            public class Example {
                public A2 method(A2 a1) {
                    return a1;
                }
            }
        """),
        java("""
            import a.A1;
            import org.foo.Example;
            
            public class Test {
                A1 local = new Example().method(null);
            }
        """,
        """
            import a.A2;
            import org.foo.Example;
            
            public class Test {
                A2 local = new Example().method(null);
            }
        """) { spec -> spec.afterRecipe { cu ->
            val methodType = cu.typesInUse.usedMethods.filter { "a.A2" == it.returnType.asFullyQualified()!!.fullyQualifiedName }
            assertThat(methodType).hasSize(1)
            assertThat(methodType[0].returnType.asFullyQualified()!!.fullyQualifiedName).isEqualTo("a.A2")
            assertThat(methodType[0].parameterTypes[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("a.A2")
        }}
    )

    @Test
    fun updateVariableType() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class Test {
                A1 a;
            }
        """,
        """
            import a.A2;
            
            public class Test {
                A2 a;
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.typesInUse.variables.toTypedArray()[0].type.asFullyQualified()!!.fullyQualifiedName).isEqualTo("a.A2")
            assertThat(cu.findType("a.A1")).isEmpty()
            assertThat(cu.findType("a.A2")).isNotEmpty()
        }}
    )

    @Test
    fun boundedGenericType() = rewriteRun(
        java(a1),
        java(a2),
        java("""
            import a.A1;
            
            public class Test {
                <T extends A1> T method(T t) {
                    return t;
                }
            }
        """,
        """
            import a.A2;
            
            public class Test {
                <T extends A2> T method(T t) {
                    return t;
                }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("a.A1")).isEmpty()
            assertThat(cu.findType("a.A2")).isNotEmpty()
        }}
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2034")
    @Test
    fun changeConstructor() = rewriteRun(
        { spec -> spec.recipe(ChangeType("a.A1", "a.A2", false)) },
        java("""
            package a;
            
            public class A1 {
                public A1() {
                }
            }
        """,
        """
            package a;
            
            public class A2 {
                public A2() {
                }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("a.A1")).isEmpty()
            assertThat(cu.findType("a.A2")).isNotEmpty()
        }}
    )

    @Disabled("requires correct Kind.")
    @Issue("https://github.com/openrewrite/rewrite/issues/2478")
    @Test
    fun updateJavaTypeClassKindAnnotation() = rewriteRun(
        { spec -> spec.recipe(ChangeType("org.openrewrite.Test1", "org.openrewrite.Test2", false)) },
        java("""
            package org.openrewrite;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @Target({ElementType.TYPE, ElementType.METHOD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Test1 {}
        """,
            """
            package org.openrewrite;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @Target({ElementType.TYPE, ElementType.METHOD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Test2 {}
        """),
        java("""
            import org.openrewrite.Test1;
            
            public class A {
                @Test1
                void method() {}
            }
        """,
            """
            import org.openrewrite.Test2;
            
            public class A {
                @Test2
                void method() {}
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test1")).isEmpty()
            assertThat(cu.findType("org.openrewrite.Test2")).isNotEmpty()
        }}
    )

    @Disabled("requires correct Kind.")
    @Suppress("StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/2478")
    @Test
    fun changeJavaTypeClassKindEnum() = rewriteRun(
        { spec -> spec.recipe(ChangeType("org.openrewrite.MyEnum1", "org.openrewrite.MyEnum2", false)) },
        java("""
            package org.openrewrite;
            public enum MyEnum1 {
                A,
                B
            }
        ""","""
            package org.openrewrite;
            public enum MyEnum2 {
                A,
                B
            }
        """),
        java("""
            package org.openrewrite;
            import static org.openrewrite.MyEnum1.A;
            import static org.openrewrite.MyEnum1.B;
            public class App {
                public void test(String s) {
                    if (s.equals(" " + A + B)) {
                    }
                }
            }
        """,
            """
            package org.openrewrite;
            import static org.openrewrite.MyEnum2.A;
            import static org.openrewrite.MyEnum2.B;
            public class App {
                public void test(String s) {
                    if (s.equals(" " + A + B)) {
                    }
                }
            }
        """) { spec ->
            spec.afterRecipe { cu ->
                assertThat(cu.findType("org.openrewrite.MyEnum1")).isEmpty()
                assertThat(cu.findType("org.openrewrite.MyEnum2")).isNotEmpty()
            }
        }
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangeType(null, null, true)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("newFullyQualifiedTypeName")
        assertThat(valid.failures()[1].property).isEqualTo("oldFullyQualifiedTypeName")

        recipe = ChangeType(null, "java.lang.String", true)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("oldFullyQualifiedTypeName")

        recipe = ChangeType("java.lang.String", null, true)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("newFullyQualifiedTypeName")
    }
}
