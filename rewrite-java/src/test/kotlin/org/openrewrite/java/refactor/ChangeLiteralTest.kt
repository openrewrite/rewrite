package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored
import org.openrewrite.java.tree.J

open class ChangeLiteralTest : JavaParser() {

    val b: String = """
        package b;
        public class B {
           public void singleArg(String s) {}
        }
    """.trimIndent()

    @Test
    fun changeStringLiteralArgument() {
        val a = """
            import b.*;
            class A {
               public void test() {
                   String s = "bar";
                   new B().singleArg("foo (%s)" + s + 0L);
               }
            }
        """.trimIndent()

        val cu = parse(a, b)
        val fixed = cu.findMethodCalls("b.B singleArg(String)").changeLiterals(cu)

        assertRefactored(fixed, """
            import b.*;
            class A {
               public void test() {
                   String s = "bar";
                   new B().singleArg("foo ({})" + s + 0L);
               }
            }
        """)
    }

    @Test
    fun changeStringLiteralArgumentWithEscapableCharacters() {
        val a = """
            import b.*;
            public class A {
                B b;
                public void test() {
                    b.singleArg("mystring '%s'");
                }
            }
        """.trimIndent()

        val cu = parse(a, b)
        val fixed = cu.findMethodCalls("b.B singleArg(..)").changeLiterals(cu)

        assertRefactored(fixed, """
            import b.*;
            public class A {
                B b;
                public void test() {
                    b.singleArg("mystring '{}'");
                }
            }
        """)
    }

    private fun List<J.MethodInvocation>.changeLiterals(cu: J.CompilationUnit) = fold(cu.refactor()) { acc, meth ->
        meth.args.args
                .fold(acc, { acc2, exp ->
                    acc2.visit(ChangeLiteral(exp) { s ->
                        s?.toString()?.replace("%s", "{}") ?: s
                    })
                })
    }.fix().fixed
}
