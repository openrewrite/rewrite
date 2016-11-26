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
package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.assertRefactored
import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Test

abstract class ChangeMethodNameTest(p: Parser): Parser by p {

    private val b: String = """
                |class B {
                |   public void singleArg(String s) {}
                |   public void arrArg(String[] s) {}
                |   public void varargArg(String... s) {}
                |}
            """

    @Test
    fun refactorMethodNameForMethodWithSingleArg() {
        val a = """
            |class A {
            |   public void test() {
            |       new B().singleArg("boo");
            |   }
            |}
        """

        val cu = parse(a, b)

        val fixed = cu.refactor {
            cu.findMethodCalls("B singleArg(String)").forEach {
                changeMethodName(it, "bar")
            }
        }.fix()

        assertRefactored(fixed, """
            |class A {
            |   public void test() {
            |       new B().bar("boo");
            |   }
            |}
        """)
    }

    @Test
    fun refactorMethodNameForMethodWithArrayArg() {
        val a = """
            |class A {
            |   public void test() {
            |       new B().arrArg(new String[] {"boo"});
            |   }
            |}
        """

        val cu = parse(a, b)

        val fixed = cu.refactor {
            cu.findMethodCalls("B arrArg(String[])").forEach {
                changeMethodName(it, "bar")
            }
        }.fix()

        assertRefactored(fixed, """
            |class A {
            |   public void test() {
            |       new B().bar(new String[] {"boo"});
            |   }
            |}
        """)
    }

    @Test
    fun refactorMethodNameForMethodWithVarargArg() {
        val a = """
            |class A {
            |   public void test() {
            |       new B().varargArg("boo", "again");
            |   }
            |}
        """

        val cu = parse(a, b)

        val fixed = cu.refactor {
            cu.findMethodCalls("B varargArg(String...)").forEach {
                changeMethodName(it, "bar")
            }
        }.fix()

        assertRefactored(fixed, """
            |class A {
            |   public void test() {
            |       new B().bar("boo", "again");
            |   }
            |}
        """)
    }

    @Test
    fun refactorMethodNameWhenMatchingAgainstMethodWithNameThatIsAnAspectjToken() {
        val b = """
            |class B {
            |   public void error() {}
            |   public void foo() {}
            |}
        """

        val a = """
            |class A {
            |   public void test() {
            |       new B().error();
            |   }
            |}
        """

        val cu = parse(a, b)
        val fixed = cu.refactor() {
            cu.findMethodCalls("B error()").forEach {
                changeMethodName(it, "foo")
            }
        }.fix()

        assertRefactored(fixed, """
            |class A {
            |   public void test() {
            |       new B().foo();
            |   }
            |}
        """)
    }
}

class OracleChangeMethodNameTest: ChangeMethodNameTest(OracleJdkParser())