package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored

open class ChangeMethodNameTest : JavaParser() {

    private val b: String = """
                class B {
                   public void singleArg(String s) {}
                   public void arrArg(String[] s) {}
                   public void varargArg(String... s) {}
                }
            """.trimIndent()

    @Test
    fun refactorMethodNameForMethodWithSingleArg() {
        val a = """
            class A {
               public void test() {
                   new B().singleArg("boo");
               }
            }
        """.trimIndent()

        val cu = parse(a, b)

        val fixed = cu.refactor()
                .fold(cu.findMethodCalls("B singleArg(String)")) { ChangeMethodName(it, "bar") }
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   new B().bar("boo");
               }
            }
        """)
    }

    @Test
    fun refactorMethodNameForMethodWithArrayArg() {
        val a = """
            class A {
               public void test() {
                   new B().arrArg(new String[] {"boo"});
               }
            }
        """.trimIndent()

        val cu = parse(a, b)

        val fixed = cu.refactor()
                .fold(cu.findMethodCalls("B arrArg(String[])")) { ChangeMethodName(it, "bar") }
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   new B().bar(new String[] {"boo"});
               }
            }
        """)
    }

    @Test
    fun refactorMethodNameForMethodWithVarargArg() {
        val a = """
            class A {
               public void test() {
                   new B().varargArg("boo", "again");
               }
            }
        """.trimIndent()

        val cu = parse(a, b)

        val fixed = cu.refactor()
                .fold(cu.findMethodCalls("B varargArg(String...)")) { ChangeMethodName(it, "bar") }
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   new B().bar("boo", "again");
               }
            }
        """)
    }

    @Test
    fun refactorMethodNameWhenMatchingAgainstMethodWithNameThatIsAnAspectjToken() {
        val b = """
            class B {
               public void error() {}
               public void foo() {}
            }
        """.trimIndent()

        val a = """
            class A {
               public void test() {
                   new B().error();
               }
            }
        """.trimIndent()

        val cu = parse(a, b)
        val fixed = cu.refactor()
                .fold(cu.findMethodCalls("B error()")) { ChangeMethodName(it, "foo") }
                .fix().fixed

        assertRefactored(fixed, """
            class A {
               public void test() {
                   new B().foo();
               }
            }
        """)
    }
}
