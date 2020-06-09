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
import org.openrewrite.java.tree.J

interface ReorderMethodArgumentsTest {

    @Test
    fun refactorReorderArguments(jp: JavaParser) {
        val a = """
            package a;
            public class A {
               public void foo(String s, Integer m, Integer n) {}
               public void foo(Integer n, Integer m, String s) {}
            }
        """.trimIndent()

        val b = """
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
        """.trimIndent()

        val cu = jp.parse(b, a)
        val foos = cu.findMethodCalls("a.A foo(..)")
        val fixed = cu.refactor()
                .fold(foos) {
                    it.args.args.firstOrNull()?.let { arg ->
                        ChangeLiteral(arg as J.Literal) { "anotherstring" }
                    }
                }
                .fold(foos) { ReorderMethodArguments.Scoped(it, arrayOf("n", "m", "s"), arrayOf()) }
                .fix().fixed

        assertRefactored(fixed, """
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
        """)
    }

    @Test
    fun refactorReorderArgumentsWithNoSourceAttachment(jp: JavaParser) {
        val a = """
            package a;
            public class A {
               public void foo(String arg0, Integer... arg1) {}
               public void foo(Integer arg0, Integer arg1, String arg2) {}
            }
        """

        val b = """
            import a.*;
            public class B {
               A a;
               public void test() {
                   a.foo("s", 0, 1);
               }
            }
        """.trimIndent()

        val cu = jp.parse(b, a)
        val fixed = cu.refactor()
                .fold(cu.findMethodCalls("a.A foo(..)")) {
                    ReorderMethodArguments.Scoped(it, arrayOf("n", "s"), arrayOf("s", "n"))
                }
                .fix().fixed

        assertRefactored(fixed, """
            import a.*;
            public class B {
               A a;
               public void test() {
                   a.foo(0, 1, "s");
               }
            }
        """)
    }

    @Test
    fun refactorReorderArgumentsWhereOneOfTheOriginalArgumentsIsVararg(jp: JavaParser) {
        val a = """
            package a;
            public class A {
               public void foo(String s, Integer n, Object... o) {}
               public void bar(String s, Object... o) {}
            }
        """

        val b = """
            import a.*;
            public class B {
               A a;
               public void test() {
                   a.foo("mystring", 0, "a", "b");
               }
            }
        """.trimIndent()

        val cu = jp.parse(b, a)

        val fixed = cu.refactor().fold(cu.findMethodCalls("a.A foo(..)")) {
            ReorderMethodArguments.Scoped(it, arrayOf("s", "o", "n"), arrayOf())
        }.fix().fixed

        assertRefactored(fixed, """
            import a.*;
            public class B {
               A a;
               public void test() {
                   a.foo("mystring", "a", "b", 0);
               }
            }
        """)
    }

    @Test
    fun refactorReorderArgumentsWhereTheLastArgumentIsVarargAndNotPresentInInvocation(jp: JavaParser) {
        val a = """
            package a;
            public class A {
               public void foo(String s, Object... o) {}
            }
        """

        val b = """
            import a.*;
            public class B {
               public void test() {
                   new A().foo("mystring");
               }
            }
        """.trimIndent()

        val cu = jp.parse(b, a)
        val fixed = cu.refactor().fold(cu.findMethodCalls("a.A foo(..)")) {
            ReorderMethodArguments.Scoped(it, arrayOf("o", "s"), arrayOf())
        }.fix().fixed

        assertRefactored(fixed, """
            import a.*;
            public class B {
               public void test() {
                   new A().foo("mystring");
               }
            }
        """)
    }
}
