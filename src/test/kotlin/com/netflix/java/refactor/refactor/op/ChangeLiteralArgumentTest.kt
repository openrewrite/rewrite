package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Test

abstract class ChangeLiteralArgumentTest(p: Parser): Parser by p {

    val b: String = """
        |package b;
        |public class B {
        |   public void singleArg(String s) {}
        |}
    """

    @Test
    fun changeStringLiteralArgument() {
        val a = """
            |import b.*;
            |class A {
            |   public void test() {
            |       String s = "bar";
            |       new B().singleArg("foo (%s)" + s + 0L);
            |   }
            |}
        """

        val cu = parse(a, b)
        val fixed = cu.refactor() {
            cu.findMethodCalls("b.B singleArg(String)").forEach {
                changeLiterals(it.args.args[0]) { s -> s?.toString()?.replace("%s", "{}") ?: s }
            }
        }.fix()

        assertRefactored(fixed, """
            |import b.*;
            |class A {
            |   public void test() {
            |       String s = "bar";
            |       new B().singleArg("foo ({})" + s + 0L);
            |   }
            |}
        """)
    }

    @Test
    fun changeStringLiteralArgumentWithEscapableCharacters() {
        val a = """
            |import b.*;
            |public class A {
            |    B b;
            |    public void test() {
            |        b.singleArg("mystring '%s'");
            |    }
            |}
        """

        val cu = parse(a, b)
        val fixed = cu.refactor {
            cu.findMethodCalls("b.B singleArg(..)").forEach {
                changeLiterals(it.args.args[0]) { s -> s?.toString()?.replace("%s", "{}") ?: s }
            }
        }.fix()

        assertRefactored(fixed, """
            |import b.*;
            |public class A {
            |    B b;
            |    public void test() {
            |        b.singleArg("mystring '{}'");
            |    }
            |}
        """)
    }
}

class OracleChangeLiteralArgumentTest: ChangeLiteralArgumentTest(OracleJdkParser())