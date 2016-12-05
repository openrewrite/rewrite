/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.refactor.op

import com.netflix.rewrite.ast.assertRefactored
import com.netflix.rewrite.parse.OracleJdkParser
import com.netflix.rewrite.parse.Parser
import org.junit.Test

abstract class ChangeLiteralTest(p: Parser): Parser by p {

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
                changeLiteral(it.args.args[0]) { s -> s?.toString()?.replace("%s", "{}") ?: s }
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
                changeLiteral(it.args.args[0]) { s -> s?.toString()?.replace("%s", "{}") ?: s }
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

class OracleChangeLiteralTest : ChangeLiteralTest(OracleJdkParser())