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
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface FindTypesTest : JavaRecipeTest {
    override val recipe: FindTypes
        get() = FindTypes("a.A1", false)

    companion object {
        private const val a1 = """
            package a;
            public class A1 extends Exception {
                public static void stat() {}
            }
        """
    }

    @Test
    fun simpleName(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import a.A1;
            public class B extends A1 {}
        """,
        after = """
            import a.A1;
            public class B extends /*~~>*/A1 {}
        """,
        dependsOn = arrayOf(a1)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1466")
    @Test
    fun wildcard(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindTypes("java.io..*", false),
        before = """
            import java.io.File;
            public class Test {
                File file;
            }
        """,
        after = """
            import java.io.File;
            public class Test {
                /*~~>*/File file;
            }
        """
    )

    @Test
    fun fullyQualifiedName(jp: JavaParser) = assertChanged(
        jp,
        before = "public class B extends a.A1 {}",
        after = "public class B extends /*~~>*/a.A1 {}",
        dependsOn = arrayOf(a1)
    )

    @Test
    fun annotation(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindTypes("A1", false),
        before = "@A1 public class B {}",
        after = "@/*~~>*/A1 public class B {}",
        dependsOn = arrayOf("public @interface A1 {}")
    )

    @Test
    fun array(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import a.A1;
            public class B {
               A1[] a = new A1[0];
            }
        """,
        after = """
            import a.A1;
            public class B {
               /*~~>*/A1[] a = new /*~~>*/A1[0];
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun classDecl(jp: JavaParser) = assertChanged(
        jp,
        recipe = recipe.doNext(FindTypes("I1", false)),
        before = """
            import a.A1;
            public class B extends A1 implements I1 {}
        """,
        after = """
            import a.A1;
            public class B extends /*~~>*/A1 implements /*~~>*/I1 {}
        """,
        dependsOn = arrayOf(a1, "public interface I1 {}")
    )

    @Test
    fun method(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import a.A1;
            public class B {
               public A1 foo() throws A1 { return null; }
            }
        """,
        after = """
            import a.A1;
            public class B {
               public /*~~>*/A1 foo() throws /*~~>*/A1 { return null; }
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Issue("https://github.com/openrewrite/rewrite-maven-plugin/issues/165")
    @Test
    fun methodWithParameterizedType(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindTypes("java.util.List", false),
        before = """
            import java.util.List;
            public class B {
               public List<String> foo() { return null; }
            }
        """,
        after = """
            import java.util.List;
            public class B {
               public /*~~>*/List<String> foo() { return null; }
            }
        """
    )

    @Test
    fun methodInvocationTypeParametersAndWildcard(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import a.A1;
            import java.util.List;
            public class B {
               public <T extends A1> T generic(T n, List<? super A1> in) { return null; }
               public void test() {
                   A1.stat();
                   this.<A1>generic(null, null);
               }
            }
        """,
        after = """
            import a.A1;
            import java.util.List;
            public class B {
               public <T extends /*~~>*/A1> T generic(T n, List<? super /*~~>*/A1> in) { return null; }
               public void test() {
                   /*~~>*/A1.stat();
                   this.</*~~>*/A1>generic(null, null);
               }
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun multiCatch(jp: JavaParser) = assertChanged(
        jp,
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
            import a.A1;
            public class B {
               public void test() {
                   try {}
                   catch(/*~~>*/A1 | RuntimeException e) {}
               }
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun multiVariable(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import a.A1;
            public class B {
               A1 f1, f2;
            }
        """,
        after = """
            import a.A1;
            public class B {
               /*~~>*/A1 f1, f2;
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun newClass(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import a.A1;
            public class B {
               A1 a = new A1();
            }
        """,
        after = """
            import a.A1;
            public class B {
               /*~~>*/A1 a = new /*~~>*/A1();
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun parameterizedType(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import a.A1;
            import java.util.Map;
            public class B {
               Map<A1, A1> m;
            }
        """,
        after = """
            import a.A1;
            import java.util.Map;
            public class B {
               Map</*~~>*/A1, /*~~>*/A1> m;
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun assignableTypes(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindTypes("java.util.Collection", true),
        before = """
            import java.util.List;
            public class B {
               List<String> l;
            }
        """,
        after = """
            import java.util.List;
            public class B {
               /*~~>*/List<String> l;
            }
        """
    )

    @Test
    fun typeCast(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import a.A1;
            public class B {
               A1 a = (A1) null;
            }
        """,
        after = """
            import a.A1;
            public class B {
               /*~~>*/A1 a = (/*~~>*/A1) null;
            }
        """,
        dependsOn = arrayOf(a1)
    )

    @Test
    fun classReference(jp: JavaParser) = assertChanged(
            jp,
            before = """
                import a.A1;
                class B {
                    Class<?> clazz = A1.class;
                }
            """,
            after = """
                import a.A1;
                class B {
                    Class<?> clazz = /*~~>*/A1.class;
                }
            """,
            dependsOn = arrayOf(a1)
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = FindTypes(null, false)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("fullyQualifiedTypeName")

        recipe = FindTypes("com.foo.Foo", false)
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }
}
