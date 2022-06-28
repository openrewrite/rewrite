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
import org.openrewrite.Issue

interface ChangeTypeTest : JavaRecipeTest {
    override val recipe: ChangeType
        get() = ChangeType("a.A1","a.A2", null)

    companion object {
        private val a1 = """
            package a;
            public class A1 extends Exception {
                public static void stat() {}
                public void foo() {}
            }
        """.trimIndent()

        private val a2 = """
            package a;
            public class A2 extends Exception {
                public static void stat() {}
                public void foo() {}
            }
        """.trimIndent()
    }

    @Test
    fun doNotAddJavaLangWrapperImports(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("java.lang.Integer","java.lang.Long", null),
        before = "public class ThinkPositive { private Integer fred = 1;}",
        after = "public class ThinkPositive { private Long fred = 1;}"
    )

    @Test
    @Suppress("deprecation")
    fun allowJavaLangSubpackages(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("java.util.logging.LoggingMXBean","java.lang.management.PlatformLoggingMXBean", null),
        before = """
            import java.util.logging.LoggingMXBean;

            class Test {
                static void method() {
                    LoggingMXBean loggingBean = null;
                }
            }
        """,
        after = """
            import java.lang.management.PlatformLoggingMXBean;

            class Test {
                static void method() {
                    PlatformLoggingMXBean loggingBean = null;
                }
            }
        """
    )

    @Suppress("InstantiationOfUtilityClass")
    @Issue("https://github.com/openrewrite/rewrite/issues/788")
    @Test
    fun unnecessaryImport(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = ChangeType("test.Outer.Inner", "java.util.ArrayList", null),
        before = """
            import test.Outer;
            
            class Test {
                private Outer p = Outer.of();
                private Outer p2 = test.Outer.of();
            }
        """,
        dependsOn = arrayOf("""
            package test;
            
            public class Outer {
                public static Outer of() {
                    return new Outer();
                }
            
                public static class Inner {
                }
            }
        """.trimIndent())
    )

    @Suppress("rawtypes")
    @Issue("https://github.com/openrewrite/rewrite/issues/868")
    @Test
    fun changeInnerClassToOuterClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("java.util.Map${'$'}Entry", "java.util.List", null),
        before = """
            import java.util.Map;
            import java.util.Map.Entry;
            
            class Test {
                Entry p;
                Map.Entry p2;
            }
        """,
        after = """
            import java.util.List;
            
            class Test {
                List p;
                List p2;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/768")
    @Test
    fun changeStaticFieldAccess(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("java.io.File", "my.pkg.List", null),
        before = """
            import java.io.File;
            
            class Test {
                String p = File.separator;
            }
        """,
        after = """
            import my.pkg.List;
            
            class Test {
                String p = List.separator;
            }
        """
    )

    @Test
    fun dontAddImportWhenNoChangesWereMade(jp: JavaParser) = assertUnchanged(
        jp,
        before = "public class B {}"
    )

    @Suppress("rawtypes")
    @Issue("https://github.com/openrewrite/rewrite/issues/774")
    @Test
    fun replaceWithNestedType(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("java.io.File", "java.util.Map${'$'}Entry", null),
        before = """
            import java.io.File;
            
            class Test {
                File p;
            }
        """,
        after = """
            import java.util.Map;
            
            class Test {
                Map.Entry p;
            }
        """
    )

    @Test
    fun simpleName(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class B extends A1 {}
        """,
        after = """
            import a.A2;
            
            public class B extends A2 {}
        """
    )

    @Test
    fun fullyQualifiedName(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = "public class B extends a.A1 {}",
        after = "public class B extends a.A2 {}"
    )

    @Test
    fun annotation(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("a.b.c.A1","a.b.d.A2", null),
        dependsOn = arrayOf(
            "package a.b.c;\npublic @interface A1 {}",
            "package a.b.d;\npublic @interface A2 {}"
        ),
        before = "@a.b.c.A1 public class B {}",
        after = "@a.b.d.A2 public class B {}"
    )

    @Test
    fun array2(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("com.acme.product.Pojo","com.acme.product.v2.Pojo", null),
        dependsOn = arrayOf("""
            package com.acme.product;
            
            public class Pojo {
            }
        """),
        before = """
            package com.acme.project.impl;
            
            import com.acme.product.Pojo;
            
            public class UsePojo2 {
                Pojo[] p;
            
                void run() {
                    p[0] = null;
                }
            }
        """,
        after = """
            package com.acme.project.impl;
            
            import com.acme.product.v2.Pojo;
            
            public class UsePojo2 {
                Pojo[] p;
            
                void run() {
                    p[0] = null;
                }
            }
        """
    )

    // array types and new arrays
    @Test
    fun array(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class B {
               A1[] a = new A1[0];
            }
        """,
        after = """
            import a.A2;
            
            public class B {
               A2[] a = new A2[0];
            }
        """
    )

    @Test
    fun classDecl(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            a1, a2,
            "public interface I1 {}",
            "public interface I2 {}"
        ),
        recipe = recipe.doNext(
            ChangeType("I1", "I2", null)
        ),
        before = """
            import a.A1;
            
            public class B extends A1 implements I1 {}
        """,
        after = """
            import a.A2;
            
            public class B extends A2 implements I2 {}
        """
    )

    @Suppress("RedundantThrows")
    @Test
    fun method(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class B {
               public A1 foo() throws A1 { return null; }
            }
        """,
        after = """
            import a.A2;
            
            public class B {
               public A2 foo() throws A2 { return null; }
            }
        """
    )

    @Test
    fun methodInvocationTypeParametersAndWildcard(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class B {
               public <T extends A1> T generic(T n, List<? super A1> in);
               public void test() {
                   A1.stat();
                   this.<A1>generic(null, null);
               }
            }
        """,
        after = """
            import a.A2;
            
            public class B {
               public <T extends A2> T generic(T n, List<? super A2> in);
               public void test() {
                   A2.stat();
                   this.<A2>generic(null, null);
               }
            }
        """
    )

    @Suppress("CatchMayIgnoreException")
    @Test
    fun multiCatch(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class B {
               public void test() {
                   try {}
                   catch(A1 | RuntimeException e) {}
               }
            }
        """,
        after = """
            import a.A2;
            
            public class B {
               public void test() {
                   try {}
                   catch(A2 | RuntimeException e) {}
               }
            }
        """
    )

    @Test
    fun multiVariable(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class B {
               A1 f1, f2;
            }
        """,
        after = """
            import a.A2;
            
            public class B {
               A2 f1, f2;
            }
        """
    )

    @Test
    fun newClass(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class B {
               A1 a = new A1();
            }
        """,
        after = """
            import a.A2;
            
            public class B {
               A2 a = new A2();
            }
        """
    )

    @Suppress("UnnecessaryLocalVariable")
    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/704")
    fun updateAssignments(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;

            class B {
                void method(A1 param) {
                    A1 a = param;
                }
            }
        """,
        after = """
            import a.A2;

            class B {
                void method(A2 param) {
                    A2 a = param;
                }
            }
        """
    )

    @Test
    fun parameterizedType(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class B {
               Map<A1, A1> m;
            }
        """,
        after = """
            import a.A2;
            
            public class B {
               Map<A2, A2> m;
            }
        """
    )

    @Suppress("RedundantCast")
    @Test
    fun typeCast(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class B {
               A1 a = (A1) null;
            }
        """,
        after = """
            import a.A2;
            
            public class B {
               A2 a = (A2) null;
            }
        """
    )

    @Test
    fun classReference(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class A {
                Class<?> clazz = A1.class;
            }
        """,
        after = """
            import a.A2;
            
            public class A {
                Class<?> clazz = A2.class;
            }
        """
    )

    @Test
    fun methodSelect(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import a.A1;
            
            public class B {
               A1 a = null;
               public void test() { a.foo(); }
            }
        """,
        after = """
            import a.A2;
            
            public class B {
               A2 a = null;
               public void test() { a.foo(); }
            }
        """
    )

    @Test
    fun staticImport(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(a1, a2),
        before = """
            import static a.A1.stat;
            
            public class B {
                public void test() {
                    stat();
                }
            }
        """,
        after = """
            import static a.A2.stat;
            
            public class B {
                public void test() {
                    stat();
                }
            }
        """
    )

    @Test
    fun staticImports2(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("com.acme.product.RunnableFactory","com.acme.product.v2.RunnableFactory", null),
        dependsOn = arrayOf("""
            package com.acme.product;
            
            public class RunnableFactory {
                public static String getString() {
                    return "hello";
                }
            }
        """),
        before = """
            package com.acme.project.impl;
            
            import static com.acme.product.RunnableFactory.getString;
            
            public class StaticImportWorker {
                public void work() {
                    getString().toLowerCase();
                }
            }
        """,
        after = """
            package com.acme.project.impl;
            
            import static com.acme.product.v2.RunnableFactory.getString;
            
            public class StaticImportWorker {
                public void work() {
                    getString().toLowerCase();
                }
            }
        """
    )

    @Test
    fun staticConstant(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("com.acme.product.RunnableFactory","com.acme.product.v2.RunnableFactory", null),
        dependsOn = arrayOf("""
            package com.acme.product;
            
            public class RunnableFactory {
                public static final String CONSTANT = "hello";
            }
        """),
        before = """
            package com.acme.project.impl;
            
            import static com.acme.product.RunnableFactory.CONSTANT;
            
            public class StaticImportWorker {
                public void work() {
                    System.out.println(CONSTANT + " fred.");
                }
            }
        """,
        after = """
            package com.acme.project.impl;
            
            import static com.acme.product.v2.RunnableFactory.CONSTANT;
            
            public class StaticImportWorker {
                public void work() {
                    System.out.println(CONSTANT + " fred.");
                }
            }
        """
    )

    @Disabled("https://github.com/openrewrite/rewrite/issues/62")
    @Test
    fun primitiveToClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("int", "java.lang.Integer", null),
        before = """
            class A {
                int foo = 5;
                int getFoo() {
                    return foo;
                }
            }
        """,
        after = """
            class A {
                Integer foo = 5;
                Integer getFoo() {
                    return foo;
                }
            }
        """
    )

    @Test
    fun classToPrimitive(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("java.lang.Integer","int", null),
        before = """
            class A {
                Integer foo = 5;
                Integer getFoo() {
                    return foo;
                }
            }
        """,
        after = """
            class A {
                int foo = 5;
                int getFoo() {
                    return foo;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/698")
    @Test
    fun importOrdering(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("com.yourorg.a.A", "com.myorg.b.B", null),
        dependsOn = arrayOf("""
            package com.yourorg.a;
            public class A {}
        """,
        """
            package com.myorg.b;
            public class B {}
        """),
        before = """
            package com.myorg;

            import java.util.ArrayList;
            import com.yourorg.a.A;
            import java.util.List;
            
            public class Foo {
                List<A> a = new ArrayList<>();
            }
        """,
        after = """
            package com.myorg;

            import com.myorg.b.B;

            import java.util.ArrayList;
            import java.util.List;
            
            public class Foo {
                List<B> a = new ArrayList<>();
            }
        """
    )

    @Test
    fun changeTypeWithInnerClass(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package com.acme.product;
                
                public class OuterClass {
                    public static class InnerClass {
                
                    }
                }
            """
        ),
        recipe = ChangeType("com.acme.product.OuterClass", "com.acme.product.v2.OuterClass", null),
        before = """
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
        after = """
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
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/925")
    @Test
    fun uppercaseInPackage(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package com.acme.product.util.accessDecision;
                
                public enum AccessVote {
                    ABSTAIN
                }
            """
        ),
        recipe = ChangeType("com.acme.product.util.accessDecision.AccessVote", "com.acme.product.v2.util.accessDecision.AccessVote", null),
        before = """
            package de;
            
            import com.acme.product.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote() {
                    return AccessVote.ABSTAIN;
                }
            }
        """,
        after = """
            package de;

            import com.acme.product.v2.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote() {
                    return AccessVote.ABSTAIN;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/934")
    @Test
    fun lambda(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package com.acme.product;
                public interface Procedure {
                    void execute();
                }
            """
        ),
        recipe = ChangeType("com.acme.product.Procedure", "com.acme.product.Procedure2", null),
        before = """
            import com.acme.product.Procedure;
            
            public abstract class Worker {
                void callWorker() {
                    worker(() -> {
                    });
                }
                abstract void worker(Procedure callback);
            }
        """,
        after = """
            import com.acme.product.Procedure2;
            
            public abstract class Worker {
                void callWorker() {
                    worker(() -> {
                    });
                }
                abstract void worker(Procedure2 callback);
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/932")
    @Test
    fun assignment(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package com.acme.product.util.accessDecision;
                
                public enum AccessVote {
                    ABSTAIN,
                    GRANT
                }
            """
        ),
        recipe = ChangeType("com.acme.product.util.accessDecision.AccessVote", "com.acme.product.v2.util.accessDecision.AccessVote", null),
        before = """
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
        after = """
            package de;

            import com.acme.product.v2.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote(Object input) {
                    AccessVote fred;
                    fred = (AccessVote) input;
                    return fred;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/932")
    @Test
    fun ternary(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package com.acme.product.util.accessDecision;
                
                public enum AccessVote {
                    ABSTAIN,
                    GRANT
                }
            """
        ),
        recipe = ChangeType("com.acme.product.util.accessDecision.AccessVote", "com.acme.product.v2.util.accessDecision.AccessVote", null),
        before = """
            package de;
            
            import com.acme.product.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote(Object input) {
                    return input == null ? AccessVote.GRANT : AccessVote.ABSTAIN;
                }
            }
        """,
        after = """
            package de;

            import com.acme.product.v2.util.accessDecision.AccessVote;

            public class ProjectVoter {
                public AccessVote vote(Object input) {
                    return input == null ? AccessVote.GRANT : AccessVote.ABSTAIN;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/775")
    @Test
    fun changeTypeInTypeDeclaration(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeType("de.Class2", "de.Class1", null),
        before = """
            package de;
            public class Class2 {}
        """,
            after = """
            package de;
            public class Class1 {}
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1291")
    @Test
    fun doNotChangeTypeInTypeDeclaration(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = ChangeType("de.Class2", "de.Class1", true),
        before = """
            package de;
            public class Class2 {}
        """
    )

    @Test
    fun javadocs() = assertChanged(
        recipe = ChangeType("java.util.List", "java.util.Collection", null),
        before = """
            import java.util.List;
            
            /**
             * {@link List} here
             */
            class Test {
                int n;
            }
        """,
        after = """
            import java.util.Collection;
            
            /**
             * {@link Collection} here
             */
            class Test {
                int n;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/978")
    @Test
    fun onlyUpdateApplicableImport(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("""
            package com.acme.product.factory;

            public class V1Factory {
                public static String getItem() {
                    return "V1Factory";
                }
            }
        """,
            """
            package com.acme.product.factory;

            public class V2Factory {
                public static String getItem() {
                    return "V2Factory";
                }
            }
        """),
        recipe = ChangeType("com.acme.product.factory.V1Factory","com.acme.product.factory.V1FactoryA", null),
        before = """
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
        after = """
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
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangeType(null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("newFullyQualifiedTypeName")
        assertThat(valid.failures()[1].property).isEqualTo("oldFullyQualifiedTypeName")

        recipe = ChangeType(null, "java.lang.String", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("oldFullyQualifiedTypeName")

        recipe = ChangeType("java.lang.String", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("newFullyQualifiedTypeName")
    }
}
