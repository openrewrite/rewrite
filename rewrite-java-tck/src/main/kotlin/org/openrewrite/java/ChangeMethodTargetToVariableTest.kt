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

interface ChangeMethodTargetToVariableTest : JavaRecipeTest {

    @Test
    fun explicitStaticToVariable(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """ 
                package a;
                public class A {
                   public void foo() {}
                }
            """,
            """
                package b;
                public class B {
                   public static void foo() {}
                }
            """
        ),
        recipe = ChangeMethodTargetToVariable("b.B foo()", "a", "a.A", null),
        before = """
            import a.*;
            
            import b.B;
            public class C {
               A a;
               public void test() {
                   B.foo();
               }
            }
        """,
        after = """
            import a.*;
            public class C {
               A a;
               public void test() {
                   a.foo();
               }
            }
        """
    )

    @Test
    fun staticImportToVariable(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package a;
                public class A {
                   public void foo() {}
                }
            """,
            """
                package b;
                public class B {
                   public static void foo() {}
                }
            """
        ),
        recipe = ChangeMethodTargetToVariable("b.B foo()", "a", "a.A", null),
        before = """
            import a.*;
            import static b.B.*;
            public class C {
               A a;
               public void test() {
                   foo();
               }
            }
        """,
        after = """
            import a.*;
            public class C {
               A a;
               public void test() {
                   a.foo();
               }
            }
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangeMethodTargetToVariable(null, null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(3)
        assertThat(valid.failures()[0].property).isEqualTo("methodPattern")
        assertThat(valid.failures()[1].property).isEqualTo("variableName")
        assertThat(valid.failures()[2].property).isEqualTo("variableType")

        recipe = ChangeMethodTargetToVariable(null, null,"a.A", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("methodPattern")
        assertThat(valid.failures()[1].property).isEqualTo("variableName")

        recipe = ChangeMethodTargetToVariable(null, "a",null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("methodPattern")
        assertThat(valid.failures()[1].property).isEqualTo("variableType")

        recipe = ChangeMethodTargetToVariable("b.B foo()", null,null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("variableName")
        assertThat(valid.failures()[1].property).isEqualTo("variableType")
    }
}
