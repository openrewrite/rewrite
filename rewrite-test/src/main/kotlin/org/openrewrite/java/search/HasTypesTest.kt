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
package org.openrewrite.java.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaRecipeTest

interface HasTypesTest : JavaRecipeTest {
    override val recipe: HasTypes
        get() = HasTypes(listOf("a.A1","a.b.*"))

    companion object {
        private const val a1 = """
            package a;
            public class A1 extends Exception {
                public static void stat() {}
            }
        """
        private const val b1 = """
            package a.b;
            public class B1 {
            }
        """
    }

    @Test
    fun simpleName() = assertChanged(
        before = """
            import a.A1;
            public class B extends A1 {}
        """,
        after = """
            /*~~>*/import a.A1;
            public class B extends A1 {}
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun simpleNameWildCard() = assertChanged(
        before = """
            import a.b.B1;
            public class B extends B1 {}
        """,
        after = """
            /*~~>*/import a.b.B1;
            public class B extends B1 {}
        """,
        dependsOn = arrayOf(a1,b1)
    )

    @Test
    fun fullyQualifiedName() = assertChanged(
        before = "public class B extends a.A1 {}",
        after = "/*~~>*/public class B extends a.A1 {}",
        dependsOn = arrayOf(a1)
    )

    @Test
    fun annotation() = assertChanged(
        recipe = HasTypes(listOf("com.foo.A2")),
        before = "@com.foo.A2 public class B {}",
        after = "/*~~>*/@com.foo.A2 public class B {}",
        dependsOn = arrayOf("package com.foo; public @interface A2 {}")
    )

    @Test
    fun array() = assertChanged(
        before = """
            import a.A1;
            public class B {
               A1[] a = new A1[0];
            }
        """,
        after = """
            /*~~>*/import a.A1;
            public class B {
               A1[] a = new A1[0];
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun classDecl() = assertChanged(
        before = """
            import a.A1;
            public class B extends A1 implements I1 {}
        """,
        after = """
            /*~~>*/import a.A1;
            public class B extends A1 implements I1 {}
        """,
        dependsOn = arrayOf(a1, "public interface I1 {}")
    )

    @Test
    fun method() = assertChanged(
        before = """
            import a.A1;
            public class B {
               public void foo() throws A1 { 
                 try {return null;} 
                 catch (Exception ex) {throw new A1();}
               }
            }
        """,
        after = """
            /*~~>*/import a.A1;
            public class B {
               public void foo() throws A1 { 
                 try {return null;} 
                 catch (Exception ex) {throw new A1();}
               }
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun methodInvocationTypeParametersAndWildcard() = assertChanged(
        before = """
            import a.A1;
            import java.util.List;
            public class B {
               public <T extends A1> T generic(T n, List<? super A1> in) { return null; }
            }
        """,
        after = """
            /*~~>*/import a.A1;
            import java.util.List;
            public class B {
               public <T extends A1> T generic(T n, List<? super A1> in) { return null; }
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun multiCatch() = assertChanged(
        before = """
            import a.A1;
            public class B {
               public void test() {
                   try {return null;}
                   catch(A1 | RuntimeException e) {throw new RuntimeException(e);}
               }
            }
        """,
        after = """
            /*~~>*/import a.A1;
            public class B {
               public void test() {
                   try {return null;}
                   catch(A1 | RuntimeException e) {throw new RuntimeException(e);}
               }
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun multiVariable() = assertChanged(
        before = """
            import a.A1;
            public class B {
               A1 f1, f2;
            }
        """,
        after = """
            /*~~>*/import a.A1;
            public class B {
               A1 f1, f2;
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun newClass() = assertChanged(
        before = """
            import a.A1;
            public class B {
               A1 a = new A1();
            }
        """,
        after = """
            /*~~>*/import a.A1;
            public class B {
               A1 a = new A1();
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun parameterizedType() = assertChanged(
        before = """
            import a.A1;
            import java.util.Map;
            public class B {
               Map<A1, A1> m;
            }
        """,
        after = """
            /*~~>*/import a.A1;
            import java.util.Map;
            public class B {
               Map<A1, A1> m;
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun typeCast() = assertChanged(
        before = """
            import a.A1;
            public class B {
               A1 a = (A1) new Exception();
            }
        """,
        after = """
            /*~~>*/import a.A1;
            public class B {
               A1 a = (A1) new Exception();
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun classReference() = assertChanged(
        before = """
                class B {
                    Class<?> clazz = a.A1.class;
                }
            """,
        after = """
                /*~~>*/class B {
                    Class<?> clazz = a.A1.class;
                }
            """,
        dependsOn = arrayOf(a1)
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = HasTypes(null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("fullyQualifiedTypeNames")

        recipe = HasTypes(listOf("com.foo.Foo"))
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }
}
