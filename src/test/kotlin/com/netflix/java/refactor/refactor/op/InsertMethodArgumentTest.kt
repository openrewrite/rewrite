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

abstract class InsertMethodArgumentTest(p: Parser): Parser by p {

    val b = """
            |class B {
            |   public void foo() {}
            |   public void foo(String s) {}
            |   public void foo(String s1, String s2) {}
            |   public void foo(String s1, String s2, String s3) {}
            |
            |   public void bar(String s, String s2) {}
            |   public void bar(String s, String s2, String s3) {}
            |}
        """

    @Test
    fun refactorInsertArgument() {
        val a = """
            |class A {
            |   public void test() {
            |       B b = new B();
            |       b.foo();
            |       b.foo("1");
            |   }
            |}
        """

//        |       b.bar("1", "2");

        val cu = parse(a, b)
        val fixed = cu.refactor {
            cu.findMethodCalls("B foo(String)").forEach {
                insertArgument(it, 0, "\"0\"") // insert at beginning
                insertArgument(it, 2, "\"2\"") // insert at end
            }

            cu.findMethodCalls("B foo()").forEach {
                insertArgument(it, 0, "\"0\"")
            }

            // FIXME re-add this compatibility test once reordering is implemented
//          .findMethodCalls("B bar(String, String)")
//              .changeArguments()
//                  .reorderByArgName("s2", "s") // compatibility of reordering and insertion
//                  .insert(0, "\"0\"")
        }.fix()

        assertRefactored(fixed, """
            |class A {
            |   public void test() {
            |       B b = new B();
            |       b.foo("0");
            |       b.foo("0", "1", "2");
            |   }
            |}
        """)

        //             |       b.bar("0", "2", "1");
    }
}

class OracleInsertMethodArgumentTest: InsertMethodArgumentTest(OracleJdkParser())