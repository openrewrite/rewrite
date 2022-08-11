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
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType

interface ReorderMethodArgumentsTest : JavaRecipeTest {

    @Test
    fun reorderArguments(jp: JavaParser) = assertChanged(
        jp,
        recipe = ReorderMethodArguments("a.A foo(String, Integer, Integer)", arrayOf("n", "m", "s"), null, null)
            .doNext(
                object : TestRecipe() {
                    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                        return object : JavaVisitor<ExecutionContext>() {
                            override fun visitLiteral(literal: J.Literal, p: ExecutionContext): J {
                                if (literal.type == JavaType.Primitive.String) {
                                    doAfterVisit(ChangeLiteral(literal) { "anotherstring" })
                                }
                                return super.visitLiteral(literal, p)
                            }
                        }
                    }
                }
            ),
        dependsOn = arrayOf(
            """
                package a;
                public class A {
                   public void foo(String s, Integer m, Integer n) {}
                   public void foo(Integer n, Integer m, String s) {}
                }
            """
        ),
        before = """
            import a.*;
            public class B {
               A a;
               public void test() {
                   a.foo(
                       "mystring",
                       1,
                       2
                   );
               }
            }
        """,
        after = """
            import a.*;
            public class B {
               A a;
               public void test() {
                   a.foo(
                       2,
                       1,
                       "anotherstring"
                   );
               }
            }
        """,
        cycles = 1,
        expectedCyclesThatMakeChanges = 1
    )

    @Test
    fun reorderArgumentsWithNoSourceAttachment(jp: JavaParser) = assertChanged(
        jp,
        recipe = ReorderMethodArguments("a.A foo(..)", arrayOf("s", "n"), arrayOf("n", "s"), null),
        dependsOn = arrayOf(
            """
                package a;
                public class A {
                   public void foo(String arg0, Integer... arg1) {}
                   public void foo(Integer arg0, Integer arg1, String arg2) {}
                }
            """
        ),
        before = """
            import a.*;
            public class B {
               A a;
               public void test() {
                   a.foo("s", 0, 1);
               }
            }
        """,
        after = """
            import a.*;
            public class B {
               A a;
               public void test() {
                   a.foo(0, 1, "s");
               }
            }
        """,
        cycles = 1,
        expectedCyclesThatMakeChanges = 1
    )

    @Test
    fun reorderArgumentsWhereOneOfTheOriginalArgumentsIsVararg(jp: JavaParser) = assertChanged(
        jp,
        recipe = ReorderMethodArguments("a.A foo(..)", arrayOf("s", "o", "n"), null, null),
        dependsOn = arrayOf(
            """
                package a;
                public class A {
                   public void foo(String s, Integer n, Object... o) {}
                   public void bar(String s, Object... o) {}
                }
            """
        ),
        before = """
            import a.*;
            public class B {
               A a;
               public void test() {
                   a.foo("mystring", 0, "a", "b");
               }
            }
        """,
        after = """
            import a.*;
            public class B {
               A a;
               public void test() {
                   a.foo("mystring", "a", "b", 0);
               }
            }
        """,
        cycles = 1,
        expectedCyclesThatMakeChanges = 1
    )

    @Test
    fun reorderArgumentsWhereTheLastArgumentIsVarargAndNotPresentInInvocation(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = ReorderMethodArguments("a.A foo(..)", arrayOf("o", "s"), null, null),
        dependsOn = arrayOf(
            """
                package a;
                public class A {
                   public void foo(String s, Object... o) {}
                }
            """
        ),
        before = """
            import a.*;
            public class B {
               public void test() {
                   new A().foo("mystring");
               }
            }
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var cm = ReorderMethodArguments(null, null, null, null)
        var valid = cm.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("methodPattern")
        assertThat(valid.failures()[1].property).isEqualTo("newParameterNames")

        cm = ReorderMethodArguments(null, null, arrayOf("a"), null)
        valid = cm.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("methodPattern")
        assertThat(valid.failures()[1].property).isEqualTo("newParameterNames")

        cm = ReorderMethodArguments(null, arrayOf("a"), null, null)
        valid = cm.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("methodPattern")

        cm = ReorderMethodArguments("b.B foo()", null, null, null)
        valid = cm.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("newParameterNames")
    }
}
