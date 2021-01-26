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
import org.openrewrite.RecipeTest

interface ChangeMethodTargetToStaticTest : RecipeTest {

    @Test
    fun targetToStatic(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                    package a;
                    public class A {
                       public void nonStatic() {}
                    }
                """,
            """
                    package b;
                    public class B {
                       public static void foo() {}
                    }
                """
        ),
        recipe = ChangeMethodTargetToStatic("a.A nonStatic()", "b.B")
            .doNext(ChangeMethodName("b.B nonStatic()", "foo")),
        before = """
            import a.*;
            class C {
               public void test() {
                   new A().nonStatic();
               }
            }
        """,
        after = """
            import b.B;
            
            class C {
               public void test() {
                   B.foo();
               }
            }
        """
    )

    @Test
    fun staticTargetToStatic(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package b;
                public class B {
                   public static void foo() {}
                }
            """,
            """
                package a;
                public class A {
                   public static void foo() {}
                }
            """
        ),
        recipe = ChangeMethodTargetToStatic("a.A foo()","b.B"),
        before = """
            import static a.A.*;
            class C {
               public void test() {
                   foo();
               }
            }
        """,
        after = """
            import b.B;
            
            class C {
               public void test() {
                   B.foo();
               }
            }
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangeMethodTargetToStatic(null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("fullyQualifiedTargetTypeName")
        assertThat(valid.failures()[1].property).isEqualTo("methodPattern")

        recipe = ChangeMethodTargetToStatic(null, "java.lang.String")
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("methodPattern")

        recipe = ChangeMethodTargetToStatic("java.lang.String emptyString(..)", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("fullyQualifiedTargetTypeName")
    }

}
