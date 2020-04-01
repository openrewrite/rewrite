package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored
import org.openrewrite.java.tree.J

open class ReorderMethodArgumentsTest : JavaParser() {

    @Test
    fun refactorReorderArguments() {
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

        val cu = parse(b, a)
        val foos = cu.findMethodCalls("a.A foo(..)")
        val fixed = cu.refactor()
                .fold(foos) {
                    it.args.args.firstOrNull()?.let { arg ->
                        ChangeLiteral(arg as J.Literal) { "anotherstring" }
                    }
                }
                .fold(foos) { ReorderMethodArguments(it, "n", "m", "s") }
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
    fun refactorReorderArgumentsWithNoSourceAttachment() {
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
        """

        val cu = parse(b, a)
        val fixed = cu.refactor()
                .fold(cu.findMethodCalls("a.A foo(..)")) {
                    ReorderMethodArguments(it, "n", "s")
                            .withOriginalParamNames("s", "n")
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
    fun refactorReorderArgumentsWhereOneOfTheOriginalArgumentsIsVararg() {
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
        """

        val cu = parse(b, a)

        val fixed = cu.refactor().fold(cu.findMethodCalls("a.A foo(..)")) {
            ReorderMethodArguments(it, "s", "o", "n")
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
    fun refactorReorderArgumentsWhereTheLastArgumentIsVarargAndNotPresentInInvocation() {
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
        """

        val cu = parse(b, a)
        val fixed = cu.refactor().fold(cu.findMethodCalls("a.A foo(..)")) {
            ReorderMethodArguments(it, "o", "s")
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
