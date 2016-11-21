package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Test

abstract class ChangeMethodTargetToVariableTest(p: Parser): Parser by p {

    @Test
    fun refactorExplicitStaticToVariable() {
        val a = """
            |package a;
            |public class A {
            |   public void foo() {}
            |}
        """

        val b = """
            |package b;
            |public class B {
            |   public static void foo() {}
            |}
        """

        val c = """
            |import a.*;
            |import b.B;
            |public class C {
            |   A a;
            |   public void test() {
            |       B.foo();
            |   }
            |}
        """

        val cu = parse(c, a, b)
        val fixed = cu.refactor {
            val f = cu.classes[0].findFields("a.A")[0]
            cu.findMethodCalls("b.B foo()").forEach {
                changeTarget(it, f.vars[0])
            }
        }.fix()

        assertRefactored(fixed, """
            |import a.*;
            |public class C {
            |   A a;
            |   public void test() {
            |       a.foo();
            |   }
            |}
        """)
    }

    @Test
    fun refactorStaticImportToVariable() {
        val a = """
            |package a;
            |public class A {
            |   public void foo() {}
            |}
        """

        val b = """
            |package b;
            |public class B {
            |   public static void foo() {}
            |}
        """

        val c = """
            |import a.*;
            |import static b.B.*;
            |public class C {
            |   A a;
            |   public void test() {
            |       foo();
            |   }
            |}
        """

        val cu = parse(c, a, b)

        val fixed = cu.refactor {
            val f = cu.classes[0].findFields("a.A")[0]
            cu.findMethodCalls("b.B foo()").forEach {
                changeTarget(it, f.vars[0])
            }
        }.fix()

        assertRefactored(fixed, """
            |import a.*;
            |public class C {
            |   A a;
            |   public void test() {
            |       a.foo();
            |   }
            |}
        """)
    }
}

class OracleChangeMethodTargetToVariableTest: ChangeMethodTargetToVariableTest(OracleJdkParser())
