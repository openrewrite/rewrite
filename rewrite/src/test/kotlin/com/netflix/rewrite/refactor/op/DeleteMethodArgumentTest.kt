package com.netflix.rewrite.refactor.op

import com.netflix.rewrite.ast.Tr
import com.netflix.rewrite.assertRefactored
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class DeleteMethodArgumentTest(p: Parser): Parser by p {

    val b = """
        |class B {
        |   public static void foo() {}
        |   public static void foo(int n) {}
        |   public static void foo(int n1, int n2) {}
        |   public static void foo(int n1, int n2, int n3) {}
        |}
    """

    @Test
    fun deleteMiddleArgument() {
        val a = parse("public class A {{ B.foo(0, 1, 2); }}", b)
        val fixed = a.refactor().deleteArgument(a.findMethodCalls("B foo(..)"), 1).fix()
        assertRefactored(fixed, "public class A {{ B.foo(0, 2); }}")
    }

    @Test
    fun deleteArgumentsConsecutively() {
        val a = parse("public class A {{ B.foo(0, 1, 2); }}", b)
        val foos = a.findMethodCalls("B foo(..)")
        val fixed = a.refactor().deleteArgument(foos, 1).deleteArgument(foos, 1).fix()
        assertRefactored(fixed, "public class A {{ B.foo(0); }}")
    }

    @Test
    fun doNotDeleteEmptyContainingFormatting() {
        val a = parse("public class A {{ B.foo( ); }}", b)
        val fixed = a.refactor().deleteArgument(a.findMethodCalls("B foo(..)"), 0).fix()
        assertRefactored(fixed, "public class A {{ B.foo( ); }}")
    }

    @Test
    fun insertEmptyWhenLastArgumentIsDeleted() {
        val a = parse("public class A {{ B.foo( ); }}", b)
        val fixed = a.refactor().deleteArgument(a.findMethodCalls("B foo(..)"), 0).fix()
        assertTrue(fixed.findMethodCalls("B foo(..)").first().args.args[0] is Tr.Empty)
    }
}

class OracleDeleteMethodArgumentTest: DeleteMethodArgumentTest(OracleJdkParser())