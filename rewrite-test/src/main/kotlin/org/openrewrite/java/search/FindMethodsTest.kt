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
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("RedundantOperationOnEmptyContainer")
interface FindMethodsTest : JavaRecipeTest {

    @Test
    fun findMethodReferences(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindMethods("A singleArg(String)"),
        before = """
            class Test {
                void test() {
                    new java.util.ArrayList<String>().forEach(new A()::singleArg);
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    new java.util.ArrayList<String>().forEach(new A()::/*~~>*/singleArg);
                }
            }
        """,
        dependsOn = arrayOf("""
            class A {
                public void singleArg(String s) {}
            }
        """)
    )

    @Test
    fun findStaticMethodCalls(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindMethods("java.util.Collections emptyList()"),
        before = """
            import java.util.Collections;
            public class A {
               Object o = Collections.emptyList();
            }
        """,
        after = """
            import java.util.Collections;
            public class A {
               Object o = /*~~>*/Collections.emptyList();
            }
        """
    )

    @Test
    fun findStaticallyImportedMethodCalls(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindMethods("java.util.Collections emptyList()"),
        before = """
            import static java.util.Collections.emptyList;
            public class A {
               Object o = emptyList();
            }
        """,
        after = """
            import static java.util.Collections.emptyList;
            public class A {
               Object o = /*~~>*/emptyList();
            }
        """
    )

    @Test
    fun matchVarargs(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindMethods("A foo(String, Object...)"),
        before = """
            public class B {
               public void test() {
                   new A().foo("s", "a", 1);
               }
            }
        """,
        after = """
            public class B {
               public void test() {
                   /*~~>*/new A().foo("s", "a", 1);
               }
            }
        """,
        dependsOn = arrayOf("""
            public class A {
                public void foo(String s, Object... o) {}
            }
        """)
    )

    @Test
    fun matchOnInnerClass(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindMethods("B.C foo()"),
        before = """
            public class A {
               void test() {
                   new B.C().foo();
               }
            }
        """,
        after = """
            public class A {
               void test() {
                   /*~~>*/new B.C().foo();
               }
            }
        """,
        dependsOn = arrayOf("""
            public class B {
               public static class C {
                   public void foo() {}
               }
            }
        """)
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = FindMethods(null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("methodPattern")

        recipe = FindMethods("com.foo.Foo bar()")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}
