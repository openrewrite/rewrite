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
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import java.nio.file.Paths

interface ChangePackageTest: RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(ChangePackage("org.openrewrite", "org.openrewrite.test", null))
    }

    companion object {
        private val testClassBefore = """
            package org.openrewrite;
            public class Test extends Exception {
                public static void stat() {}
                public void foo() {}
            }
        """.trimIndent()
        private val testClassAfter = """
            package org.openrewrite.test;
            public class Test extends Exception {
                public static void stat() {}
                public void foo() {}
            }
        """.trimIndent()
    }

    @Test
    fun renamePackageNonRecursive() = rewriteRun(
        { spec -> spec.recipe(
            ChangePackage(
            "org.openrewrite",
            "org.openrewrite.test",
            false
        ))},
        java("""
            package org.openrewrite.internal;
            class Test {
            }
        """)
    )

    @Test
    fun dontAddImportWhenNoChangesWereMade() = rewriteRun(
        java("""
            public class B {}
        """)
    )

    @Test
    fun renameImport() = rewriteRun(
        java("""
            package org.openrewrite;
            public class Test {
            }
        """,
        """
            package org.openrewrite.test;
            public class Test {
            }
        """),
        java("""
            import org.openrewrite.Test;
            
            class A {
            }
        """,
            """
            import org.openrewrite.test.Test;
            
            class A {
            }
        """) { spec -> spec.afterRecipe { cu ->
            val imported = cu.imports[0]
            assertThat(imported.packageName).isEqualTo("org.openrewrite.test")
        }}
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun typesInUseContainsOneTypeReference() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            import org.openrewrite.Test;
            
            public class A {
                Test a;
                Test b;
                Test c;
            }
        """,
        """
            import org.openrewrite.test.Test;
            
            public class A {
                Test a;
                Test b;
                Test c;
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.typesInUse.typesInUse).hasSize(1)
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun typesInUseContainsOneMethodTypeReference() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            import org.openrewrite.Test;
            
            public class A {
                void method() {
                    Test a = test(null);
                    Test b = test(null);
                    Test c = test(null);
                }
                
                Test test(Test test) {
                    return test;
                }
            }
        """,
        """
            import org.openrewrite.test.Test;
            
            public class A {
                void method() {
                    Test a = test(null);
                    Test b = test(null);
                    Test c = test(null);
                }
                
                Test test(Test test) {
                    return test;
                }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.typesInUse.usedMethods).hasSize(1)
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun updateMethodType() = rewriteRun(
        java("""
            package org.openrewrite;
            public class Test {
            }
        """,
        """
            package org.openrewrite.test;
            public class Test {
            }
        """),
        java("""
            package org.foo;
            
            import org.openrewrite.Test;
            
            public class Example {
                public static Test method(Test test) {
                    return test;
                }
            }
        """,
        """
            package org.foo;
            
            import org.openrewrite.test.Test;
            
            public class Example {
                public static Test method(Test test) {
                    return test;
                }
            }
        """),
        java("""
            import org.openrewrite.Test;
            import org.foo.Example;
            
            public class A {
                Test local = Example.method(null);
            }
        """,
        """
            import org.openrewrite.test.Test;
            import org.foo.Example;
            
            public class A {
                Test local = Example.method(null);
            }
        """) { spec -> spec.afterRecipe { cu ->
            val methodType = cu.typesInUse.usedMethods.toTypedArray()[0]
            assertThat(methodType.returnType.asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.test.Test")
            assertThat(methodType.parameterTypes[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.test.Test")
        }}
    )

    @Test
    fun updateVariableType() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            import org.openrewrite.Test;
            
            public class A {
                Test a;
            }
        """,
        """
            import org.openrewrite.test.Test;
            
            public class A {
                Test a;
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.typesInUse.variables.toTypedArray()[0].type.asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.test.Test")
        }}
    )

    @Test
    fun renamePackage() = rewriteRun(
        java("""
            package org.openrewrite;
            class Test {
            }
        """,
        """
            package org.openrewrite.test;
            class Test {
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun renamePackageRecursive() = rewriteRun(
        java("""
            package org.openrewrite.internal;
            class Test {
            }
        """,
        """
            package org.openrewrite.test.internal;
            class Test {
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.sourcePath).isEqualTo(Paths.get("org/openrewrite/test/internal/Test.java"))

            assertThat(cu.findType("org.openrewrite.internal.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.internal.Test")).isNotEmpty()
        }}
    )

    @Test
    fun renamePackageRecursiveImported() = rewriteRun(
        { spec -> spec.recipe(
            ChangePackage(
                "org.openrewrite",
                "org.openrewrite.test",
                true
        ))},
        java("""
            package org.openrewrite.other;
            public class Test {}
        """,
        """
            package org.openrewrite.test.other;
            public class Test {}
        """),
        java("""
            import org.openrewrite.other.Test;
            class A {
                Test test = null;
            }
        """,
        """
            import org.openrewrite.test.other.Test;
            class A {
                Test test = null;
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.other.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.other.Test")).isNotEmpty()
        }}
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun typeParameter() = rewriteRun(
        java("""
            package org.openrewrite;
            public class Test {
            }
        """,
        """
            package org.openrewrite.test;
            public class Test {
            }
        """),
        java("""
            import org.openrewrite.Test;
            import java.util.List;
            
            class A {
                List<Test> list;
            }
        """,
        """
            import org.openrewrite.test.Test;
            import java.util.List;
            
            class A {
                List<Test> list;
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun classTypeParameter() = rewriteRun(
        java("""
            package org.openrewrite;
            public class Test {
            }
        """,
        """
            package org.openrewrite.test;
            public class Test {
            }
        """),
        java("""
            import org.openrewrite.Test;
            
            class A<T extends Test> {
            }
        """,
        """
            import org.openrewrite.test.Test;
            
            class A<T extends Test> {
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun boundedGenericType() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            import org.openrewrite.Test;
            
            public class A {
                <T extends Test> T method(T t) {
                    return t;
                }
            }
        """,
        """
            import org.openrewrite.test.Test;
            
            public class A {
                <T extends Test> T method(T t) {
                    return t;
                }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun annotation() = rewriteRun(
        java("""
            package org.openrewrite;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @Target({ElementType.TYPE, ElementType.METHOD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Test {}
        """,
        """
            package org.openrewrite.test;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @Target({ElementType.TYPE, ElementType.METHOD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Test {}
        """),
        java("""
            import org.openrewrite.Test;
            public class A {
                @Test
                void method() {}
            }
        """,
        """
            import org.openrewrite.test.Test;
            public class A {
                @Test
                void method() {}
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun array() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            public class B {
               org.openrewrite.Test[] a = new org.openrewrite.Test[0];
            }
        """,
        """
            public class B {
               org.openrewrite.test.Test[] a = new org.openrewrite.test.Test[0];
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun multiDimensionalArray() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            import org.openrewrite.Test;
            
            public class A {
                Test[][] multiDimensionalArray;
            }
        """,
        """
            import org.openrewrite.test.Test;
            
            public class A {
                Test[][] multiDimensionalArray;
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun classDecl() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("public interface I1 {}"),
        java("""
            public class B extends org.openrewrite.Test implements I1 {}
        """,
        """
            public class B extends org.openrewrite.test.Test implements I1 {}
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun updatesImplements() = rewriteRun(
        java("""
            package org.openrewrite;
            public interface Oi{}
        """,
        """
            package org.openrewrite.test;
            public interface Oi{}
        """),
        java("""
            package org.openrewrite;
            
            public class Mi implements org.openrewrite.Oi {
            }
        """,
        """
            package org.openrewrite.test;
            
            public class Mi implements org.openrewrite.test.Oi {
            }
        """)
    )

    @Test
    fun moveToSubPackageRemoveImport() = rewriteRun(
        { spec -> spec.recipe(
            ChangePackage(
                "com.acme.project",
                "com.acme.product",
                null
        ))},
        java("""
            package com.acme.product;

            public class RunnableFactory {
                public static Runnable getRunnable() {
                    return null;
                }
            }
        """),
        java("""
            package com.acme.project;

            import com.acme.product.RunnableFactory;
            
            public class StaticImportWorker {
                public void work() {
                    RunnableFactory.getRunnable().run();
                }
            }
        """,
        """
            package com.acme.product;
            
            public class StaticImportWorker {
                public void work() {
                    RunnableFactory.getRunnable().run();
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun moveToSubPackageDoNotRemoveImport() = rewriteRun(
        { spec -> spec.recipe(
            ChangePackage(
                "com.acme.project",
                "com.acme.product",
                true
        ))},
        java("""
            package com.acme.product;

            public class RunnableFactory {
                public static Runnable getRunnable() {
                    return null;
                }
            }
        """),
        java("""
            package com.acme.project.other;

            import com.acme.product.RunnableFactory;
            
            public class StaticImportWorker {
                public void work() {
                    RunnableFactory.getRunnable().run();
                }
            }
        """,
        """
            package com.acme.product.other;
            
            import com.acme.product.RunnableFactory;
            
            public class StaticImportWorker {
                public void work() {
                    RunnableFactory.getRunnable().run();
                }
            }
        """)
    )

    @Test
    fun lambda() = rewriteRun(
        java("""
            package org.openrewrite;
            public interface Procedure {
                void execute();
            }
        """,
        """
            package org.openrewrite.test;
            public interface Procedure {
                void execute();
            }
        """),
        java("""
            import org.openrewrite.Procedure;
            public abstract class Worker {
                void callWorker() {
                    worker(() -> {});
                }
                abstract void worker(Procedure procedure);
            }
        """,
        """
            import org.openrewrite.test.Procedure;
            public abstract class Worker {
                void callWorker() {
                    worker(() -> {});
                }
                abstract void worker(Procedure procedure);
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Procedure")).isEmpty()
        }}
    )

    @Test
    fun method() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            public class A {
               public org.openrewrite.Test foo() { return null; }
            }
        """,
        """
            public class A {
               public org.openrewrite.test.Test foo() { return null; }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun methodInvocationTypeParametersAndWildcard() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            package org.openrewrite;
            public class ThingOne {            
            }
        """,
        """
            package org.openrewrite.test;
            public class ThingOne {            
            }
        """),
        java("""
            package org.openrewrite;
            public class ThingTwo {
                public static ThingOne getThingOne() {
                    return new ThingOne();
                }
            }
        """,
        """
            package org.openrewrite.test;
            public class ThingTwo {
                public static ThingOne getThingOne() {
                    return new ThingOne();
                }
            }
        """),
        java("""
            import java.util.List;
            import org.openrewrite.ThingOne;
            import org.openrewrite.ThingTwo;
            public class B {
                public <T extends org.openrewrite.Test> T generic(T n, List<? super org.openrewrite.Test> in) {
                    return null;
                }
                public void test() {
                    org.openrewrite.Test.stat();
                    this.<org.openrewrite.Test>generic(null, null);
                    ThingOne t1 = ThingTwo.getThingOne();
                }
            }
        """,
        """
            import java.util.List;
            import org.openrewrite.test.ThingOne;
            import org.openrewrite.test.ThingTwo;
            public class B {
                public <T extends org.openrewrite.test.Test> T generic(T n, List<? super org.openrewrite.test.Test> in) {
                    return null;
                }
                public void test() {
                    org.openrewrite.test.Test.stat();
                    this.<org.openrewrite.test.Test>generic(null, null);
                    ThingOne t1 = ThingTwo.getThingOne();
                }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun multiCatch() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            public class B {
               public void test() {
                   try {System.out.println();}
                   catch(org.openrewrite.Test | RuntimeException ignored) {}
               }
            }
        """,
        """
            public class B {
               public void test() {
                   try {System.out.println();}
                   catch(org.openrewrite.test.Test | RuntimeException ignored) {}
               }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun multiVariable() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            public class B {
               org.openrewrite.Test f1, f2;
            }
        """,
        """
            public class B {
               org.openrewrite.test.Test f1, f2;
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun assignment() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            public class B {
               org.openrewrite.Test t;
               void method(org.openrewrite.Test param) {
                   t = param;
               }
            }
        """,
        """
            public class B {
               org.openrewrite.test.Test t;
               void method(org.openrewrite.test.Test param) {
                   t = param;
               }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun newClass() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            public class B {
               org.openrewrite.Test test = new org.openrewrite.Test();
            }
        """,
        """
            public class B {
               org.openrewrite.test.Test test = new org.openrewrite.test.Test();
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun parameterizedType() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            import org.openrewrite.Test;
            import java.util.Map;
            public class B {
               Map<Test, Test> m;
            }
        """,
        """
            import org.openrewrite.test.Test;
            import java.util.Map;
            public class B {
               Map<Test, Test> m;
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun typeCast() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            import org.openrewrite.Test;
            
            public class A {
                Test method(Object obj) {
                    return (Test) obj;
                }
            }
        """,
        """
            import org.openrewrite.test.Test;
            
            public class A {
                Test method(Object obj) {
                    return (Test) obj;
                }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun classReference() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            import org.openrewrite.Test;
            public class Test {
                Class<?> clazz = Test.class;
            }
        """,
        """
            import org.openrewrite.test.Test;
            public class Test {
                Class<?> clazz = Test.class;
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun methodSelect() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            public class B {
               org.openrewrite.Test test = null;
               public void getFoo() { test.foo(); }
            }
        """,
        """
            public class B {
               org.openrewrite.test.Test test = null;
               public void getFoo() { test.foo(); }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun staticImport() = rewriteRun(
        java(testClassBefore, testClassAfter),
        java("""
            import static org.openrewrite.Test.stat;

            public class B {
                public void test() {
                    stat();
                }
            }
        """,
        """
            import static org.openrewrite.test.Test.stat;

            public class B {
                public void test() {
                    stat();
                }
            }
        """) { spec -> spec.afterRecipe { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }}
    )

    @Test
    fun changeTypeWithInnerClass() = rewriteRun(
        { spec -> spec.recipe(
            ChangePackage(
                "com.acme.product",
                "com.acme.product.v2",
                null
        ))},
        java("""
            package com.acme.product;
            
            public class OuterClass {
                public static class InnerClass {
            
                }
            }
        """,
        """
            package com.acme.product.v2;
            
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

    @Test
    fun updateImportPrefixWithEmptyPackage() = rewriteRun(
        { spec -> spec.recipe(
            ChangePackage(
                "a.b",
                "",
                false
        ))},
        java("""
            package a.b;
            
            import java.util.List;
            
            class Test {
            }
        """,
        """
            import java.util.List;
            
            class Test {
            }
        """)
    )

    @Test
    fun updateClassPrefixWithEmptyPackage() = rewriteRun(
        { spec -> spec.recipe(
            ChangePackage(
                "a.b",
                "",
                false
        ))},
        java("""
            package a.b;
            
            class Test {
            }
        """,
            """
            class Test {
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2328")
    @Test
    fun renameSingleSegmentPackage() = rewriteRun(
        { spec -> spec.recipe(
            ChangePackage(
                "x",
                "y",
                false
        ))},
        java("""
            package x;
            
            class A {
            }
        """,
            """
            package y;
            
            class A {
            }
        """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2328")
    @Test
    fun removePackage() = rewriteRun(
        { spec -> spec.recipe(
            ChangePackage(
                "x.y.z",
                "x",
                false
        ))},
        java(
        """
            package x.y.z;
            
            class A {
            }
        ""","""
            package x;
            
            class A {
            }
        """)
    )

    @Disabled("Requires investigation.")
    @Suppress("StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/2439")
    @Test
    fun staticImportEnumSamePackage() = rewriteRun(
        java("""
            package org.openrewrite;
            public enum MyEnum {
                A,
                B
            }
        ""","""
            package org.openrewrite.test;
            public enum MyEnum {
                A,
                B
            }
        """),
        java("""
            package org.openrewrite;
            import static org.openrewrite.MyEnum.A;
            import static org.openrewrite.MyEnum.B;
            public class App {
                public void test(String s) {
                    if (s.equals(" " + A + B)) {
                    }
                }
            }
        """,
            """
            package org.openrewrite.test;
            import static org.openrewrite.test.MyEnum.A;
            import static org.openrewrite.test.MyEnum.B;
            public class App {
                public void test(String s) {
                    if (s.equals(" " + A + B)) {
                    }
                }
            }
        """) { spec ->
            spec.afterRecipe { cu ->
                assertThat(cu.findType("org.openrewrite.MyEnum")).isEmpty()
                assertThat(cu.findType("org.openrewrite.test.MyEnum")).isNotEmpty()
                assertThat(cu.findType("org.openrewrite.App")).isEmpty()
                assertThat(cu.findType("org.openrewrite.test.App")).isNotEmpty()
            }
        }
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangePackage(null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("newPackageName")
        assertThat(valid.failures()[1].property).isEqualTo("oldPackageName")

        recipe = ChangePackage(null, "java.lang.String", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("oldPackageName")

        recipe = ChangePackage("java.lang.String", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("newPackageName")
    }
}
