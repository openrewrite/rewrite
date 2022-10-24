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
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import java.nio.file.Paths

interface ChangePackageTest: JavaRecipeTest, RewriteTest {

    override val recipe: Recipe
        get() = ChangePackage("org.openrewrite", "org.openrewrite.test", null)

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(recipe)
    }

    companion object {
        private val testClass = """
            package org.openrewrite;
            public class Test extends Exception {
                public static void stat() {}
                public void foo() {}
            }
        """.trimIndent()
    }

    @Test
    fun renamePackageNonRecursive(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = ChangePackage(
            "org.openrewrite",
            "org.openrewrite.test",
            false
        ),
        before = """
            package org.openrewrite.internal;
            class Test {
            }
        """
    )

    @Test
    fun dontAddImportWhenNoChangesWereMade(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            public class B {}
        """
    )

    @Test
    fun renameImport(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("""
            package org.openrewrite;
            public class Test {
            }
        """),
        before = """
            import org.openrewrite.Test;
            
            class A {
            }
        """,
        after = """ 
            import org.openrewrite.test.Test;
            
            class A {
            }
        """,
        afterConditions = { cu ->
            val imported = cu.imports[0]
            assertThat(imported.packageName).isEqualTo("org.openrewrite.test")
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun typesInUseContainsOneTypeReference() = assertChanged(
        dependsOn = arrayOf(testClass),
        before = """
            import org.openrewrite.Test;
            
            public class A {
                Test a;
                Test b;
                Test c;
            }
        """,
        after = """
            import org.openrewrite.test.Test;
            
            public class A {
                Test a;
                Test b;
                Test c;
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.typesInUse.typesInUse).hasSize(1)
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun typesInUseContainsOneMethodTypeReference() = assertChanged(
        dependsOn = arrayOf(testClass),
        before = """
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
        after = """
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
        """,
        afterConditions = { cu ->
            assertThat(cu.typesInUse.usedMethods).hasSize(1)
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun updateMethodType() = assertChanged(
        dependsOn = arrayOf("""
            package org.openrewrite;
            public class Test {
            }
            """,
            """
            package org.foo;
            
            import org.openrewrite.Test;
            
            public class Example {
                public static Test method(Test test) {
                    return test;
                }
            }
        """),
        before = """
            import org.openrewrite.Test;
            import org.foo.Example;
            
            public class A {
                Test local = Example.method(null);
            }
        """,
        after = """
            import org.openrewrite.test.Test;
            import org.foo.Example;
            
            public class A {
                Test local = Example.method(null);
            }
        """,
        afterConditions = { cu ->
            val methodType = cu.typesInUse.usedMethods.toTypedArray()[0]
            assertThat(methodType.returnType.asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.test.Test")
            assertThat(methodType.parameterTypes[0].asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.test.Test")
        }
    )

    @Test
    fun updateVariableType() = assertChanged(
        dependsOn = arrayOf(testClass),
        before = """
            import org.openrewrite.Test;
            
            public class A {
                Test a;
            }
        """,
        after = """
            import org.openrewrite.test.Test;
            
            public class A {
                Test a;
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.typesInUse.variables.toTypedArray()[0].type.asFullyQualified()!!.fullyQualifiedName).isEqualTo("org.openrewrite.test.Test")
        }
    )

    @Test
    fun renamePackage(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package org.openrewrite;
            class Test {
            }
        """,
        after = """ 
            package org.openrewrite.test;
            class Test {
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun renamePackageRecursive(jp: JavaParser) = assertChanged(
        jp,
        before = """
            package org.openrewrite.internal;
            class Test {
            }
        """,
        after = """ 
            package org.openrewrite.test.internal;
            class Test {
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.sourcePath).isEqualTo(Paths.get("org/openrewrite/test/internal/Test.java"))

            assertThat(cu.findType("org.openrewrite.internal.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.internal.Test")).isNotEmpty()
        }
    )

    @Test
    fun renamePackageRecursiveImported(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangePackage("org.openrewrite", "org.openrewrite.test", true),
        dependsOn = arrayOf("""
            package org.openrewrite.other;
            public class Test {}
        """),
        before = """
            import org.openrewrite.other.Test;
            class A {
                Test test = null;
            }
        """,
        after = """ 
            import org.openrewrite.test.other.Test;
            class A {
                Test test = null;
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.other.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.other.Test")).isNotEmpty()
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun typeParameter(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("""
            package org.openrewrite;
            public class Test {
            }
        """),
        before = """
            import org.openrewrite.Test;
            import java.util.List;
            
            class A {
                List<Test> list;
            }
        """,
        after = """ 
            import org.openrewrite.test.Test;
            import java.util.List;
            
            class A {
                List<Test> list;
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun classTypeParameter(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("""
            package org.openrewrite;
            public class Test {
            }
        """),
        before = """
            import org.openrewrite.Test;
            
            class A<T extends Test> {
            }
        """,
        after = """ 
            import org.openrewrite.test.Test;
            
            class A<T extends Test> {
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun boundedGenericType() = assertChanged(
        dependsOn = arrayOf(testClass),
        before = """
            import org.openrewrite.Test;
            
            public class A {
                <T extends Test> T method(T t) {
                    return t;
                }
            }
        """,
        after = """
            import org.openrewrite.test.Test;
            
            public class A {
                <T extends Test> T method(T t) {
                    return t;
                }
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun annotation(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("""
                package org.openrewrite;
                
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;
                
                @Target({ElementType.TYPE, ElementType.METHOD})
                @Retention(RetentionPolicy.RUNTIME)
                public @interface Test {}
        """),
        before = """
            import org.openrewrite.Test;
            public class A {
                @Test
                void method() {}
            }
        """,

        after = """
            import org.openrewrite.test.Test;
            public class A {
                @Test
                void method() {}
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun array(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass),
        before = """
            public class B {
               org.openrewrite.Test[] a = new org.openrewrite.Test[0];
            }
        """,
        after = """
            public class B {
               org.openrewrite.test.Test[] a = new org.openrewrite.test.Test[0];
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun multiDimensionalArray() = assertChanged(
        dependsOn = arrayOf(testClass),
        before = """
            import org.openrewrite.Test;
            
            public class A {
                Test[][] multiDimensionalArray;
            }
        """,
        after = """
            import org.openrewrite.test.Test;
            
            public class A {
                Test[][] multiDimensionalArray;
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun classDecl(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass, "public interface I1 {}"),
        before = """
            public class B extends org.openrewrite.Test implements I1 {}
        """,
        after = """
            public class B extends org.openrewrite.test.Test implements I1 {}
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun updatesImplements(jp: JavaParser) = assertChanged(
        dependsOn = arrayOf(
            """
                package org.openrewrite;
                public interface Oi{}
            """
        ),
        before = """
            package org.openrewrite;
            
            public class Mi implements org.openrewrite.Oi {
            }
        """,
        after = """
            package org.openrewrite.test;
            
            public class Mi implements org.openrewrite.test.Oi {
            }
        """
    )

    @Test
    fun moveToSubPackageRemoveImport() = assertChanged(
        recipe = ChangePackage("com.acme.project", "com.acme.product", null),
        dependsOn = arrayOf("""
            package com.acme.product;

            public class RunnableFactory {
                public static Runnable getRunnable() {
                    return null;
                }
            }
        """),
        before = """
            package com.acme.project;

            import com.acme.product.RunnableFactory;
            
            public class StaticImportWorker {
                public void work() {
                    RunnableFactory.getRunnable().run();
                }
            }
        """,
        after = """
            package com.acme.product;
            
            public class StaticImportWorker {
                public void work() {
                    RunnableFactory.getRunnable().run();
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun moveToSubPackageDoNotRemoveImport() = assertChanged(
        recipe = ChangePackage("com.acme.project", "com.acme.product", true),
        dependsOn = arrayOf("""
            package com.acme.product;

            public class RunnableFactory {
                public static Runnable getRunnable() {
                    return null;
                }
            }
        """),
        before = """
            package com.acme.project.other;

            import com.acme.product.RunnableFactory;
            
            public class StaticImportWorker {
                public void work() {
                    RunnableFactory.getRunnable().run();
                }
            }
        """,
        after = """
            package com.acme.product.other;
            
            import com.acme.product.RunnableFactory;
            
            public class StaticImportWorker {
                public void work() {
                    RunnableFactory.getRunnable().run();
                }
            }
        """
    )

    @Test
    fun lambda(jp: JavaParser) = assertChanged(
        dependsOn = arrayOf("""
            package org.openrewrite;
            public interface Procedure {
                void execute();
            }
        """),
        before = """
            import org.openrewrite.Procedure;
            public abstract class Worker {
                void callWorker() {
                    worker(() -> {});
                }
                abstract void worker(Procedure procedure);
            }
        """,
        after = """
            import org.openrewrite.test.Procedure;
            public abstract class Worker {
                void callWorker() {
                    worker(() -> {});
                }
                abstract void worker(Procedure procedure);
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Procedure")).isEmpty()
        }
    )

    @Test
    fun method(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass),
        before = """
            public class A {
               public org.openrewrite.Test foo() { return null; }
            }
        """,
        after = """
            public class A {
               public org.openrewrite.test.Test foo() { return null; }
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun methodInvocationTypeParametersAndWildcard(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass,
        """
            package org.openrewrite;
            public class ThingOne {            
            }
        """,
        """
            package org.openrewrite;
            public class ThingTwo {
                public static ThingOne getThingOne() {
                    return new ThingOne();
                }
            }
        """),
        before = """
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
        after = """
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
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun multiCatch(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass),
        before = """
            public class B {
               public void test() {
                   try {System.out.println();}
                   catch(org.openrewrite.Test | RuntimeException ignored) {}
               }
            }
        """,
        after = """
            public class B {
               public void test() {
                   try {System.out.println();}
                   catch(org.openrewrite.test.Test | RuntimeException ignored) {}
               }
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun multiVariable(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass),
        before = """
            public class B {
               org.openrewrite.Test f1, f2;
            }
        """,
        after = """
            public class B {
               org.openrewrite.test.Test f1, f2;
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun assignment(jp: JavaParser) = assertChanged(
        dependsOn = arrayOf(testClass),
        before = """
            public class B {
               org.openrewrite.Test t;
               void method(org.openrewrite.Test param) {
                   t = param;
               }
            }
        """,
        after = """
            public class B {
               org.openrewrite.test.Test t;
               void method(org.openrewrite.test.Test param) {
                   t = param;
               }
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun newClass(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass),
        before = """
            public class B {
               org.openrewrite.Test test = new org.openrewrite.Test();
            }
        """,
        after = """
            public class B {
               org.openrewrite.test.Test test = new org.openrewrite.test.Test();
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun parameterizedType(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass),
        before = """
            public class B {
               Map<org.openrewrite.Test, org.openrewrite.Test> m;
            }
        """,
        after = """
            public class B {
               Map<org.openrewrite.test.Test, org.openrewrite.test.Test> m;
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun typeCast(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass),
        before = """
            import org.openrewrite.Test;
            
            public class A {
                Test method(Object obj) {
                    return (Test) obj;
                }
            }
        """,
        after = """
            import org.openrewrite.test.Test;
            
            public class A {
                Test method(Object obj) {
                    return (Test) obj;
                }
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1997")
    @Test
    fun classReference(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass),
        before = """
            import org.openrewrite.Test;
            public class Test {
                Class<?> clazz = Test.class;
            }
        """,
        after = """
            import org.openrewrite.test.Test;
            public class Test {
                Class<?> clazz = Test.class;
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun methodSelect(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass),
        before = """
            public class B {
               org.openrewrite.Test test = null;
               public void getFoo() { test.foo(); }
            }
        """,
        after = """
            public class B {
               org.openrewrite.test.Test test = null;
               public void getFoo() { test.foo(); }
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun staticImport(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(testClass),
        before = """
            import static org.openrewrite.Test.stat;

            public class B {
                public void test() {
                    stat();
                }
            }
        """,
        after = """
            import static org.openrewrite.test.Test.stat;

            public class B {
                public void test() {
                    stat();
                }
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.findType("org.openrewrite.Test")).isEmpty()
            assertThat(cu.findType("org.openrewrite.test.Test")).isNotEmpty()
        }
    )

    @Test
    fun changeTypeWithInnerClass(jp: JavaParser) = assertChanged(
        recipe = ChangePackage("com.acme.product", "com.acme.product.v2", null),
        dependsOn = arrayOf(
            """
                package com.acme.product;
                
                public class OuterClass {
                    public static class InnerClass {
                
                    }
                }
            """
        ),
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

    @Test
    fun updateImportPrefixWithEmptyPackage() = rewriteRun(
        { spec ->
            spec.recipe(
                ChangePackage("a.b", "", false)
            )},
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
            """
        )
    )

    @Test
    fun updateClassPrefixWithEmptyPackage() = rewriteRun(
        { spec ->
            spec.recipe(
                ChangePackage("a.b", "", false)
            )
        },
        java("""
            package a.b;
            
            class Test {
            }
            """,
            """
            class Test {
            }
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2328")
    @Test
    fun renameSingleSegmentPackage() = rewriteRun(
        {spec -> spec.recipe(ChangePackage("x", "y", false))},
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
        {spec -> spec.recipe(ChangePackage("x.y.z", "x", false))},
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
