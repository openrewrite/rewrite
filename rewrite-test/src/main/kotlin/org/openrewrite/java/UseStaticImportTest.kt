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
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType

interface UseStaticImportTest : JavaRecipeTest {
    @Test
    fun replaceWithStaticImports(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package asserts;
                
                public class Assert {
                    public static void assertTrue(boolean b) {}
                    public static void assertFalse(boolean b) {}
                    public static void assertEquals(int m, int n) {}
                }
            """
        ),
        recipe = UseStaticImport("asserts.Assert assert*(..)"),
        before = """
            package test;
            
            import asserts.Assert;
            
            class Test {
                void test() {
                    Assert.assertTrue(true);
                    Assert.assertEquals(1, 2);
                    Assert.assertFalse(false);
                }
            }
        """,
        after = """
            package test;
            
            import static asserts.Assert.*;
            
            class Test {
                void test() {
                    assertTrue(true);
                    assertEquals(1, 2);
                    assertFalse(false);
                }
            }
        """
    )

    @Test
    fun methodInvocationsHavingNullSelect(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package asserts;
                
                public class Assert {
                    public static void assertTrue(boolean b) {}
                    public static void assertEquals(int m, int n) {}
                }
                
                public class MyAssert {
                    public void assertTrue(boolean b) {Assert.assertTrue(b);}
                    public void assertEquals(int m, int n) {Assert.assertEquals(m, n);}
                }
            """
        ),
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    val cd = super.visitClassDeclaration(classDecl, p)
                    return cd.withExtends(null)
                }

                override fun visitImport(_import: J.Import, p: ExecutionContext): J.Import? {
                    return null
                }

                override fun visitMethodInvocation(
                    method: J.MethodInvocation,
                    p: ExecutionContext
                ): J.MethodInvocation {
                    val mi = super.visitMethodInvocation(method, p)
                    return mi.withDeclaringType(JavaType.ShallowClass.build("asserts.Assert"))
                }

                override fun visitCompilationUnit(cu: J.CompilationUnit, p: ExecutionContext): J.CompilationUnit {
                    doAfterVisit(UseStaticImport("asserts.Assert assert*(..)"))
                    return super.visitCompilationUnit(cu, p)
                }
            }
        },
        before = """
            package test;
            
            import asserts.MyAssert;
            
            class Test extends MyAssert {
                void test() {
                    assertTrue(true);
                    assertEquals(1, 2);
                }
            }
        """,
        after = """
            package test;
            
            import static asserts.Assert.assertEquals;
            import static asserts.Assert.assertTrue;
            
            class Test {
                void test() {
                    assertTrue(true);
                    assertEquals(1, 2);
                }
            }
        """,
        cycles = 2,
        expectedCyclesThatMakeChanges = 2
    )

    @Test
    fun junit5Assertions(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp
            .classpath(JavaParser.dependenciesFromClasspath("junit-jupiter-api"))
            .build(),
        recipe = UseStaticImport("org.junit.jupiter.api.Assertions assert*(..)"),
        before = """
            package org.openrewrite;

            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.Assertions;

            class Sample {
                @Test
                void sample() {
                    Assertions.assertEquals(42, 21*2);
                }
            }
        """,
        after = """
            package org.openrewrite;

            import org.junit.jupiter.api.Test;
            
            import static org.junit.jupiter.api.Assertions.assertEquals;

            class Sample {
                @Test
                void sample() {
                    assertEquals(42, 21*2);
                }
            }
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = UseStaticImport(null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("methodPattern")

        recipe = UseStaticImport("Foo.F foo()")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}
