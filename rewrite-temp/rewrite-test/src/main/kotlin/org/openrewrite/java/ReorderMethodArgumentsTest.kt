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

import org.junit.jupiter.api.Test
import org.openrewrite.RecipeTest

interface ReorderMethodArgumentsTest : RecipeTest {

    @Test
    fun refactorReorderArguments(jp: JavaParser) = assertChanged(
            jp,
            dependsOn = arrayOf(
                """
                    package a;
                    public class A {
                       public void foo(String s, Integer m, Integer n) {}
                       public void foo(Integer n, Integer m, String s) {}
                    }
                """
            ),
            visitorsMappedToMany = listOf { cu ->
                val foos = cu.findMethodCalls("a.A foo(..)")
                listOf(
                        ChangeLiteral.Scoped(foos[0].args.args.first()) { "anotherstring" },
                        ReorderMethodArguments.Scoped(foos[0], arrayOf("n", "m", "s"), arrayOf())
                )
            },
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
            """
    )

    @Test
    fun refactorReorderArgumentsWithNoSourceAttachment(jp: JavaParser) = assertChanged(
            jp,
            dependsOn = arrayOf(
                """
                    package a;
                    public class A {
                       public void foo(String arg0, Integer... arg1) {}
                       public void foo(Integer arg0, Integer arg1, String arg2) {}
                    }
                """
            ),
            visitors = listOf(
                ReorderMethodArguments().apply {
                    setMethod("a.A foo(..)")
                    setOrder("n", "s")
                    setOriginalOrder("s", "n")
                }
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
            """
    )

    @Test
    fun refactorReorderArgumentsWhereOneOfTheOriginalArgumentsIsVararg(jp: JavaParser) = assertChanged(
            jp,
            dependsOn = arrayOf(
                """
                    package a;
                    public class A {
                       public void foo(String s, Integer n, Object... o) {}
                       public void bar(String s, Object... o) {}
                    }
                """
            ),
            visitors = listOf(
                ReorderMethodArguments().apply {
                    setMethod("a.A foo(..)")
                    setOrder("s", "o", "n")
                }
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
            """
    )

    @Test
    fun refactorReorderArgumentsWhereTheLastArgumentIsVarargAndNotPresentInInvocation(jp: JavaParser) = assertUnchanged(
            jp,
            dependsOn = arrayOf(
                """
                    package a;
                    public class A {
                       public void foo(String s, Object... o) {}
                    }
                """
            ),
            visitors = listOf(
                ReorderMethodArguments().apply {
                    setMethod("a.A foo(..)")
                    setOrder("o", "s")
                }
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
}
